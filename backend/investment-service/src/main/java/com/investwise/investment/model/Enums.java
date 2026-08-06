package com.investwise.investment.model;

/**
 * The investment domain vocabulary, in one file.
 * <p>
 * The original spread seventeen enums across seventeen files. Grouping them here
 * makes the whole domain readable at a glance, and the behaviour that mattered
 * (risk-to-profile matching, allocation targets, inflation assumptions) is kept.
 */
public final class Enums {

    private Enums() { }

    /** Asset class. Categories map onto these so the allocator has something to balance. */
    public enum AssetClass { EQUITY, DEBT, GOLD, HYBRID, REAL_ESTATE }

    public enum Category {
        EQUITY("Equity", AssetClass.EQUITY),
        ELSS("Tax Saving (ELSS)", AssetClass.EQUITY),
        DEBT("Debt", AssetClass.DEBT),
        GOLD("Gold", AssetClass.GOLD),
        HYBRID("Hybrid", AssetClass.HYBRID),
        REAL_ESTATE("Real Estate", AssetClass.REAL_ESTATE);

        private final String label;
        private final AssetClass assetClass;

        Category(String label, AssetClass assetClass) {
            this.label = label;
            this.assetClass = assetClass;
        }

        public String label() { return label; }

        public AssetClass assetClass() { return assetClass; }
    }

    /** Product volatility band. The score lets a product be matched arithmetically. */
    public enum RiskLevel {
        VERY_LOW(1), LOW(2), MODERATE(3), HIGH(4), VERY_HIGH(5);

        private final int score;

        RiskLevel(int score) { this.score = score; }

        public int score() { return score; }

        public boolean suitableFor(RiskProfile profile) {
            return score <= profile.maxRiskScore();
        }
    }

    /** Investor appetite, with the strategic allocation each one implies. */
    public enum RiskProfile {
        CONSERVATIVE(0, 30, 2, 20, 65, 15),
        MODERATE(31, 55, 3, 45, 45, 10),
        BALANCED(56, 72, 4, 65, 27, 8),
        AGGRESSIVE(73, 100, 5, 85, 10, 5);

        private final int min, max, maxRiskScore, equity, debt, gold;

        RiskProfile(int min, int max, int maxRiskScore, int equity, int debt, int gold) {
            this.min = min; this.max = max; this.maxRiskScore = maxRiskScore;
            this.equity = equity; this.debt = debt; this.gold = gold;
        }

        public int minScore() { return min; }

        public int maxScore() { return max; }

        public int maxRiskScore() { return maxRiskScore; }

        public int equityPct() { return equity; }

        public int debtPct() { return debt; }

        public int goldPct() { return gold; }

        /** Maps a questionnaire total onto a profile; out-of-range scores clamp. */
        public static RiskProfile fromScore(int score) {
            for (RiskProfile profile : values()) {
                if (score >= profile.min && score <= profile.max) return profile;
            }
            return score < CONSERVATIVE.min ? CONSERVATIVE : AGGRESSIVE;
        }
    }

    /** Goal category, each with its own horizon and inflation assumption. */
    public enum GoalType {
        RETIREMENT("Retirement", 25, 6.0),
        CHILD_EDUCATION("Child's Education", 15, 9.0),
        CHILD_MARRIAGE("Child's Marriage", 18, 7.5),
        HOME_PURCHASE("Home Purchase", 8, 6.5),
        VEHICLE_PURCHASE("Vehicle Purchase", 4, 5.0),
        EMERGENCY_FUND("Emergency Fund", 1, 6.0),
        TRAVEL("Travel", 3, 5.5),
        WEALTH_CREATION("Wealth Creation", 12, 6.0),
        TAX_SAVING("Tax Saving", 3, 6.0),
        HIGHER_EDUCATION("Higher Education", 6, 9.0),
        OTHER("Other", 5, 6.0);

        private final String label;
        private final int horizonYears;
        private final double inflationPct;

        GoalType(String label, int horizonYears, double inflationPct) {
            this.label = label; this.horizonYears = horizonYears; this.inflationPct = inflationPct;
        }

        public String label() { return label; }

        public int horizonYears() { return horizonYears; }

        public double inflationPct() { return inflationPct; }
    }

    public enum GoalStatus { ACTIVE, ON_TRACK, BEHIND, ACHIEVED, CANCELLED }

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }

    public enum Knowledge { BEGINNER, INTERMEDIATE, ADVANCED, EXPERT }

    /** Stated reaction to a 20% drawdown — the tolerance half of the risk score. */
    public enum LossTolerance { SELL_EVERYTHING, SELL_SOME, HOLD, BUY_MORE }

    public enum TransactionType { BUY, REDEEM }

    public enum SubscriptionStatus { PENDING, ACTIVE, EXPIRED, CANCELLED }

    public enum PaymentStatus { CREATED, SUCCESS, FAILED }

    public enum Tier {
        FREE(3), PREMIUM(999), ELITE(999);

        private final int maxGoals;

        Tier(int maxGoals) { this.maxGoals = maxGoals; }

        public int maxGoals() { return maxGoals; }

        public boolean isPremium() { return this != FREE; }
    }

    public enum ArticleCategory { BASICS, STRATEGY, PRODUCTS, TAX, RETIREMENT }

    /** Report kinds, and whether each one needs a paid plan. */
    public enum ReportType {
        PORTFOLIO_SUMMARY("Portfolio Summary", false),
        GOAL_PROGRESS("Goal Progress", false),
        TRANSACTION_STATEMENT("Transaction Statement", false),
        RECOMMENDATION_SHEET("Recommendation Sheet", false),
        PREMIUM_ANALYTICS("Premium Analytics Pack", true),
        TAX_STATEMENT("Capital Gains Statement", true);

        private final String label;
        private final boolean premiumOnly;

        ReportType(String label, boolean premiumOnly) {
            this.label = label; this.premiumOnly = premiumOnly;
        }

        public String label() { return label; }

        public boolean premiumOnly() { return premiumOnly; }
    }
}
