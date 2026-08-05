package com.investwise.investment.service;

import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.RiskAssessment;
import com.investwise.investment.repository.jpa.RiskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Risk profiling.
 * <p>
 * Seven scored factors summing to 100. Capacity (what you can afford to lose) and
 * tolerance (what you are willing to lose) are scored separately, because an
 * investor with a large surplus and a nervous disposition should not be handed an
 * aggressive portfolio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskRepository assessments;
    private final ActivityService activity;

    @Transactional
    public Responses.RiskView assess(Long userId, Requests.RiskQuestionnaire request) {
        Map<String, Integer> breakdown = score(request);
        int total = breakdown.values().stream().mapToInt(Integer::intValue).sum();
        Enums.RiskProfile profile = Enums.RiskProfile.fromScore(total);

        RiskAssessment saved = assessments.save(RiskAssessment.builder()
                .userId(userId)
                .age(request.age())
                .annualIncome(request.annualIncome())
                .monthlySurplus(request.monthlySurplus())
                .dependents(Optional.ofNullable(request.dependents()).orElse(0))
                .horizonYears(request.horizonYears())
                .knowledgeLevel(request.knowledgeLevel())
                .lossTolerance(request.lossTolerance())
                .hasEmergencyFund(request.hasEmergencyFund())
                .hasHealthInsurance(request.hasHealthInsurance())
                .score(total)
                .profile(profile)
                .equityPct(profile.equityPct())
                .debtPct(profile.debtPct())
                .goldPct(profile.goldPct())
                .build());

        activity.record(userId, null, "RISK_ASSESSED", "Assessed as %s (score %d)".formatted(profile, total));
        log.info("User {} assessed as {} with score {}", userId, profile, total);

        return Responses.RiskView.from(saved, summarise(profile), breakdown, guidance(request, profile));
    }

    /**
     * The scoring model, with each factor's maximum in the comment beside it.
     * An ordered map means the UI can show exactly which answer moved the score,
     * which is far more useful than an opaque number.
     */
    private Map<String, Integer> score(Requests.RiskQuestionnaire request) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();

        // Capacity: a longer working life genuinely is a capacity for risk (max 18)
        int age = request.age();
        breakdown.put("Age", age < 30 ? 18 : age < 40 ? 15 : age < 50 ? 11 : age < 60 ? 6 : 3);

        // Capacity: the single strongest determinant of how much equity is sensible (max 22)
        int horizon = request.horizonYears();
        breakdown.put("Investment horizon",
                horizon >= 15 ? 22 : horizon >= 10 ? 18 : horizon >= 7 ? 14 : horizon >= 4 ? 9 : 4);

        // Capacity: how much of a shock the budget can absorb (max 16)
        double surplusRatio = request.annualIncome().signum() == 0 ? 0
                : request.monthlySurplus().multiply(BigDecimal.valueOf(12)).doubleValue()
                    / request.annualIncome().doubleValue();
        breakdown.put("Savings capacity",
                surplusRatio >= 0.40 ? 16 : surplusRatio >= 0.25 ? 13
                        : surplusRatio >= 0.15 ? 9 : surplusRatio >= 0.05 ? 5 : 2);

        // Capacity: dependents reduce it regardless of appetite (max 10)
        int dependents = Optional.ofNullable(request.dependents()).orElse(0);
        breakdown.put("Dependents",
                dependents == 0 ? 10 : dependents == 1 ? 8 : dependents == 2 ? 6 : dependents == 3 ? 4 : 2);

        // Tolerance: behaviour under drawdown (max 20)
        breakdown.put("Reaction to a loss", switch (request.lossTolerance()) {
            case BUY_MORE -> 20;
            case HOLD -> 15;
            case SELL_SOME -> 8;
            case SELL_EVERYTHING -> 2;
        });

        // Tolerance: knowledge affects the ability to hold through volatility (max 8)
        breakdown.put("Market knowledge", switch (request.knowledgeLevel()) {
            case EXPERT -> 8;
            case ADVANCED -> 7;
            case INTERMEDIATE -> 5;
            case BEGINNER -> 2;
        });

        // Safety net: without it any drawdown may force a redemption (max 6)
        breakdown.put("Financial safety net",
                (request.hasEmergencyFund() ? 4 : 0) + (request.hasHealthInsurance() ? 2 : 0));

        return breakdown;
    }

    private String summarise(Enums.RiskProfile profile) {
        return switch (profile) {
            case CONSERVATIVE -> "Capital preservation comes first. Your portfolio leans on debt and "
                    + "government-backed instruments, accepting lower returns for steadier value.";
            case MODERATE -> "Stability with a measured growth component. A modest equity allocation "
                    + "lifts long-run returns without exposing you to sharp drawdowns.";
            case BALANCED -> "Growth oriented with a debt cushion. This suits investors who want real "
                    + "returns above inflation but would find a very deep fall difficult to sit through.";
            case AGGRESSIVE -> "Maximum long-term growth. Predominantly equity, with the expectation of "
                    + "significant interim drawdowns you have the horizon and temperament to absorb.";
        };
    }

    /** Advice that names a specific action, not a platitude. */
    private List<String> guidance(Requests.RiskQuestionnaire request, Enums.RiskProfile profile) {
        List<String> guidance = new ArrayList<>();

        if (!request.hasEmergencyFund()) {
            guidance.add("Build an emergency fund of six months' expenses in a liquid fund before "
                    + "increasing your equity allocation. Without it, the first unexpected bill forces a "
                    + "redemption at the worst possible moment.");
        }
        if (!request.hasHealthInsurance()) {
            guidance.add("Health cover is the cheapest protection available for an investment plan. "
                    + "A single hospitalisation can undo years of compounding.");
        }
        if (request.horizonYears() <= 3 && profile.equityPct() > 40) {
            guidance.add("Your horizon is short relative to your appetite. Money needed within three "
                    + "years belongs in debt instruments regardless of your risk profile.");
        }
        if (request.age() > 50 && profile.equityPct() > 60) {
            guidance.add("As retirement approaches, review this allocation annually and shift gradually "
                    + "towards debt to protect the corpus you have accumulated.");
        }
        if (Optional.ofNullable(request.dependents()).orElse(0) >= 2) {
            guidance.add("With dependents, ensure term life cover of at least ten times your annual "
                    + "income sits alongside your investment plan.");
        }
        guidance.add("Review this assessment annually, or sooner if your income, dependents or time "
                + "horizon change materially.");
        return guidance;
    }

    // ---------------- reads ----------------

    @Transactional(readOnly = true)
    public Optional<Responses.RiskView> current(Long userId) {
        return assessments.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(assessment -> Responses.RiskView.from(assessment,
                        summarise(assessment.getProfile()), Map.of(), List.of()));
    }

    @Transactional(readOnly = true)
    public Optional<RiskAssessment> latest(Long userId) {
        return assessments.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.RiskView> history(Long userId, int page, int size) {
        return PageResponse.of(
                assessments.findByUserIdOrderByCreatedAtDesc(userId, ProductService.pageable(page, size)),
                assessment -> Responses.RiskView.from(assessment,
                        summarise(assessment.getProfile()), Map.of(), List.of()));
    }

    /**
     * The return assumption used for goal projections: the investor's own blended
     * rate when they have been assessed, otherwise a neutral default.
     */
    @Transactional(readOnly = true)
    public BigDecimal returnAssumption(Long userId) {
        return assessments.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(a -> BigDecimal.valueOf(
                        (a.getEquityPct() * 13.0 + a.getDebtPct() * 7.0 + a.getGoldPct() * 9.0) / 100.0)
                        .setScale(2, java.math.RoundingMode.HALF_UP))
                .orElse(new BigDecimal("11.00"));
    }
}
