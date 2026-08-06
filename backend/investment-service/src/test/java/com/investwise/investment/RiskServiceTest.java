package com.investwise.investment;

import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.RiskAssessment;
import com.investwise.investment.repository.jpa.RiskRepository;
import com.investwise.investment.service.ActivityService;
import com.investwise.investment.service.RiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskService")
class RiskServiceTest {

    @Mock private RiskRepository assessments;
    @Mock private ActivityService activity;

    private RiskService riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskService(assessments, activity);
        when(assessments.save(any(RiskAssessment.class))).thenAnswer(inv -> {
            RiskAssessment saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });
    }

    private Requests.RiskQuestionnaire questionnaire(int age, int horizon, String surplus,
                                                     int dependents, Enums.Knowledge knowledge,
                                                     Enums.LossTolerance tolerance,
                                                     boolean fund, boolean insurance) {
        return new Requests.RiskQuestionnaire(age, new BigDecimal("1200000"), new BigDecimal(surplus),
                dependents, horizon, knowledge, tolerance, fund, insurance);
    }

    @Test
    @DisplayName("scores a young, protected, long-horizon investor as aggressive")
    void scoresAggressive() {
        Responses.RiskView view = riskService.assess(1L, questionnaire(26, 25, "30000", 0,
                Enums.Knowledge.ADVANCED, Enums.LossTolerance.BUY_MORE, true, true));

        assertThat(view.profile()).isEqualTo(Enums.RiskProfile.AGGRESSIVE);
        assertThat(view.equityPct()).isGreaterThanOrEqualTo(70);
        assertThat(view.score()).isGreaterThan(72);
    }

    @Test
    @DisplayName("scores an older, short-horizon, loss-averse investor as conservative")
    void scoresConservative() {
        Responses.RiskView view = riskService.assess(1L, questionnaire(58, 2, "5000", 3,
                Enums.Knowledge.BEGINNER, Enums.LossTolerance.SELL_EVERYTHING, false, false));

        assertThat(view.profile()).isEqualTo(Enums.RiskProfile.CONSERVATIVE);
        assertThat(view.debtPct()).isGreaterThan(view.equityPct());
    }

    @Test
    @DisplayName("every allocation totals exactly 100 percent")
    void allocationsTotalHundred() {
        for (Enums.LossTolerance tolerance : Enums.LossTolerance.values()) {
            Responses.RiskView view = riskService.assess(1L, questionnaire(35, 12, "25000", 1,
                    Enums.Knowledge.INTERMEDIATE, tolerance, true, true));

            assertThat(view.equityPct() + view.debtPct() + view.goldPct())
                    .as("allocation for %s", tolerance).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("the factor breakdown sums to the total score")
    void breakdownSumsToTotal() {
        Responses.RiskView view = riskService.assess(1L, questionnaire(35, 12, "25000", 1,
                Enums.Knowledge.INTERMEDIATE, Enums.LossTolerance.HOLD, true, true));

        assertThat(view.breakdown().values().stream().mapToInt(Integer::intValue).sum())
                .isEqualTo(view.score());
        assertThat(view.breakdown()).containsKeys("Age", "Investment horizon", "Savings capacity",
                "Dependents", "Reaction to a loss", "Market knowledge", "Financial safety net");
    }

    @Test
    @DisplayName("warns an investor with no emergency fund")
    void guidanceFlagsMissingFund() {
        Responses.RiskView view = riskService.assess(1L, questionnaire(35, 12, "25000", 1,
                Enums.Knowledge.INTERMEDIATE, Enums.LossTolerance.HOLD, false, true));

        assertThat(view.guidance()).anyMatch(line -> line.toLowerCase().contains("emergency fund"));
    }

    @Test
    @DisplayName("always carries a plain-language summary")
    void alwaysExplains() {
        Responses.RiskView view = riskService.assess(1L, questionnaire(35, 12, "25000", 1,
                Enums.Knowledge.INTERMEDIATE, Enums.LossTolerance.HOLD, true, true));

        assertThat(view.summary()).isNotBlank().hasSizeGreaterThan(50);
    }

    @Test
    @DisplayName("maps boundary scores onto the correct profile band")
    void mapsBoundaries() {
        assertThat(Enums.RiskProfile.fromScore(0)).isEqualTo(Enums.RiskProfile.CONSERVATIVE);
        assertThat(Enums.RiskProfile.fromScore(30)).isEqualTo(Enums.RiskProfile.CONSERVATIVE);
        assertThat(Enums.RiskProfile.fromScore(31)).isEqualTo(Enums.RiskProfile.MODERATE);
        assertThat(Enums.RiskProfile.fromScore(56)).isEqualTo(Enums.RiskProfile.BALANCED);
        assertThat(Enums.RiskProfile.fromScore(73)).isEqualTo(Enums.RiskProfile.AGGRESSIVE);
        assertThat(Enums.RiskProfile.fromScore(200)).isEqualTo(Enums.RiskProfile.AGGRESSIVE);
    }

    @Test
    @DisplayName("every risk profile's target allocation totals 100")
    void everyProfileTotalsHundred() {
        for (Enums.RiskProfile profile : Enums.RiskProfile.values()) {
            assertThat(profile.equityPct() + profile.debtPct() + profile.goldPct())
                    .as("allocation for %s", profile).isEqualTo(100);
        }
    }
}
