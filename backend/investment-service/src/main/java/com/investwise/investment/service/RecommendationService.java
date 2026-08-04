package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Goal;
import com.investwise.investment.model.Product;
import com.investwise.investment.model.Recommendation;
import com.investwise.investment.model.RecommendationHistory;
import com.investwise.investment.model.RiskAssessment;
import com.investwise.investment.repository.jpa.GoalRepository;
import com.investwise.investment.repository.mongo.HistoryRepository;
import com.investwise.investment.repository.jpa.ProductRepository;
import com.investwise.investment.repository.jpa.RecommendationRepository;
import com.investwise.investment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orchestrates a recommendation run: gather inputs, invoke the engine, persist.
 * <p>
 * Storage is split deliberately. The relational rows support "show me my current
 * advice" with a join onto the live catalogue; the Mongo document is a frozen
 * record of what was advised and why, which must survive later catalogue edits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationEngine engine;
    private final ProductRepository products;
    private final GoalRepository goals;
    private final RecommendationRepository recommendations;
    private final HistoryRepository history;
    private final RiskService riskService;
    private final ActivityService activity;

    @Transactional
    public Responses.RecommendationView generate(AuthUser user, Requests.Recommend request) {
        RiskAssessment assessment = riskService.latest(user.id()).orElseThrow(() ->
                ApiException.badRequest("Complete the risk assessment first so we can tailor recommendations to you"));

        Goal goal = request.goalId() == null ? null
                : goals.findByIdAndUserId(request.goalId(), user.id())
                    .orElseThrow(() -> ApiException.notFound("Goal"));

        int horizon = request.horizonYears() != null ? request.horizonYears()
                : goal != null ? (int) Math.max(1, goal.monthsRemaining() / 12)
                : assessment.getHorizonYears();

        var input = new RecommendationEngine.Input(assessment.getProfile(), horizon,
                request.investableAmount(), goal == null ? null : goal.getGoalType(), user.isPremium());

        List<Product> candidates = products.findCandidates(request.investableAmount(), user.isPremium());
        if (candidates.isEmpty()) {
            throw ApiException.badRequest(
                    "No products accept an investment of %s. Try a larger amount.".formatted(request.investableAmount()));
        }

        List<RecommendationEngine.Allocated> allocated =
                engine.allocate(engine.select(engine.score(candidates, input)), input, request.investableAmount());

        if (allocated.isEmpty()) {
            throw ApiException.badRequest("We could not build a suitable basket. Try adjusting the amount or horizon.");
        }

        persist(user.id(), goal, allocated);
        Responses.RecommendationView view = toView(goal, assessment, input, allocated);
        saveHistory(user.id(), goal, assessment, input, allocated, view);

        activity.record(user.id(), user.email(), "RECOMMENDATION_GENERATED",
                "Generated %d recommendations".formatted(allocated.size()));

        log.info("Generated {} recommendations for user {} ({})", allocated.size(), user.id(), assessment.getProfile());
        return view;
    }

    private void persist(Long userId, Goal goal, List<RecommendationEngine.Allocated> allocated) {
        // Superseded advice is cleared so a user never sees two conflicting baskets
        recommendations.deleteForUser(userId, goal == null ? null : goal.getId());

        recommendations.saveAll(allocated.stream()
                .map(item -> Recommendation.builder()
                        .userId(userId).goal(goal).product(item.product())
                        .allocationPct(item.allocationPct()).amount(item.amount())
                        .matchScore(BigDecimal.valueOf(item.score()).setScale(2, RoundingMode.HALF_UP))
                        .rationale(truncate(item.rationale()))
                        .build())
                .toList());
    }

    private Responses.RecommendationView toView(Goal goal, RiskAssessment assessment,
                                                RecommendationEngine.Input input,
                                                List<RecommendationEngine.Allocated> allocated) {
        BigDecimal expectedReturn = engine.expectedReturn(allocated);

        List<Responses.RecommendationItem> items = allocated.stream()
                .map(item -> new Responses.RecommendationItem(
                        item.product().getId(), item.product().getCode(), item.product().getName(),
                        item.product().getCategory().label(),
                        item.product().getCategory().assetClass().name(),
                        item.product().getRiskLevel().name(), item.product().getExpectedReturn(),
                        item.allocationPct(), item.amount(),
                        BigDecimal.valueOf(item.score()).setScale(2, RoundingMode.HALF_UP),
                        item.rationale(), item.product().getLockInMonths(),
                        item.product().getMinInvestment()))
                .toList();

        Map<String, BigDecimal> byAssetClass = allocated.stream()
                .collect(Collectors.groupingBy(
                        a -> a.product().getCategory().assetClass().name(), LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                RecommendationEngine.Allocated::allocationPct, BigDecimal::add)));

        return new Responses.RecommendationView(
                goal == null ? null : goal.getId(), goal == null ? null : goal.getTitle(),
                assessment.getProfile(), assessment.getScore(), input.investable(), input.horizonYears(),
                expectedReturn, Money.futureValue(input.investable(), expectedReturn, input.horizonYears()),
                items, byAssetClass, LocalDateTime.now());
    }

    private void saveHistory(Long userId, Goal goal, RiskAssessment assessment,
                             RecommendationEngine.Input input,
                             List<RecommendationEngine.Allocated> allocated,
                             Responses.RecommendationView view) {
        history.save(RecommendationHistory.builder()
                .userId(userId)
                .goalId(goal == null ? null : goal.getId())
                .goalTitle(goal == null ? null : goal.getTitle())
                .riskProfile(assessment.getProfile().name())
                .riskScore(assessment.getScore())
                .investableAmount(input.investable())
                .horizonYears(input.horizonYears())
                .expectedReturn(view.expectedReturn())
                .items(allocated.stream()
                        .map(item -> new RecommendationHistory.Item(
                                item.product().getId(), item.product().getCode(), item.product().getName(),
                                item.product().getCategory().name(), item.product().getRiskLevel().name(),
                                item.product().getExpectedReturn(), item.allocationPct(), item.amount(),
                                BigDecimal.valueOf(item.score()).setScale(2, RoundingMode.HALF_UP),
                                item.rationale()))
                        .toList())
                .build());
    }

    @Transactional(readOnly = true)
    public Responses.RecommendationView latest(Long userId, Long goalId) {
        List<Recommendation> stored = recommendations.findLatest(userId, goalId);
        if (stored.isEmpty()) {
            throw ApiException.notFound("Recommendations");
        }

        BigDecimal investable = stored.stream().map(Recommendation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Responses.RecommendationItem> items = stored.stream()
                .map(rec -> new Responses.RecommendationItem(
                        rec.getProduct().getId(), rec.getProduct().getCode(), rec.getProduct().getName(),
                        rec.getProduct().getCategory().label(),
                        rec.getProduct().getCategory().assetClass().name(),
                        rec.getProduct().getRiskLevel().name(), rec.getProduct().getExpectedReturn(),
                        rec.getAllocationPct(), rec.getAmount(), rec.getMatchScore(), rec.getRationale(),
                        rec.getProduct().getLockInMonths(), rec.getProduct().getMinInvestment()))
                .toList();

        Map<String, BigDecimal> byAssetClass = stored.stream()
                .collect(Collectors.groupingBy(
                        rec -> rec.getProduct().getCategory().assetClass().name(), LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Recommendation::getAllocationPct, BigDecimal::add)));

        Recommendation first = stored.get(0);
        return new Responses.RecommendationView(
                first.getGoal() == null ? null : first.getGoal().getId(),
                first.getGoal() == null ? null : first.getGoal().getTitle(),
                null, null, investable, null, null, null, items, byAssetClass, first.getCreatedAt());
    }

    public PageResponse<RecommendationHistory> history(Long userId, int page, int size) {
        return PageResponse.of(
                history.findByUserIdOrderByCreatedAtDesc(userId, ProductService.pageable(page, size)),
                item -> item);
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() <= 500 ? text : text.substring(0, 497) + "...";
    }
}
