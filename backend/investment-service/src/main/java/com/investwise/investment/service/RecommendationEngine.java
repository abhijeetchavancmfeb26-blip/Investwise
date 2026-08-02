package com.investwise.investment.service;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Scores the catalogue against an investor and allocates their money across it.
 * <p>
 * Collapsed from the original's {@code ScoringRule} functional interface,
 * {@code ScoringRules} holder of seven weighted lambdas and a
 * {@code RecommendationContext} builder into one class with a plain input record
 * and four named scoring methods. The behaviour is the same; the indirection is not.
 * <p>
 * Three stages: score everything, keep the best few per asset class, then divide
 * the money according to the profile's strategic split.
 */
@Slf4j
@Component
public class RecommendationEngine {

    private static final int MAX_PER_ASSET_CLASS = 3;
    private static final int MAX_TOTAL = 8;

    /** Everything the scorer needs, in one place. */
    public record Input(Enums.RiskProfile profile, int horizonYears, BigDecimal investable,
                        Enums.GoalType goalType, boolean premiumMember) {

        public boolean isShortHorizon() { return horizonYears <= 3; }

        public boolean isLongHorizon() { return horizonYears >= 7; }
    }

    public record Scored(Product product, double score, String rationale) { }

    public record Allocated(Product product, double score, String rationale,
                            BigDecimal allocationPct, BigDecimal amount) { }

    // ------------------------------------------------------------------
    //  Stage 1 — score
    // ------------------------------------------------------------------

    /**
     * Weights, stated once and visible: risk and horizon dominate because getting
     * either wrong causes real harm, whereas a slightly costlier fund does not.
     */
    public List<Scored> score(List<Product> candidates, Input input) {
        return candidates.stream()
                // Hard constraint first: money locked past the goal date is unusable
                // however attractive the product otherwise looks, so it is excluded
                // outright rather than merely scored down.
                .filter(product -> product.getLockInMonths() / 12 <= input.horizonYears())
                .map(product -> {
                    double weighted =
                            riskFit(product, input) * 0.35
                          + horizonFit(product, input) * 0.25
                          + goalFit(product, input) * 0.20
                          + qualityAndCost(product) * 0.20;

                    double pct = Math.clamp(weighted * 100, 0, 100);
                    return new Scored(product, pct, explain(product, input, pct));
                })
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();
    }

    /** Is the product's volatility inside the investor's tolerance? */
    private double riskFit(Product product, Input input) {
        int ceiling = input.profile().maxRiskScore();
        int level = product.getRiskLevel().score();
        // Beyond tolerance is penalised heavily but not excluded, so a marginally
        // aggressive fund can still surface for a long horizon.
        return level > ceiling
                ? Math.max(0, 0.35 - 0.15 * (level - ceiling))
                : 1.0 - (ceiling - level) * 0.12;
    }

    /**
     * Does the product's volatility suit the time available? Lock-in is already
     * handled by the filter in {@link #score}, so this is purely about horizon.
     */
    private double horizonFit(Product product, Input input) {
        int level = product.getRiskLevel().score();
        if (input.isShortHorizon()) {
            return switch (level) { case 1 -> 1.0; case 2 -> 0.85; case 3 -> 0.5; case 4 -> 0.2; default -> 0.05; };
        }
        if (input.isLongHorizon()) {
            return switch (level) { case 1 -> 0.45; case 2 -> 0.6; case 3 -> 0.85; case 4 -> 1.0; default -> 0.9; };
        }
        return switch (level) { case 1 -> 0.65; case 2 -> 0.8; case 3 -> 1.0; case 4 -> 0.7; default -> 0.4; };
    }

    /** Certain goals map naturally onto certain instruments. */
    private double goalFit(Product product, Input input) {
        if (input.goalType() == null) return 0.7;
        Enums.Category category = product.getCategory();

        return switch (input.goalType()) {
            case EMERGENCY_FUND -> category == Enums.Category.DEBT ? 1.0 : 0.15;
            case TAX_SAVING -> category == Enums.Category.ELSS ? 1.0 : 0.25;
            case RETIREMENT, WEALTH_CREATION -> switch (category) {
                case EQUITY -> 1.0;
                case HYBRID -> 0.8;
                case DEBT, GOLD -> 0.6;
                default -> 0.45;
            };
            case CHILD_EDUCATION, CHILD_MARRIAGE, HIGHER_EDUCATION -> switch (category) {
                case EQUITY, HYBRID -> 0.95;
                case DEBT, GOLD -> 0.7;
                default -> 0.5;
            };
            case HOME_PURCHASE, VEHICLE_PURCHASE, TRAVEL -> switch (category) {
                case DEBT -> 0.95;
                case HYBRID -> 0.7;
                default -> 0.35;
            };
            case OTHER -> 0.7;
        };
    }

    /** Rating, expected return and cost, blended — the "is this a good fund" factors. */
    private double qualityAndCost(Product product) {
        double rating = (product.getRating() - 1) / 4.0;
        double expectedReturn = Math.clamp((product.getExpectedReturn().doubleValue() - 5.0) / 15.0, 0, 1);
        double cost = product.getExpenseRatio() == null ? 0.75
                : Math.clamp(1.0 - product.getExpenseRatio().doubleValue() / 2.0, 0, 1);
        return rating * 0.4 + expectedReturn * 0.4 + cost * 0.2;
    }

    /** A written justification, assembled from whichever factors actually stood out. */
    private String explain(Product product, Input input, double score) {
        StringBuilder text = new StringBuilder();

        if (product.getRiskLevel().suitableFor(input.profile())) {
            text.append("Risk level %s sits within your %s profile. ".formatted(
                    product.getRiskLevel().name().toLowerCase().replace('_', ' '),
                    input.profile().name().toLowerCase()));
        } else {
            text.append("Slightly above your usual risk band, included for diversification. ");
        }
        if (input.isLongHorizon() && product.getRiskLevel().score() >= 3) {
            text.append("Your %d year horizon gives volatility time to even out. ".formatted(input.horizonYears()));
        } else if (input.isShortHorizon() && product.getRiskLevel().score() <= 2) {
            text.append("Capital stability suits your %d year horizon. ".formatted(input.horizonYears()));
        }
        if (product.getExpenseRatio() != null && product.getExpenseRatio().doubleValue() <= 0.5) {
            text.append("Low expense ratio of %s%% preserves more of the return. ".formatted(product.getExpenseRatio()));
        }
        if (product.getRating() >= 4) {
            text.append("Rated %d of 5 by our research desk. ".formatted(product.getRating()));
        }
        if (product.hasLockIn()) {
            text.append("Note the %d month lock-in. ".formatted(product.getLockInMonths()));
        }
        return text.append("Overall match %.0f%%.".formatted(score)).toString();
    }

    // ------------------------------------------------------------------
    //  Stage 2 — select
    // ------------------------------------------------------------------

    /**
     * Keeps the strongest few products per asset class.
     * <p>
     * A bounded min-heap per class: adding is O(log k) and the weakest entry is
     * evicted in constant time, so the pass stays cheap however large the catalogue.
     */
    public List<Scored> select(List<Scored> scored) {
        Map<Enums.AssetClass, PriorityQueue<Scored>> byClass = new EnumMap<>(Enums.AssetClass.class);

        for (Scored candidate : scored) {
            PriorityQueue<Scored> heap = byClass.computeIfAbsent(
                    candidate.product().getCategory().assetClass(),
                    key -> new PriorityQueue<>(Comparator.comparingDouble(Scored::score)));

            heap.offer(candidate);
            if (heap.size() > MAX_PER_ASSET_CLASS) {
                heap.poll();
            }
        }

        return byClass.values().stream()
                .flatMap(java.util.Collection::stream)
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(MAX_TOTAL)
                .toList();
    }

    // ------------------------------------------------------------------
    //  Stage 3 — allocate
    // ------------------------------------------------------------------

    /**
     * Divides {@code investable} across the selection according to the profile's
     * strategic split, weighting by relative score within each asset class.
     */
    public List<Allocated> allocate(List<Scored> selected, Input input, BigDecimal investable) {
        if (selected.isEmpty() || investable == null || investable.signum() <= 0) {
            return List.of();
        }

        Map<Enums.AssetClass, Integer> targets = targets(input);
        Map<Enums.AssetClass, List<Scored>> grouped = selected.stream()
                .collect(Collectors.groupingBy(s -> s.product().getCategory().assetClass(),
                        LinkedHashMap::new, Collectors.toList()));

        // An asset class with no eligible product hands its share back
        int available = grouped.keySet().stream().mapToInt(k -> targets.getOrDefault(k, 0)).sum();
        if (available == 0) return List.of();

        List<Allocated> result = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            int classTarget = targets.getOrDefault(entry.getKey(), 0);
            if (classTarget == 0) continue;

            // Rescale so the classes we can actually fill total 100%
            double classShare = classTarget * 100.0 / available;
            double totalScore = entry.getValue().stream().mapToDouble(Scored::score).sum();

            for (Scored member : entry.getValue()) {
                double within = totalScore == 0 ? 1.0 / entry.getValue().size() : member.score() / totalScore;
                BigDecimal pct = BigDecimal.valueOf(classShare * within).setScale(2, RoundingMode.HALF_UP);
                BigDecimal amount = investable.multiply(pct)
                        .divide(Money.HUNDRED, 2, RoundingMode.HALF_UP);
                result.add(new Allocated(member.product(), member.score(), member.rationale(), pct, amount));
            }
        }

        return balance(result, investable);
    }

    /** Strategic weights, with hybrid carved out of equity and debt rather than added on top. */
    private Map<Enums.AssetClass, Integer> targets(Input input) {
        Enums.RiskProfile profile = input.profile();
        int hybrid = (profile.equityPct() + profile.debtPct()) / 6;
        int fromEquity = (int) Math.round(hybrid * profile.equityPct()
                / (double) Math.max(1, profile.equityPct() + profile.debtPct()));

        Map<Enums.AssetClass, Integer> targets = new EnumMap<>(Enums.AssetClass.class);
        targets.put(Enums.AssetClass.EQUITY, Math.max(0, profile.equityPct() - fromEquity));
        targets.put(Enums.AssetClass.DEBT, Math.max(0, profile.debtPct() - (hybrid - fromEquity)));
        targets.put(Enums.AssetClass.GOLD, profile.goldPct());
        targets.put(Enums.AssetClass.HYBRID, hybrid);
        targets.put(Enums.AssetClass.REAL_ESTATE, input.premiumMember() ? 5 : 0);
        return targets;
    }

    /**
     * Rounding leaves a few paise unassigned. Pushing the correction onto the
     * strongest recommendation makes the basket sum to exactly the amount invested.
     */
    private List<Allocated> balance(List<Allocated> allocations, BigDecimal investable) {
        List<Allocated> ordered = allocations.stream()
                .sorted(Comparator.comparingDouble(Allocated::score).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        BigDecimal total = ordered.stream().map(Allocated::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = investable.subtract(total);

        if (difference.signum() != 0 && !ordered.isEmpty()) {
            Allocated best = ordered.get(0);
            ordered.set(0, new Allocated(best.product(), best.score(), best.rationale(),
                    best.allocationPct(), best.amount().add(difference)));
        }
        return ordered;
    }

    /** Blended expected return of the basket, weighted by allocation. */
    public BigDecimal expectedReturn(List<Allocated> allocations) {
        BigDecimal totalPct = allocations.stream()
                .map(Allocated::allocationPct).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPct.signum() == 0) return BigDecimal.ZERO;

        return allocations.stream()
                .map(a -> a.product().getExpectedReturn().multiply(a.allocationPct()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(totalPct, 2, RoundingMode.HALF_UP);
    }
}
