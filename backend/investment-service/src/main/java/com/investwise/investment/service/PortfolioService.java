package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Goal;
import com.investwise.investment.model.Holding;
import com.investwise.investment.model.Portfolio;
import com.investwise.investment.model.Product;
import com.investwise.investment.model.Transaction;
import com.investwise.investment.repository.jpa.GoalRepository;
import com.investwise.investment.repository.jpa.HoldingRepository;
import com.investwise.investment.repository.jpa.PortfolioRepository;
import com.investwise.investment.repository.jpa.ProductRepository;
import com.investwise.investment.repository.jpa.TransactionRepository;
import com.investwise.investment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Portfolio tracking, ROI, the dashboard and premium analytics.
 * <p>
 * The rebalancing plan lives here rather than in a service of its own â€” it is a
 * read over the same holdings the analytics already loads, so a separate service
 * would only mean loading them twice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    /** Allocation may drift five points before rebalancing is suggested; tighter causes churn. */
    private static final BigDecimal DRIFT_TOLERANCE = BigDecimal.valueOf(5);

    private final PortfolioRepository portfolios;
    private final HoldingRepository holdings;
    private final ProductRepository products;
    private final GoalRepository goals;
    private final TransactionRepository transactions;
    private final RiskService riskService;
    private final SubscriptionService subscriptions;
    private final ActivityService activity;

    // ---------------- lifecycle ----------------

    /** Idempotent, because RabbitMQ delivers at least once. */
    @Transactional
    public void ensureExists(Long userId) {
        if (!portfolios.existsByUserId(userId)) {
            portfolios.save(Portfolio.builder().userId(userId).build());
            log.info("Created portfolio for user {}", userId);
        }
    }

    private Portfolio require(Long userId) {
        return portfolios.findByUserIdWithHoldings(userId)
                .or(() -> portfolios.findByUserId(userId))
                .orElseGet(() -> {
                    ensureExists(userId);
                    return portfolios.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Portfolio"));
                });
    }

    private List<Holding> open(Portfolio portfolio) {
        return portfolio.getHoldings().stream().filter(h -> !h.isRedeemed()).toList();
    }

    // ---------------- reads ----------------

    @Transactional
    public Responses.PortfolioView get(Long userId) {
        Portfolio portfolio = require(userId);
        List<Holding> live = open(portfolio);

        List<Responses.HoldingView> views = live.stream()
                .sorted(Comparator.comparing(Holding::getCurrentValue).reversed())
                .map(Responses.HoldingView::from).toList();

        return new Responses.PortfolioView(portfolio.getId(), portfolio.getTotalInvested(),
                portfolio.getCurrentValue(), portfolio.gain(), portfolio.gainPct(),
                annualised(portfolio, live), views.size(), views,
                percentages(live, h -> h.getProduct().getCategory().label(), portfolio.getCurrentValue()),
                percentages(live, h -> h.getProduct().getRiskLevel().name(), portfolio.getCurrentValue()));
    }

    @Transactional
    public Responses.DashboardView dashboard(AuthUser user) {
        Portfolio portfolio = require(user.id());
        List<Holding> live = open(portfolio);
        List<Goal> allGoals = goals.findByUserId(user.id());
        BigDecimal rate = riskService.returnAssumption(user.id());

        var risk = riskService.latest(user.id());
        Enums.Tier tier = subscriptions.tierOf(user.id());

        List<Transaction> recent = transactions
                .findFiltered(user.id(), null, null, null, ProductService.pageable(0, 5)).getContent();
        Map<Long, String> names = productNames(recent);

        return new Responses.DashboardView(
                portfolio.getTotalInvested(), portfolio.getCurrentValue(),
                portfolio.gain(), portfolio.gainPct(),
                allGoals.size(),
                (int) allGoals.stream().filter(g -> g.getStatus() == Enums.GoalStatus.ON_TRACK).count(),
                (int) allGoals.stream().filter(g -> g.getStatus() == Enums.GoalStatus.BEHIND).count(),
                (int) allGoals.stream().filter(g -> g.getStatus() == Enums.GoalStatus.ACHIEVED).count(),
                allGoals.stream().map(Goal::getTargetAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                allGoals.stream().map(Goal::getCurrentAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                risk.map(r -> r.getProfile().name()).orElse(null),
                risk.map(com.investwise.investment.model.RiskAssessment::getScore).orElse(null),
                risk.isPresent(), tier.name(), tier.isPremium(),
                subscriptions.current(user.id()).map(Responses.SubscriptionView::daysRemaining).orElse(0L),
                percentages(live, h -> h.getProduct().getCategory().label(), portfolio.getCurrentValue()),
                allGoals.stream()
                        .filter(g -> g.getStatus() != Enums.GoalStatus.CANCELLED)
                        .sorted(Comparator.naturalOrder()).limit(3)
                        .map(g -> Responses.GoalView.from(g, rate)).toList(),
                live.stream().sorted(Comparator.comparing(Holding::getCurrentValue).reversed())
                        .limit(5).map(Responses.HoldingView::from).toList(),
                recent.stream().map(t -> Responses.TransactionView.from(t, names.get(t.getProductId()))).toList());
    }

    /** Premium analytics. Gated on the tier server-side, where it cannot be bypassed. */
    @Transactional
    public Responses.AnalyticsView analytics(AuthUser user) {
        if (!subscriptions.tierOf(user.id()).isPremium()) {
            throw ApiException.forbidden("Advanced analytics is a Premium feature. Upgrade to unlock it.");
        }

        Portfolio portfolio = require(user.id());
        List<Holding> live = open(portfolio);

        if (live.isEmpty()) {
            return new Responses.AnalyticsView(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, BigDecimal.ZERO,
                    BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, Map.of(), Map.of(),
                    List.of("Add your first holding to unlock portfolio analytics."), null);
        }

        BigDecimal value = portfolio.getCurrentValue();
        Holding best = live.stream().max(Comparator.comparing(Holding::gainPct)).orElseThrow();
        Holding worst = live.stream().min(Comparator.comparing(Holding::gainPct)).orElseThrow();
        Holding largest = live.stream().max(Comparator.comparing(Holding::getCurrentValue)).orElseThrow();

        BigDecimal concentration = value.signum() == 0 ? BigDecimal.ZERO
                : largest.getCurrentValue().multiply(Money.HUNDRED).divide(value, 2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> byAssetClass = absolute(live,
                h -> h.getProduct().getCategory().assetClass().name());

        BigDecimal weightedReturn = live.stream()
                .map(h -> h.getProduct().getExpectedReturn().multiply(h.getCurrentValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(value.max(BigDecimal.ONE), 2, RoundingMode.HALF_UP);

        BigDecimal diversification = diversificationScore(byAssetClass, value, live.size());
        BigDecimal longTerm = live.stream().filter(Holding::isLongTerm).map(Holding::gain)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shortTerm = live.stream().filter(h -> !h.isLongTerm()).map(Holding::gain)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Responses.AnalyticsView(
                portfolio.getTotalInvested(), value, portfolio.gain(), portfolio.gainPct(),
                annualised(portfolio, live),
                best.getProduct().getName(), best.gainPct(),
                worst.getProduct().getName(), worst.gainPct(),
                diversification, concentration, largest.getProduct().getName(), weightedReturn,
                Money.futureValue(value, weightedReturn, 5), Money.futureValue(value, weightedReturn, 10),
                longTerm, shortTerm,
                toPercent(byAssetClass, value),
                percentages(live, h -> h.getProduct().getRiskLevel().name(), value),
                insights(live, concentration, diversification, largest, worst),
                rebalance(user.id(), live, value));
    }

    // ---------------- rebalancing ----------------

    /**
     * Compares live weights against the profile's target and produces ordered actions.
     * <p>
     * A {@link TreeMap} keeps asset classes in a stable sorted order by its own
     * contract, and a {@link LinkedList} used as a {@link Deque} assembles the
     * actions with O(1) insertion at either end â€” sells appended first so proceeds
     * exist before they are spent.
     */
    private Responses.RebalanceView rebalance(Long userId, List<Holding> live, BigDecimal value) {
        var assessment = riskService.latest(userId);
        if (assessment.isEmpty() || value.signum() == 0) {
            return null;
        }
        var profile = assessment.get().getProfile();

        Map<String, BigDecimal> held = new TreeMap<>(absolute(live,
                h -> h.getProduct().getCategory().assetClass().name()));

        Map<String, Integer> targets = new TreeMap<>(Map.of(
                Enums.AssetClass.EQUITY.name(), profile.equityPct(),
                Enums.AssetClass.DEBT.name(), profile.debtPct(),
                Enums.AssetClass.GOLD.name(), profile.goldPct()));
        held.keySet().forEach(assetClass -> targets.putIfAbsent(assetClass, 0));

        Map<String, Responses.RebalanceView.Drift> drift = new TreeMap<>();
        BigDecimal maxDrift = BigDecimal.ZERO;

        for (var entry : targets.entrySet()) {
            BigDecimal amount = held.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal currentPct = amount.multiply(Money.HUNDRED).divide(value, 2, RoundingMode.HALF_UP);
            BigDecimal targetPct = BigDecimal.valueOf(entry.getValue());
            BigDecimal driftPct = currentPct.subtract(targetPct);
            BigDecimal targetAmount = value.multiply(targetPct).divide(Money.HUNDRED, 2, RoundingMode.HALF_UP);

            String status = driftPct.abs().compareTo(DRIFT_TOLERANCE) <= 0 ? "ON_TARGET"
                    : driftPct.signum() > 0 ? "OVERWEIGHT" : "UNDERWEIGHT";

            drift.put(entry.getKey(), new Responses.RebalanceView.Drift(
                    currentPct, targetPct, driftPct, amount, targetAmount.subtract(amount), status));

            maxDrift = maxDrift.max(driftPct.abs());
        }

        Deque<Responses.RebalanceView.Action> queue = new LinkedList<>();
        drift.entrySet().stream()
                .filter(e -> "OVERWEIGHT".equals(e.getValue().status()))
                .sorted((a, b) -> b.getValue().driftPct().compareTo(a.getValue().driftPct()))
                .forEach(e -> queue.addLast(new Responses.RebalanceView.Action(0, "REDUCE", e.getKey(),
                        e.getValue().differenceAmount().abs(),
                        "Overweight by %s points. Trimming frees capital for the underweight sleeves."
                                .formatted(e.getValue().driftPct()))));

        drift.entrySet().stream()
                .filter(e -> "UNDERWEIGHT".equals(e.getValue().status()))
                .sorted(Comparator.comparing(e -> e.getValue().driftPct()))
                .forEach(e -> queue.addLast(new Responses.RebalanceView.Action(0, "INCREASE", e.getKey(),
                        e.getValue().differenceAmount().abs(),
                        "Underweight by %s points against a target of %s%%."
                                .formatted(e.getValue().driftPct().abs(), e.getValue().targetPct()))));

        List<Responses.RebalanceView.Action> actions = new ArrayList<>();
        int step = 1;
        for (var action : queue) {
            actions.add(new Responses.RebalanceView.Action(step++, action.type(),
                    action.assetClass(), action.amount(), action.rationale()));
        }

        boolean needed = maxDrift.compareTo(DRIFT_TOLERANCE) > 0;
        return new Responses.RebalanceView(needed,
                needed ? "Your largest deviation is %s points, beyond the %s point tolerance band."
                            .formatted(maxDrift, DRIFT_TOLERANCE)
                       : "Every asset class is within %s points of its target. No action needed."
                            .formatted(DRIFT_TOLERANCE),
                maxDrift, drift, actions);
    }

    // ---------------- writes ----------------

    @Transactional
    public Responses.HoldingView addHolding(AuthUser user, Requests.Holding request) {
        Portfolio portfolio = require(user.id());
        Product product = products.findById(request.productId())
                .orElseThrow(() -> ApiException.notFound("Product"));

        if (!product.isActive()) {
            throw ApiException.badRequest("This product is no longer open for investment");
        }
        if (request.amount().compareTo(product.getMinInvestment()) < 0) {
            throw ApiException.badRequest("%s has a minimum investment of %s"
                    .formatted(product.getName(), product.getMinInvestment()));
        }

        Goal goal = request.goalId() == null ? null
                : goals.findByIdAndUserId(request.goalId(), user.id())
                    .orElseThrow(() -> ApiException.notFound("Goal"));

        BigDecimal units = request.amount().divide(request.buyPrice(), 4, RoundingMode.HALF_UP);
        BigDecimal price = simulatePrice(request.buyPrice(), product.getExpectedReturn(), request.purchaseDate());

        Holding holding = Holding.builder()
                .product(product).goal(goal).units(units)
                .buyPrice(request.buyPrice()).currentPrice(price)
                .investedAmount(request.amount())
                .currentValue(price.multiply(units).setScale(2, RoundingMode.HALF_UP))
                .purchaseDate(request.purchaseDate())
                .build();

        portfolio.addHolding(holding);
        portfolio.recalculate();
        portfolios.save(portfolio);

        transactions.save(Transaction.builder()
                .userId(user.id()).productId(product.getId()).type(Enums.TransactionType.BUY)
                .units(units).price(request.buyPrice()).amount(request.amount())
                .referenceNo(reference("BUY")).build());

        // Money invested against a goal counts towards that goal's progress
        if (goal != null) {
            goal.contribute(request.amount());
            goals.save(goal);
        }

        activity.record(user.id(), user.email(), "HOLDING_ADDED",
                "Invested %s in %s".formatted(request.amount(), product.getName()));

        return Responses.HoldingView.from(holding);
    }

    @Transactional
    public Responses.HoldingView redeem(Long holdingId, AuthUser user, Requests.Redeem request) {
        Holding holding = holdings.findByIdAndPortfolioUserId(holdingId, user.id())
                .orElseThrow(() -> ApiException.notFound("Holding"));

        if (holding.isRedeemed()) {
            throw ApiException.badRequest("This holding has already been fully redeemed");
        }
        if (request.units().compareTo(holding.getUnits()) > 0) {
            throw ApiException.badRequest("You hold only %s units of %s"
                    .formatted(holding.getUnits(), holding.getProduct().getName()));
        }
        if (holding.getProduct().hasLockIn()) {
            LocalDate unlocks = holding.getPurchaseDate().plusMonths(holding.getProduct().getLockInMonths());
            if (LocalDate.now().isBefore(unlocks)) {
                throw ApiException.forbidden("%s is locked in until %s"
                        .formatted(holding.getProduct().getName(), unlocks));
            }
        }

        BigDecimal proceeds = request.units().multiply(holding.getCurrentPrice())
                .setScale(2, RoundingMode.HALF_UP);
        holding.redeem(request.units());
        holdings.save(holding);

        Portfolio portfolio = holding.getPortfolio();
        portfolio.recalculate();
        portfolios.save(portfolio);

        transactions.save(Transaction.builder()
                .userId(user.id()).productId(holding.getProduct().getId())
                .type(Enums.TransactionType.REDEEM).units(request.units())
                .price(holding.getCurrentPrice()).amount(proceeds)
                .referenceNo(reference("RED")).build());

        activity.record(user.id(), user.email(), "HOLDING_REDEEMED",
                "Redeemed %s units of %s".formatted(request.units(), holding.getProduct().getName()));

        return Responses.HoldingView.from(holding);
    }

    @Transactional
    public void removeHolding(Long holdingId, Long userId) {
        Holding holding = holdings.findByIdAndPortfolioUserId(holdingId, userId)
                .orElseThrow(() -> ApiException.notFound("Holding"));

        Portfolio portfolio = holding.getPortfolio();
        portfolio.getHoldings().remove(holding);
        portfolio.recalculate();
        portfolios.save(portfolio);
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.TransactionView> transactions(Long userId, Enums.TransactionType type,
                                                                LocalDateTime from, LocalDateTime to,
                                                                int page, int size) {
        var result = transactions.findFiltered(userId, type, from, to, ProductService.pageable(page, size));
        Map<Long, String> names = productNames(result.getContent());
        return PageResponse.of(result, t -> Responses.TransactionView.from(t, names.get(t.getProductId())));
    }

    /**
     * Nightly mark to market.
     * <p>
     * A real deployment would read a NAV feed. This applies the product's expected
     * return pro-rata with a small random deviation, so the demo portfolio moves
     * believably instead of sitting frozen at cost.
     */
    @Transactional
    public int refreshMarketValues() {
        List<Holding> open = holdings.findAllOpen();
        if (open.isEmpty()) return 0;

        open.forEach(holding -> holding.markToMarket(simulatePrice(
                holding.getBuyPrice(), holding.getProduct().getExpectedReturn(), holding.getPurchaseDate())));
        holdings.saveAll(open);

        open.stream().map(Holding::getPortfolio).distinct().forEach(portfolio -> {
            portfolio.recalculate();
            portfolios.save(portfolio);
        });

        log.info("Marked {} holdings to market", open.size());
        return open.size();
    }

    // ---------------- helpers ----------------

    private BigDecimal simulatePrice(BigDecimal buyPrice, BigDecimal annualReturn, LocalDate since) {
        double years = Math.max(0, ChronoUnit.DAYS.between(since, LocalDate.now())) / 365.0;
        double drift = Math.pow(1 + annualReturn.doubleValue() / 100.0, years);
        double noise = 1 + ThreadLocalRandom.current().nextDouble(-0.04, 0.04);
        return buyPrice.multiply(BigDecimal.valueOf(drift * noise))
                .setScale(4, RoundingMode.HALF_UP).max(new BigDecimal("0.0001"));
    }

    private BigDecimal annualised(Portfolio portfolio, List<Holding> live) {
        if (live.isEmpty() || portfolio.getTotalInvested().signum() == 0) return BigDecimal.ZERO;
        LocalDate earliest = live.stream().map(Holding::getPurchaseDate)
                .min(Comparator.naturalOrder()).orElse(LocalDate.now());
        return Money.annualised(portfolio.getTotalInvested(), portfolio.getCurrentValue(), earliest);
    }

    private Map<String, BigDecimal> absolute(List<Holding> live,
                                             java.util.function.Function<Holding, String> key) {
        return live.stream().collect(Collectors.groupingBy(key, LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, Holding::getCurrentValue, BigDecimal::add)));
    }

    private Map<String, BigDecimal> percentages(List<Holding> live,
                                                java.util.function.Function<Holding, String> key,
                                                BigDecimal total) {
        return toPercent(absolute(live, key), total);
    }

    private Map<String, BigDecimal> toPercent(Map<String, BigDecimal> amounts, BigDecimal total) {
        if (total == null || total.signum() == 0) return Map.of();
        return amounts.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().multiply(Money.HUNDRED).divide(total, 2, RoundingMode.HALF_UP),
                (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Diversification as breadth plus evenness. Holding five funds in one asset
     * class is not diversification, so the spread across classes weighs most.
     */
    private BigDecimal diversificationScore(Map<String, BigDecimal> byClass, BigDecimal total, int count) {
        if (total.signum() == 0 || byClass.isEmpty()) return BigDecimal.ZERO;

        double breadth = Math.min(1.0, byClass.size() / 4.0);
        double herfindahl = byClass.values().stream()
                .mapToDouble(v -> Math.pow(v.doubleValue() / total.doubleValue(), 2)).sum();
        double evenness = 1.0 - herfindahl;
        double spread = Math.min(1.0, count / 6.0);

        return BigDecimal.valueOf((breadth * 0.45 + evenness * 0.40 + spread * 0.15) * 100)
                .setScale(1, RoundingMode.HALF_UP);
    }

    /** Observations that name a specific holding, which is the point of the premium tier. */
    private List<String> insights(List<Holding> live, BigDecimal concentration,
                                  BigDecimal diversification, Holding largest, Holding worst) {
        List<String> insights = new ArrayList<>();

        if (concentration.compareTo(BigDecimal.valueOf(40)) > 0) {
            insights.add("%s accounts for %s%% of your portfolio. Above 40%% in one position means your outcome is driven by one fund rather than by your asset allocation."
                    .formatted(largest.getProduct().getName(), concentration));
        }
        if (diversification.compareTo(BigDecimal.valueOf(50)) < 0) {
            insights.add("Your diversification score is %s out of 100. Adding an asset class you do not currently hold would reduce volatility more than another fund in the same class."
                    .formatted(diversification));
        }
        long nearLongTerm = live.stream().filter(h -> !h.isLongTerm() && h.holdingDays() > 300).count();
        if (nearLongTerm > 0) {
            insights.add("%d holding(s) cross the one year mark within about two months. Deferring any redemption until then moves the gain into the long-term tax bracket."
                    .formatted(nearLongTerm));
        }
        if (worst.gainPct().compareTo(BigDecimal.valueOf(-15)) < 0) {
            insights.add("%s is down %s%%. Check whether the fall reflects the whole asset class or this fund specifically; the first is noise, the second is worth investigating."
                    .formatted(worst.getProduct().getName(), worst.gainPct().abs()));
        }
        if (insights.isEmpty()) {
            insights.add("Your portfolio is well balanced with no single position dominating. Continue your contributions and review annually.");
        }
        return insights;
    }

    private Map<Long, String> productNames(List<Transaction> list) {
        if (list.isEmpty()) return Map.of();
        return products.findAllById(list.stream().map(Transaction::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
    }

    private String reference(String prefix) {
        return "IW-%s-%s-%04d".formatted(prefix, LocalDate.now().toString().replace("-", ""),
                ThreadLocalRandom.current().nextInt(1000, 9999));
    }
}
