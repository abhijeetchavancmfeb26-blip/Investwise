package com.investwise.investment.controller;

import com.investwise.investment.common.ApiResponse;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.security.AuthUser;
import com.investwise.investment.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscriptions and Razorpay checkout.
 * <p>
 * Two calls make up the payment flow: create an order, then verify the signature
 * the gateway returns. Nothing is granted between those two steps.
 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "3. Subscriptions & Payments")
public class BillingController {

    private final SubscriptionService billing;

    // ---------------- subscriptions ----------------

    @GetMapping("/subscriptions/me")
    @Operation(summary = "Current subscription, if any")
    public ResponseEntity<ApiResponse<Responses.SubscriptionView>> current(
            @AuthenticationPrincipal AuthUser user) {
        return billing.current(user.id())
                .map(view -> ResponseEntity.ok(ApiResponse.ok(view)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(null, "You are on the free Starter plan")));
    }

    @GetMapping("/subscriptions/history")
    @Operation(summary = "Subscription history")
    public ResponseEntity<ApiResponse<PageResponse<Responses.SubscriptionView>>> history(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(billing.history(user.id(), page, size)));
    }

    @DeleteMapping("/subscriptions/{id}")
    @Operation(summary = "Cancel a subscription",
            description = "Access continues until the end of the paid term.")
    public ResponseEntity<ApiResponse<Responses.SubscriptionView>> cancel(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(billing.cancel(id, user),
                "Subscription cancelled. Premium access continues until the end of your paid term."));
    }

    // ---------------- payments ----------------

    @PostMapping("/payments/create-order")
    @Operation(summary = "Create a Razorpay order for a plan",
            description = "Returns the order id and the public key needed to open Checkout.")
    public ResponseEntity<ApiResponse<Responses.OrderView>> createOrder(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody Requests.Subscribe request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                billing.createOrder(user, request), "Order created. Continue to payment."));
    }

    @PostMapping("/payments/verify")
    @Operation(summary = "Verify the payment signature and activate the subscription",
            description = "The signature is validated server side; only then is access granted.")
    public ResponseEntity<ApiResponse<Responses.PaymentView>> verify(
            @AuthenticationPrincipal AuthUser user, @Valid @RequestBody Requests.VerifyPayment request) {
        return ResponseEntity.ok(ApiResponse.ok(billing.verify(user, request),
                "Payment successful. Your premium features are now active."));
    }

    @PostMapping("/payments/failed")
    @Operation(summary = "Record an abandoned or declined payment")
    public ResponseEntity<ApiResponse<Responses.PaymentView>> markFailed(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam @NotBlank String razorpayOrderId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok(
                billing.markFailed(user, razorpayOrderId, reason), "Payment recorded as unsuccessful"));
    }

    @GetMapping("/payments/me")
    @Operation(summary = "Payment history for the signed-in user")
    public ResponseEntity<ApiResponse<PageResponse<Responses.PaymentView>>> myPayments(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(billing.myPayments(user.id(), page, size)));
    }

    @GetMapping("/payments/{id}")
    @Operation(summary = "Fetch one payment with its invoice number")
    public ResponseEntity<ApiResponse<Responses.PaymentView>> payment(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(billing.payment(id, user.id())));
    }
}
