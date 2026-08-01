package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.config.Events;
import com.investwise.investment.config.RabbitConfig;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Payment;
import com.investwise.investment.model.Plan;
import com.investwise.investment.model.Subscription;
import com.investwise.investment.repository.jpa.PaymentRepository;
import com.investwise.investment.repository.jpa.PlanRepository;
import com.investwise.investment.repository.jpa.SubscriptionRepository;
import com.investwise.investment.security.AuthUser;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Subscriptions and the payments that activate them.
 * <p>
 * The original kept these in two services that could not act without each other:
 * creating an order needed a subscription, and verifying a payment activated one.
 * Merging them removed a circular dependency and a whole file.
 * <p>
 * The security model is unchanged and worth stating plainly: the server creates
 * the order, the browser receives only the public key, and the returned signature
 * is verified here before any entitlement is granted. The browser is never trusted
 * to assert that a payment succeeded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final int PAISE = 100;

    private final SubscriptionRepository subscriptions;
    private final PlanRepository plans;
    private final PaymentRepository payments;
    private final RazorpayClient razorpay;
    private final ActivityService activity;
    private final RabbitTemplate rabbit;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    // ---------------- plans ----------------

    @Transactional(readOnly = true)
    public List<Responses.PlanView> activePlans() {
        return plans.findByActiveTrueOrderByPriceAsc().stream().map(Responses.PlanView::from).toList();
    }

    @Transactional(readOnly = true)
    public Responses.PlanView plan(String code) {
        return plans.findByCodeIgnoreCase(code).map(Responses.PlanView::from)
                .orElseThrow(() -> ApiException.notFound("Plan"));
    }

    // ---------------- entitlement ----------------

    /** Falls back to FREE when nothing is active, which is the safe default. */
    @Transactional(readOnly = true)
    public Enums.Tier tierOf(Long userId) {
        return subscriptions.findCurrent(userId).map(s -> s.getPlan().getTier()).orElse(Enums.Tier.FREE);
    }

    @Transactional(readOnly = true)
    public Optional<Responses.SubscriptionView> current(Long userId) {
        return subscriptions.findCurrent(userId).map(Responses.SubscriptionView::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.SubscriptionView> history(Long userId, int page, int size) {
        return PageResponse.of(
                subscriptions.findByUserIdOrderByCreatedAtDesc(userId, ProductService.pageable(page, size)),
                Responses.SubscriptionView::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.SubscriptionView> adminList(Enums.SubscriptionStatus status, int page, int size) {
        var pageable = ProductService.pageable(page, size);
        return PageResponse.of(
                status == null ? subscriptions.findAll(pageable) : subscriptions.findByStatus(status, pageable),
                Responses.SubscriptionView::from);
    }

    // ---------------- checkout ----------------

    @Transactional
    public Responses.OrderView createOrder(AuthUser user, Requests.Subscribe request) {
        Plan plan = plans.findByCodeIgnoreCase(request.planCode())
                .orElseThrow(() -> ApiException.notFound("Plan"));

        if (plan.isFree()) {
            throw ApiException.badRequest("The free plan is applied automatically and cannot be purchased");
        }
        subscriptions.findCurrent(user.id()).ifPresent(existing -> {
            if (existing.getPlan().getTier() == plan.getTier() && existing.daysRemaining() > 7) {
                throw ApiException.badRequest("You already have an active %s plan with %d days remaining"
                        .formatted(existing.getPlan().getName(), existing.daysRemaining()));
            }
        });

        Subscription subscription = subscriptions.save(Subscription.builder()
                .userId(user.id()).plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(plan.getDurationMonths()))
                .status(Enums.SubscriptionStatus.PENDING)
                .build());

        try {
            JSONObject order = new JSONObject();
            order.put("amount", plan.getPrice().multiply(BigDecimal.valueOf(PAISE)).longValue());
            order.put("currency", "INR");
            order.put("receipt", "iw_sub_" + subscription.getId());
            order.put("payment_capture", 1);

            String orderId = razorpay.orders.create(order).get("id");

            Payment payment = payments.save(Payment.builder()
                    .userId(user.id()).userEmail(user.email()).subscription(subscription)
                    .orderId(orderId).amount(plan.getPrice())
                    .status(Enums.PaymentStatus.CREATED).build());

            activity.record(user.id(), user.email(), "PAYMENT_INITIATED",
                    "Order %s for %s".formatted(orderId, plan.getName()));

            log.info("Razorpay order {} created for user {}", orderId, user.id());

            return new Responses.OrderView(payment.getId(), orderId, razorpayKeyId,
                    plan.getPrice().multiply(BigDecimal.valueOf(PAISE)).longValue(),
                    plan.getPrice(), "INR", plan.getName(), plan.getCode(),
                    user.name(), user.email(), "InvestWise " + plan.getName() + " subscription");

        } catch (RazorpayException ex) {
            log.error("Razorpay order creation failed for user {}", user.id(), ex);
            throw ApiException.badRequest("We could not reach the payment gateway. Please try again.");
        }
    }

    @Transactional
    public Responses.PaymentView verify(AuthUser user, Requests.VerifyPayment request) {
        Payment payment = payments.findByOrderId(request.razorpayOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment"));

        if (!payment.getUserId().equals(user.id())) {
            throw ApiException.forbidden("This payment does not belong to your account");
        }
        // Idempotent: a repeat call on an already captured payment returns the record
        if (payment.getStatus() == Enums.PaymentStatus.SUCCESS) {
            return Responses.PaymentView.from(payment);
        }

        if (!signatureValid(request)) {
            payment.setStatus(Enums.PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            payments.save(payment);
            log.error("SIGNATURE MISMATCH on order {} for user {}", request.razorpayOrderId(), user.id());
            throw ApiException.badRequest(
                    "Payment verification failed. If money was deducted it will be refunded automatically.");
        }

        payment.setPaymentId(request.razorpayPaymentId());
        payment.setStatus(Enums.PaymentStatus.SUCCESS);
        payment.setMethod(fetchMethod(request.razorpayPaymentId()));
        payment.setInvoiceNo(invoiceNumber(payment.getId()));
        Payment saved = payments.save(payment);

        Subscription subscription = saved.getSubscription();
        if (subscription != null) {
            subscription.activate();
            subscriptions.save(subscription);
            publishTier(subscription.getUserId(), subscription.getPlan().getTier(),
                    subscription.getPlan().getName());
        }

        activity.record(user.id(), user.email(), "PAYMENT_VERIFIED",
                "Captured %s, invoice %s".formatted(saved.getAmount(), saved.getInvoiceNo()));

        log.info("Payment {} verified for user {}", saved.getPaymentId(), user.id());
        return Responses.PaymentView.from(saved);
    }

    /** HMAC-SHA256 over "orderId|paymentId", keyed with the secret held only by us. */
    private boolean signatureValid(Requests.VerifyPayment request) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.razorpayOrderId());
            attributes.put("razorpay_payment_id", request.razorpayPaymentId());
            attributes.put("razorpay_signature", request.razorpaySignature());
            return Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        } catch (RazorpayException ex) {
            log.error("Signature verification threw for order {}", request.razorpayOrderId(), ex);
            return false;
        }
    }

    /** Informational only, so a failure here must not fail an already verified payment. */
    private String fetchMethod(String paymentId) {
        try {
            return razorpay.payments.fetch(paymentId).get("method");
        } catch (RazorpayException | RuntimeException ex) {
            return "unknown";
        }
    }

    @Transactional
    public Responses.PaymentView markFailed(AuthUser user, String orderId, String reason) {
        Payment payment = payments.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Payment"));
        if (!payment.getUserId().equals(user.id())) {
            throw ApiException.forbidden("This payment does not belong to your account");
        }
        payment.setStatus(Enums.PaymentStatus.FAILED);
        payment.setFailureReason(reason == null ? "Cancelled by the customer" : reason);
        return Responses.PaymentView.from(payments.save(payment));
    }

    // ---------------- payments ----------------

    @Transactional(readOnly = true)
    public PageResponse<Responses.PaymentView> myPayments(Long userId, int page, int size) {
        return PageResponse.of(
                payments.findByUserIdOrderByCreatedAtDesc(userId, ProductService.pageable(page, size)),
                Responses.PaymentView::from);
    }

    @Transactional(readOnly = true)
    public Responses.PaymentView payment(Long id, Long userId) {
        return payments.findByIdAndUserId(id, userId).map(Responses.PaymentView::from)
                .orElseThrow(() -> ApiException.notFound("Payment"));
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.PaymentView> adminPayments(Enums.PaymentStatus status, Long userId,
                                                             int page, int size) {
        return PageResponse.of(
                payments.findFiltered(status, userId, ProductService.pageable(page, size)),
                Responses.PaymentView::from);
    }

    // ---------------- lifecycle ----------------

    @Transactional
    public Responses.SubscriptionView cancel(Long id, AuthUser user) {
        Subscription subscription = subscriptions.findByIdAndUserId(id, user.id())
                .orElseThrow(() -> ApiException.notFound("Subscription"));

        if (subscription.getStatus() != Enums.SubscriptionStatus.ACTIVE) {
            throw ApiException.badRequest("Only an active subscription can be cancelled");
        }
        // Access is honoured to the end of the paid term rather than revoked immediately
        subscription.setStatus(Enums.SubscriptionStatus.CANCELLED);
        Subscription saved = subscriptions.save(subscription);

        publishTier(user.id(), Enums.Tier.FREE, saved.getPlan().getName());
        activity.record(user.id(), user.email(), "SUBSCRIPTION_CANCELLED", saved.getPlan().getName());

        return Responses.SubscriptionView.from(saved);
    }

    /** Nightly sweep, run from the scheduler. */
    @Transactional
    public int expireLapsed() {
        List<Subscription> lapsed = subscriptions.findLapsed(LocalDate.now());

        lapsed.forEach(subscription -> {
            subscription.setStatus(Enums.SubscriptionStatus.EXPIRED);
            publishTier(subscription.getUserId(), Enums.Tier.FREE, subscription.getPlan().getName());
        });
        subscriptions.saveAll(lapsed);

        if (!lapsed.isEmpty()) {
            log.info("Expired {} lapsed subscription(s)", lapsed.size());
        }
        return lapsed.size();
    }

    /** Tells the User Service to refresh its cached tier. */
    private void publishTier(Long userId, Enums.Tier tier, String planName) {
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_SUBSCRIPTION_CHANGED,
                new Events.SubscriptionChanged(userId, tier.name(), planName));
    }

    /** Sequential per financial year, e.g. {@code IW/2026-27/000042}. */
    private String invoiceNumber(Long paymentId) {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        return "IW/%d-%02d/%06d".formatted(startYear, (startYear + 1) % 100, paymentId);
    }
}
