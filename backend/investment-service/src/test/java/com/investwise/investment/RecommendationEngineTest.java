package com.investwise.investment;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Product;
import com.investwise.investment.service.RecommendationEngine;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationEngine")
class RecommendationEngineTest {

    private RecommendationEngine engine;
    private List<Product> catalogue;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
        catalogue = List.of(
                product(1L, "IW-EQ-001", "Large Cap", Enums.Category.EQUITY, Enums.RiskLevel.MODERATE, "13.50", "5000", 0, "1.05", 4),
                product(2L, "IW-EQ-003", "Small Cap Alpha", Enums.Category.EQUITY, Enums.RiskLevel.VERY_HIGH, "19.20", "5000", 12, "1.68", 3),
                product(3L, "IW-DB-002", "Liquid Fund", Enums.Category.DEBT, Enums.RiskLevel.VERY_LOW, "6.40", "1000", 0, "0.16", 5),
                product(4L, "IW-DB-001", "Corporate Bond", Enums.Category.DEBT, Enums.RiskLevel.LOW, "7.60", "5000", 0, "0.58", 4),
                product(5L, "IW-GD-002", "Gold ETF", Enums.Category.GOLD, Enums.RiskLevel.LOW, "9.10", "1000", 0, "0.32", 4),
                product(6L, "IW-EQ-004", "Nifty 50 Index", Enums.Category.EQUITY, Enums.RiskLevel.MODERATE, "12.40", "1000", 0, "0.20", 5),
                product(7L, "IW-HY-001", "Balanced Advantage", Enums.Category.HYBRID, Enums.RiskLevel.MODERATE, "11.60", "5000", 0, "1.12", 4));
    }

    private Product product(Long id, String code, String name, Enums.Category category,
                            Enums.RiskLevel risk, String expectedReturn, String minInvestment,
                            int lockIn, String expenseRatio, int rating) {
        return Product.builder().id(id).code(code).name(name).category(category).riskLevel(risk)
                .expectedReturn(new BigDecimal(expectedReturn))
                .minInvestment(new BigDecimal(minInvestment))
                .lockInMonths(lockIn).expenseRatio(new BigDecimal(expenseRatio))
                .rating(rating).active(true).premiumOnly(false).build();
    }

    private RecommendationEngine.Input input(Enums.RiskProfile profile, int horizon, Enums.GoalType goal) {
        return new RecommendationEngine.Input(profile, horizon, new BigDecimal("200000"), goal, false);
    }

    @Test
    @DisplayName("scores every candidate and returns them best first")
    void scoresAndOrders() {
        var scored = engine.score(catalogue, input(Enums.RiskProfile.BALANCED, 10, Enums.GoalType.WEALTH_CREATION));

        assertThat(scored).isNotEmpty();
        assertThat(scored).isSortedAccordingTo((a, b) -> Double.compare(b.score(), a.score()));
        assertThat(scored).allSatisfy(item -> {
            assertThat(item.score()).isBetween(0.0, 100.0);
            assertThat(item.rationale()).isNotBlank();
        });
    }

    @Test
    @DisplayName("favours low risk for a conservative investor with a short horizon")
    void conservativeGetsLowRisk() {
        var scored = engine.score(catalogue, input(Enums.RiskProfile.CONSERVATIVE, 3, Enums.GoalType.EMERGENCY_FUND));
        assertThat(scored.get(0).product().getRiskLevel().score()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("favours equity for an aggressive investor with a long horizon")
    void aggressiveGetsEquity() {
        var scored = engine.score(catalogue, input(Enums.RiskProfile.AGGRESSIVE, 20, Enums.GoalType.WEALTH_CREATION));
        assertThat(scored.get(0).product().getCategory().assetClass())
                .isEqualTo(Enums.AssetClass.EQUITY);
    }

    @Test
    @DisplayName("excludes a product whose lock-in outlives the goal")
    void excludesLockedBeyondHorizon() {
        Product ppf = product(9L, "IW-DB-004", "PPF", Enums.Category.DEBT,
                Enums.RiskLevel.VERY_LOW, "7.10", "500", 180, "0.10", 5);

        List<Product> withPpf = new ArrayList<>(catalogue);
        withPpf.add(ppf);

        // A 15 year lock-in cannot fund a 3 year goal
        var scored = engine.score(withPpf, input(Enums.RiskProfile.CONSERVATIVE, 3, Enums.GoalType.EMERGENCY_FUND));
        assertThat(scored).noneMatch(item -> "IW-DB-004".equals(item.product().getCode()));
    }

    @Test
    @DisplayName("keeps at most three products per asset class and eight overall")
    void selectionIsBounded() {
        var selected = engine.select(engine.score(catalogue,
                input(Enums.RiskProfile.BALANCED, 10, Enums.GoalType.WEALTH_CREATION)));

        assertThat(selected).hasSizeLessThanOrEqualTo(8);
        assertThat(selected.stream().collect(Collectors.groupingBy(
                        s -> s.product().getCategory().assetClass(), Collectors.counting())).values())
                .allMatch(count -> count <= 3);
    }

    @Test
    @DisplayName("allocates the investable amount exactly, with no rupee lost to rounding")
    void allocationSumsExactly() {
        var ctx = input(Enums.RiskProfile.BALANCED, 10, Enums.GoalType.WEALTH_CREATION);
        BigDecimal investable = new BigDecimal("200000");

        var allocated = engine.allocate(engine.select(engine.score(catalogue, ctx)), ctx, investable);

        assertThat(allocated).isNotEmpty();
        assertThat(allocated.stream().map(RecommendationEngine.Allocated::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(investable);
    }

    @Test
    @DisplayName("allocation percentages total approximately 100")
    void percentagesTotalHundred() {
        var ctx = input(Enums.RiskProfile.AGGRESSIVE, 15, Enums.GoalType.RETIREMENT);
        var allocated = engine.allocate(engine.select(engine.score(catalogue, ctx)), ctx, new BigDecimal("500000"));

        assertThat(allocated.stream().map(RecommendationEngine.Allocated::allocationPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue())
                .isCloseTo(100.0, Offset.offset(2.0));
    }

    @Test
    @DisplayName("still totals 100 for a premium member, who gets an alternatives sleeve")
    void premiumAllocationStillTotalsHundred() {
        var ctx = new RecommendationEngine.Input(Enums.RiskProfile.BALANCED, 15,
                new BigDecimal("300000"), Enums.GoalType.RETIREMENT, true);

        var allocated = engine.allocate(engine.select(engine.score(catalogue, ctx)), ctx, new BigDecimal("300000"));

        assertThat(allocated.stream().map(RecommendationEngine.Allocated::allocationPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue())
                .isCloseTo(100.0, Offset.offset(2.0));
    }

    @Test
    @DisplayName("blends the expected return across the basket")
    void expectedReturnIsWeighted() {
        var ctx = input(Enums.RiskProfile.BALANCED, 10, Enums.GoalType.WEALTH_CREATION);
        var allocated = engine.allocate(engine.select(engine.score(catalogue, ctx)), ctx, new BigDecimal("200000"));

        assertThat(engine.expectedReturn(allocated).doubleValue()).isBetween(6.4, 19.2);
    }

    @Test
    @DisplayName("returns an empty basket rather than failing on empty input")
    void handlesEmptyCatalogue() {
        var ctx = input(Enums.RiskProfile.BALANCED, 10, null);
        assertThat(engine.score(List.of(), ctx)).isEmpty();
        assertThat(engine.allocate(List.of(), ctx, new BigDecimal("100000"))).isEmpty();
    }

    @Test
    @DisplayName("steers a tax saving goal towards ELSS")
    void goalAffinitySteersTowardsElss() {
        Product elss = product(8L, "IW-EQ-005", "ELSS Tax Saver", Enums.Category.ELSS,
                Enums.RiskLevel.HIGH, "14.70", "500", 36, "1.75", 4);

        List<Product> withElss = new ArrayList<>(catalogue);
        withElss.add(elss);

        var scored = engine.score(withElss, input(Enums.RiskProfile.AGGRESSIVE, 10, Enums.GoalType.TAX_SAVING));
        double elssScore = scored.stream().filter(s -> s.product().getId() == 8L)
                .findFirst().orElseThrow().score();
        double indexScore = scored.stream().filter(s -> s.product().getId() == 6L)
                .findFirst().orElseThrow().score();

        assertThat(elssScore).isGreaterThan(indexScore);
    }
}
