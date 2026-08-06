package com.investwise.investment.dto;

import com.investwise.investment.model.Article;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Goal;
import com.investwise.investment.model.Holding;
import com.investwise.investment.model.Payment;
import com.investwise.investment.model.Plan;
import com.investwise.investment.model.Product;
import com.investwise.investment.model.RiskAssessment;
import com.investwise.investment.model.Subscription;
import com.investwise.investment.model.Transaction;
import com.investwise.investment.service.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Every outbound payload, each with a static {@code from()} in place of a mapper class. */
public final class Responses {

    private Responses() { }

    // ---------------- catalogue ----------------

    public record ProductView(Long id, String code, String name, String description,
                              Enums.Category category, String categoryLabel, String assetClass,
                              Enums.RiskLevel riskLevel, BigDecimal expectedReturn,
                              BigDecimal minInvestment, Integer lockInMonths, String fundHouse,
                              BigDecimal expenseRatio, Integer rating, boolean premiumOnly,
                              boolean active) {

        public static ProductView from(Product p) {
            return new ProductView(p.getId(), p.getCode(), p.getName(), p.getDescription(),
                    p.getCategory(), p.getCategory().label(), p.getCategory().assetClass().name(),
                    p.getRiskLevel(), p.getExpectedReturn(), p.getMinInvestment(),
                    p.getLockInMonths(), p.getFundHouse(), p.getExpenseRatio(),
                    p.getRating(), p.isPremiumOnly(), p.isActive());
        }
    }

    // ---------------- goals ----------------

    public record GoalView(Long id, String title, String description, Enums.GoalType goalType,
                           String goalTypeLabel, BigDecimal targetAmount, BigDecimal currentAmount,
                           BigDecimal monthlyContribution, LocalDate targetDate, Enums.Priority priority,
                           Enums.GoalStatus status, BigDecimal progressPct, BigDecimal shortfall,
                           long monthsRemaining, BigDecimal requiredMonthly, BigDecimal projectedValue,
                           BigDecimal inflatedTarget, LocalDateTime createdAt) {

        /** @param rate the investor's own blended return assumption, not a hard-coded one */
        public static GoalView from(Goal g, BigDecimal rate) {
            int months = (int) Math.max(1, g.monthsRemaining());
            int years = Math.max(1, months / 12);

            BigDecimal projected = Money.sipFutureValue(g.getMonthlyContribution(), rate, months)
                    .add(Money.futureValue(g.getCurrentAmount(), rate, years));

            return new GoalView(g.getId(), g.getTitle(), g.getDescription(), g.getGoalType(),
                    g.getGoalType().label(), g.getTargetAmount(), g.getCurrentAmount(),
                    g.getMonthlyContribution(), g.getTargetDate(), g.getPriority(), g.getStatus(),
                    g.progressPct(), g.shortfall(), g.monthsRemaining(),
                    Money.requiredSip(g.shortfall(), rate, months), projected,
                    Money.inflate(g.getTargetAmount(), g.getGoalType().inflationPct(), years),
                    g.getCreatedAt());
        }
    }

    // ---------------- risk ----------------

    public record RiskView(Long id, Integer score, Enums.RiskProfile profile, String summary,
                           Integer equityPct, Integer debtPct, Integer goldPct, Integer horizonYears,
                           Map<String, Integer> breakdown, List<String> guidance,
                           LocalDateTime createdAt) {

        public static RiskView from(RiskAssessment r, String summary,
                                    Map<String, Integer> breakdown, List<String> guidance) {
            return new RiskView(r.getId(), r.getScore(), r.getProfile(), summary,
                    r.getEquityPct(), r.getDebtPct(), r.getGoldPct(), r.getHorizonYears(),
                    breakdown, guidance, r.getCreatedAt());
        }
    }

    // ---------------- recommendations ----------------

    public record RecommendationItem(Long productId, String productCode, String productName,
                                     String category, String assetClass, String riskLevel,
                                     BigDecimal expectedReturn, BigDecimal allocationPct,
                                     BigDecimal amount, BigDecimal matchScore, String rationale,
                                     Integer lockInMonths, BigDecimal minInvestment) { }

    public record RecommendationView(Long goalId, String goalTitle, Enums.RiskProfile riskProfile,
                                     Integer riskScore, BigDecimal investableAmount, Integer horizonYears,
                                     BigDecimal expectedReturn, BigDecimal projectedValue,
                                     List<RecommendationItem> items,
                                     Map<String, BigDecimal> allocationByAssetClass,
                                     LocalDateTime createdAt) { }

    // ---------------- portfolio ----------------

    public record HoldingView(Long id, Long productId, String productName, String productCode,
                              String category, String riskLevel, Long goalId, String goalTitle,
                              BigDecimal units, BigDecimal buyPrice, BigDecimal currentPrice,
                              BigDecimal investedAmount, BigDecimal currentValue, BigDecimal gain,
                              BigDecimal gainPct, BigDecimal annualisedPct, LocalDate purchaseDate,
                              long holdingDays, boolean longTerm) {

        public static HoldingView from(Holding h) {
            return new HoldingView(h.getId(), h.getProduct().getId(), h.getProduct().getName(),
                    h.getProduct().getCode(), h.getProduct().getCategory().label(),
                    h.getProduct().getRiskLevel().name(),
                    h.getGoal() == null ? null : h.getGoal().getId(),
                    h.getGoal() == null ? null : h.getGoal().getTitle(),
                    h.getUnits(), h.getBuyPrice(), h.getCurrentPrice(), h.getInvestedAmount(),
                    h.getCurrentValue(), h.gain(), h.gainPct(),
                    Money.annualised(h.getInvestedAmount(), h.getCurrentValue(), h.getPurchaseDate()),
                    h.getPurchaseDate(), h.holdingDays(), h.isLongTerm());
        }
    }

    public record PortfolioView(Long id, BigDecimal totalInvested, BigDecimal currentValue,
                                BigDecimal gain, BigDecimal gainPct, BigDecimal annualisedPct,
                                int holdingCount, List<HoldingView> holdings,
                                Map<String, BigDecimal> allocationByCategory,
                                Map<String, BigDecimal> allocationByRisk) { }

    public record TransactionView(Long id, Long productId, String productName,
                                  Enums.TransactionType type, BigDecimal units, BigDecimal price,
                                  BigDecimal amount, String referenceNo, LocalDateTime createdAt) {

        public static TransactionView from(Transaction t, String productName) {
            return new TransactionView(t.getId(), t.getProductId(), productName, t.getType(),
                    t.getUnits(), t.getPrice(), t.getAmount(), t.getReferenceNo(), t.getCreatedAt());
        }
    }

    public record DashboardView(BigDecimal totalInvested, BigDecimal currentValue, BigDecimal gain,
                                BigDecimal gainPct, int totalGoals, int goalsOnTrack, int goalsBehind,
                                int goalsAchieved, BigDecimal totalGoalTarget, BigDecimal totalGoalProgress,
                                String riskProfile, Integer riskScore, boolean riskAssessmentComplete,
                                String tier, boolean premium, long subscriptionDaysRemaining,
                                Map<String, BigDecimal> allocationByCategory,
                                List<GoalView> upcomingGoals, List<HoldingView> topHoldings,
                                List<TransactionView> recentTransactions) { }

    /** Premium analytics, including the rebalancing plan the original kept separate. */
    public record AnalyticsView(BigDecimal totalInvested, BigDecimal currentValue, BigDecimal gain,
                                BigDecimal returnPct, BigDecimal annualisedPct,
                                String bestHolding, BigDecimal bestReturnPct,
                                String worstHolding, BigDecimal worstReturnPct,
                                BigDecimal diversificationScore, BigDecimal concentrationPct,
                                String largestHolding, BigDecimal weightedExpectedReturn,
                                BigDecimal projected5Years, BigDecimal projected10Years,
                                BigDecimal longTermGains, BigDecimal shortTermGains,
                                Map<String, BigDecimal> allocationByAssetClass,
                                Map<String, BigDecimal> allocationByRisk,
                                List<String> insights, RebalanceView rebalance) { }

    public record RebalanceView(boolean actionNeeded, String summary, BigDecimal maxDriftPct,
                                Map<String, Drift> driftByAssetClass, List<Action> actions) {

        public record Drift(BigDecimal currentPct, BigDecimal targetPct, BigDecimal driftPct,
                            BigDecimal currentAmount, BigDecimal differenceAmount, String status) { }

        public record Action(int step, String type, String assetClass, BigDecimal amount, String rationale) { }
    }

    // ---------------- money ----------------

    public record PlanView(Long id, String code, String name, String description, Enums.Tier tier,
                           BigDecimal price, BigDecimal monthlyEquivalent, Integer durationMonths,
                           Integer maxGoals, boolean active, List<String> features) {

        public static PlanView from(Plan p) {
            return new PlanView(p.getId(), p.getCode(), p.getName(), p.getDescription(), p.getTier(),
                    p.getPrice(), p.monthlyEquivalent(), p.getDurationMonths(), p.getMaxGoals(),
                    p.isActive(), p.featureList());
        }
    }

    public record SubscriptionView(Long id, Long userId, PlanView plan, LocalDate startDate,
                                   LocalDate endDate, Enums.SubscriptionStatus status,
                                   boolean currentlyActive, long daysRemaining, boolean expiringSoon,
                                   LocalDateTime createdAt) {

        public static SubscriptionView from(Subscription s) {
            return new SubscriptionView(s.getId(), s.getUserId(), PlanView.from(s.getPlan()),
                    s.getStartDate(), s.getEndDate(), s.getStatus(), s.isCurrentlyActive(),
                    s.daysRemaining(), s.isExpiringSoon(), s.getCreatedAt());
        }
    }

    public record PaymentView(Long id, Long userId, String userEmail, String orderId, String paymentId,
                              BigDecimal amount, Enums.PaymentStatus status, String method,
                              String failureReason, String invoiceNo, String planName,
                              LocalDateTime createdAt) {

        public static PaymentView from(Payment p) {
            return new PaymentView(p.getId(), p.getUserId(), p.getUserEmail(), p.getOrderId(),
                    p.getPaymentId(), p.getAmount(), p.getStatus(), p.getMethod(),
                    p.getFailureReason(), p.getInvoiceNo(),
                    p.getSubscription() == null ? null : p.getSubscription().getPlan().getName(),
                    p.getCreatedAt());
        }
    }

    /** Everything the browser needs to open Razorpay Checkout. */
    public record OrderView(Long paymentId, String orderId, String razorpayKeyId, Long amountInPaise,
                            BigDecimal amount, String currency, String planName, String planCode,
                            String customerName, String customerEmail, String description) { }

    // ---------------- content ----------------

    public record ArticleView(Long id, String title, String slug, String summary, String content,
                              Enums.ArticleCategory category, String author, Integer readMinutes,
                              boolean premiumOnly, boolean published, Long viewCount,
                              LocalDateTime createdAt) {

        /** List responses omit the body; shipping full articles in a list is wasted bandwidth. */
        public static ArticleView from(Article a, boolean includeContent) {
            return new ArticleView(a.getId(), a.getTitle(), a.getSlug(), a.getSummary(),
                    includeContent ? a.getContent() : null, a.getCategory(), a.getAuthor(),
                    a.getReadMinutes(), a.isPremiumOnly(), a.isPublished(), a.getViewCount(),
                    a.getCreatedAt());
        }
    }

    // ---------------- calculators and admin ----------------

    public record CalculatorView(BigDecimal totalInvested, BigDecimal estimatedReturns,
                                 BigDecimal maturityValue, BigDecimal requiredMonthly,
                                 BigDecimal yearsToDouble, Map<String, BigDecimal> yearlyProjection) { }

    public record AdminStats(long totalProducts, long activeProducts, long premiumProducts,
                             BigDecimal averageExpectedReturn, long totalGoals, long goalsAchieved,
                             long recommendationsLast30Days, long activeSubscriptions,
                             long expiredSubscriptions, BigDecimal recurringRevenue,
                             BigDecimal totalRevenue, BigDecimal revenueLast30Days,
                             long successfulPayments, long failedPayments,
                             BigDecimal platformInvested, BigDecimal platformCurrentValue,
                             Map<String, Long> productsByCategory, Map<String, Long> goalsByType,
                             Map<String, Long> usersByRiskProfile, Map<String, BigDecimal> revenueByMonth,
                             Map<String, Long> subscriptionsByPlan, List<PaymentView> recentPayments,
                             List<String> mostRecommended) { }
}
