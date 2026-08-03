package com.investwise.investment.service;

import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.repository.jpa.GoalRepository;
import com.investwise.investment.repository.jpa.PaymentRepository;
import com.investwise.investment.repository.jpa.PortfolioRepository;
import com.investwise.investment.repository.jpa.ProductRepository;
import com.investwise.investment.repository.jpa.RecommendationRepository;
import com.investwise.investment.repository.jpa.RiskRepository;
import com.investwise.investment.repository.jpa.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Platform statistics and the public calculators.
 * <p>
 * The original ran a dozen concurrent {@code CompletableFuture}s at the same
 * database for this page. These are indexed aggregates returning in single-digit
 * milliseconds; sequential is simpler to read and no slower in practice.
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ProductRepository products;
    private final GoalRepository goals;
    private final RecommendationRepository recommendations;
    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final PortfolioRepository portfolios;
    private final RiskRepository risks;

    @Transactional(readOnly = true)
    public Responses.AdminStats adminStats() {
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        return new Responses.AdminStats(
                products.count(), products.countByActiveTrue(), products.countByPremiumOnlyTrue(),
                products.averageExpectedReturn(),
                goals.count(), goals.countByStatus(Enums.GoalStatus.ACHIEVED),
                recommendations.countSince(monthAgo),
                subscriptions.countByStatus(Enums.SubscriptionStatus.ACTIVE),
                subscriptions.countByStatus(Enums.SubscriptionStatus.EXPIRED),
                subscriptions.recurringRevenue(),
                payments.totalRevenue(), payments.revenueSince(monthAgo),
                payments.countByStatus(Enums.PaymentStatus.SUCCESS),
                payments.countByStatus(Enums.PaymentStatus.FAILED),
                portfolios.sumInvested(), portfolios.sumCurrentValue(),
                counts(products.countByCategory()),
                counts(goals.countByType()),
                counts(risks.countByProfile()),
                amounts(payments.revenueByMonth(LocalDateTime.now().minusMonths(12))),
                counts(subscriptions.countActiveByPlan()),
                payments.findTop10ByStatusOrderByCreatedAtDesc(Enums.PaymentStatus.SUCCESS)
                        .stream().map(Responses.PaymentView::from).toList(),
                recommendations.mostRecommended().stream().limit(5)
                        .map(row -> "%s (%s)".formatted(row[0], row[1])).toList());
    }

    // ---------------- public calculators ----------------

    public Responses.CalculatorView sip(BigDecimal monthly, BigDecimal rate, int years) {
        BigDecimal invested = monthly.multiply(BigDecimal.valueOf(years * 12L));
        BigDecimal maturity = Money.sipFutureValue(monthly, rate, years * 12);
        return new Responses.CalculatorView(invested, maturity.subtract(invested), maturity, null,
                Money.yearsToDouble(rate),
                projection(years, year -> Money.sipFutureValue(monthly, rate, year * 12)));
    }

    public Responses.CalculatorView lumpsum(BigDecimal principal, BigDecimal rate, int years) {
        BigDecimal maturity = Money.futureValue(principal, rate, years);
        return new Responses.CalculatorView(principal, maturity.subtract(principal), maturity, null,
                Money.yearsToDouble(rate),
                projection(years, year -> Money.futureValue(principal, rate, year)));
    }

    /** Inflates the target first, so the answer funds the goal in future rupees. */
    public Responses.CalculatorView goal(BigDecimal target, BigDecimal rate, int years, double inflationPct) {
        BigDecimal inflated = Money.inflate(target, inflationPct, years);
        BigDecimal required = Money.requiredSip(inflated, rate, years * 12);
        BigDecimal invested = required.multiply(BigDecimal.valueOf(years * 12L));

        return new Responses.CalculatorView(invested, inflated.subtract(invested), inflated, required,
                Money.yearsToDouble(rate),
                projection(years, year -> Money.sipFutureValue(required, rate, year * 12)));
    }

    private Map<String, BigDecimal> projection(int years, Function<Integer, BigDecimal> valueAt) {
        Map<String, BigDecimal> projection = new LinkedHashMap<>();
        for (int year = 1; year <= years; year++) {
            projection.put("Year " + year, valueAt.apply(year));
        }
        return projection;
    }

    // ---------------- helpers ----------------

    private Map<String, Long> counts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row[0]), row -> ((Number) row[1]).longValue(),
                (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, BigDecimal> amounts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> String.valueOf(row[0]), row -> (BigDecimal) row[1],
                (a, b) -> a, LinkedHashMap::new));
    }
}
