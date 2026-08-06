package com.investwise.investment;

import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Goal;
import com.investwise.investment.model.Holding;
import com.investwise.investment.model.Plan;
import com.investwise.investment.model.Portfolio;
import com.investwise.investment.model.Product;
import com.investwise.investment.model.Subscription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Behaviour that lives on the entities, tested without Spring. */
@DisplayName("Domain model")
class DomainModelTest {

    private Product product(String name, Enums.RiskLevel risk, String expectedReturn, int rating) {
        return Product.builder().id(1L).code("IW-EQ-001").name(name)
                .category(Enums.Category.EQUITY).riskLevel(risk)
                .expectedReturn(new BigDecimal(expectedReturn))
                .minInvestment(new BigDecimal("5000")).lockInMonths(0)
                .expenseRatio(new BigDecimal("1.00")).rating(rating).active(true).build();
    }

    @Nested
    @DisplayName("Goal")
    class Goals {

        private Goal goal(String target, String current, int monthsAway) {
            Goal goal = Goal.builder().id(1L).userId(1L).title("Test goal")
                    .goalType(Enums.GoalType.WEALTH_CREATION)
                    .targetAmount(new BigDecimal(target))
                    .currentAmount(new BigDecimal(current))
                    .monthlyContribution(new BigDecimal("5000"))
                    .targetDate(LocalDate.now().plusMonths(monthsAway))
                    .priority(Enums.Priority.MEDIUM).status(Enums.GoalStatus.ACTIVE).build();
            goal.setCreatedAt(LocalDateTime.now().minusMonths(12));
            return goal;
        }

        @Test
        @DisplayName("computes progress, shortfall and months remaining")
        void derivedFigures() {
            Goal goal = goal("1000000", "250000", 60);
            assertThat(goal.progressPct()).isEqualByComparingTo("25.00");
            assertThat(goal.shortfall()).isEqualByComparingTo("750000");
            assertThat(goal.monthsRemaining()).isBetween(58L, 60L);
        }

        @Test
        @DisplayName("caps progress at 100 percent and never reports a negative shortfall")
        void capsProgress() {
            Goal goal = goal("100000", "150000", 12);
            assertThat(goal.progressPct()).isEqualByComparingTo("100.00");
            assertThat(goal.shortfall()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("marks the goal achieved once contributions reach the target")
        void marksAchieved() {
            Goal goal = goal("100000", "90000", 12);
            goal.contribute(new BigDecimal("10000"));

            assertThat(goal.getStatus()).isEqualTo(Enums.GoalStatus.ACHIEVED);
            assertThat(goal.getCurrentAmount()).isEqualByComparingTo("100000");
        }

        @Test
        @DisplayName("rejects a zero or negative contribution")
        void rejectsInvalidContribution() {
            Goal goal = goal("100000", "0", 12);
            assertThatThrownBy(() -> goal.contribute(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> goal.contribute(new BigDecimal("-500")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("flags a goal that has fallen behind its glide path")
        void detectsBehind() {
            Goal goal = goal("1000000", "50000", 12);
            goal.setCreatedAt(LocalDateTime.now().minusMonths(24));
            goal.refreshStatus();

            assertThat(goal.getStatus()).isEqualTo(Enums.GoalStatus.BEHIND);
        }

        @Test
        @DisplayName("orders by priority first, then by urgency")
        void naturalOrdering() {
            Goal low = goal("100000", "0", 6);
            low.setPriority(Enums.Priority.LOW);
            Goal critical = goal("100000", "0", 120);
            critical.setPriority(Enums.Priority.CRITICAL);
            Goal medium = goal("100000", "0", 3);
            medium.setPriority(Enums.Priority.MEDIUM);

            List<Goal> goals = new ArrayList<>(List.of(low, critical, medium));
            Collections.sort(goals);

            assertThat(goals).extracting(Goal::getPriority)
                    .containsExactly(Enums.Priority.CRITICAL, Enums.Priority.MEDIUM, Enums.Priority.LOW);
        }
    }

    @Nested
    @DisplayName("Portfolio and holdings")
    class Portfolios {

        private Holding holding(String invested, String units, String currentPrice, int daysAgo) {
            BigDecimal unitCount = new BigDecimal(units);
            BigDecimal price = new BigDecimal(currentPrice);
            return Holding.builder().id(1L)
                    .product(product("Large Cap", Enums.RiskLevel.MODERATE, "13.50", 4))
                    .units(unitCount)
                    .buyPrice(new BigDecimal(invested).divide(unitCount, 4, RoundingMode.HALF_UP))
                    .currentPrice(price)
                    .investedAmount(new BigDecimal(invested))
                    .currentValue(price.multiply(unitCount).setScale(2, RoundingMode.HALF_UP))
                    .purchaseDate(LocalDate.now().minusDays(daysAgo)).build();
        }

        @Test
        @DisplayName("computes gain and gain percentage")
        void computesGain() {
            Holding holding = holding("100000", "1000", "120", 400);
            assertThat(holding.getCurrentValue()).isEqualByComparingTo("120000.00");
            assertThat(holding.gain()).isEqualByComparingTo("20000.00");
            assertThat(holding.gainPct()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("classifies holdings over a year old as long term")
        void classifiesLongTerm() {
            assertThat(holding("100000", "1000", "120", 400).isLongTerm()).isTrue();
            assertThat(holding("100000", "1000", "120", 200).isLongTerm()).isFalse();
        }

        @Test
        @DisplayName("reduces the cost basis proportionally on a partial redemption")
        void partialRedemption() {
            Holding holding = holding("100000", "1000", "120", 400);
            holding.redeem(new BigDecimal("400"));

            assertThat(holding.getUnits()).isEqualByComparingTo("600");
            assertThat(holding.getInvestedAmount()).isEqualByComparingTo("60000.00");
            assertThat(holding.isRedeemed()).isFalse();
        }

        @Test
        @DisplayName("zero units is the only state that means fully redeemed")
        void fullRedemption() {
            Holding holding = holding("100000", "1000", "120", 400);
            holding.redeem(new BigDecimal("1000"));

            assertThat(holding.getUnits()).isEqualByComparingTo("0");
            assertThat(holding.isRedeemed()).isTrue();
        }

        @Test
        @DisplayName("refuses to redeem more units than are held")
        void refusesOverRedemption() {
            Holding holding = holding("100000", "1000", "120", 400);
            assertThatThrownBy(() -> holding.redeem(new BigDecimal("1500")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rolls holdings up into the portfolio totals")
        void recalculates() {
            Portfolio portfolio = Portfolio.builder().id(1L).userId(1L).build();
            portfolio.addHolding(holding("100000", "1000", "120", 400));
            portfolio.addHolding(holding("50000", "500", "110", 200));
            portfolio.recalculate();

            assertThat(portfolio.getTotalInvested()).isEqualByComparingTo("150000.00");
            assertThat(portfolio.getCurrentValue()).isEqualByComparingTo("175000.00");
            assertThat(portfolio.gain()).isEqualByComparingTo("25000.00");
            assertThat(portfolio.gainPct()).isEqualByComparingTo("16.67");
        }

        @Test
        @DisplayName("reports zero rather than dividing by zero when empty")
        void handlesEmpty() {
            Portfolio portfolio = Portfolio.builder().id(1L).userId(1L).build();
            portfolio.recalculate();
            assertThat(portfolio.gainPct()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Products, plans and enums")
    class Rest {

        @Test
        @DisplayName("products order naturally by expected return, then rating, then name")
        void productOrdering() {
            List<Product> products = new ArrayList<>(List.of(
                    product("Low return", Enums.RiskLevel.LOW, "7.00", 5),
                    product("High return", Enums.RiskLevel.HIGH, "18.00", 3),
                    product("Mid return", Enums.RiskLevel.MODERATE, "12.00", 4)));

            Collections.sort(products);

            assertThat(products).extracting(Product::getName)
                    .containsExactly("High return", "Mid return", "Low return");
        }

        @Test
        @DisplayName("splits the pipe-delimited feature string and computes a monthly equivalent")
        void planFeatures() {
            Plan plan = Plan.builder().code("PREMIUM_Y").name("Premium Annual")
                    .tier(Enums.Tier.PREMIUM).price(new BigDecimal("4999"))
                    .durationMonths(12).features("One|Two|Three").maxGoals(999).active(true).build();

            assertThat(plan.featureList()).containsExactly("One", "Two", "Three");
            assertThat(plan.monthlyEquivalent()).isEqualByComparingTo("416.58");
            assertThat(plan.isFree()).isFalse();
        }

        @Test
        @DisplayName("activates a subscription for the plan's full duration")
        void subscriptionActivation() {
            Plan plan = Plan.builder().code("PREMIUM_Y").name("Premium Annual")
                    .tier(Enums.Tier.PREMIUM).price(new BigDecimal("4999"))
                    .durationMonths(12).maxGoals(999).active(true).build();

            Subscription subscription = Subscription.builder().id(1L).userId(1L).plan(plan)
                    .startDate(LocalDate.now()).endDate(LocalDate.now())
                    .status(Enums.SubscriptionStatus.PENDING).build();

            subscription.activate();

            assertThat(subscription.getStatus()).isEqualTo(Enums.SubscriptionStatus.ACTIVE);
            assertThat(subscription.getEndDate()).isEqualTo(LocalDate.now().plusMonths(12));
            assertThat(subscription.isCurrentlyActive()).isTrue();
        }

        @Test
        @DisplayName("matches product risk against an investor's ceiling")
        void riskSuitability() {
            assertThat(Enums.RiskLevel.VERY_LOW.suitableFor(Enums.RiskProfile.CONSERVATIVE)).isTrue();
            assertThat(Enums.RiskLevel.VERY_HIGH.suitableFor(Enums.RiskProfile.CONSERVATIVE)).isFalse();
            assertThat(Enums.RiskLevel.VERY_HIGH.suitableFor(Enums.RiskProfile.AGGRESSIVE)).isTrue();
        }

        @Test
        @DisplayName("free tier caps goals at three; paid tiers do not")
        void tierGoalCaps() {
            assertThat(Enums.Tier.FREE.maxGoals()).isEqualTo(3);
            assertThat(Enums.Tier.FREE.isPremium()).isFalse();
            assertThat(Enums.Tier.PREMIUM.isPremium()).isTrue();
            assertThat(Enums.Tier.PREMIUM.maxGoals()).isGreaterThan(100);
        }
    }
}
