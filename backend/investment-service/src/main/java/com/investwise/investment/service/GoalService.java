package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.common.PageResponse;
import com.investwise.investment.config.Events;
import com.investwise.investment.config.RabbitConfig;
import com.investwise.investment.dto.Requests;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.model.Goal;
import com.investwise.investment.repository.jpa.GoalRepository;
import com.investwise.investment.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Financial goal management.
 * <p>
 * The free-tier cap is enforced here rather than in the controller: it is a
 * business rule, so it must hold no matter which entry point creates the goal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goals;
    private final RiskService riskService;
    private final SubscriptionService subscriptions;
    private final ActivityService activity;
    private final RabbitTemplate rabbit;

    @Transactional
    public Responses.GoalView create(AuthUser user, Requests.Goal request) {
        Enums.Tier tier = subscriptions.tierOf(user.id());
        if (goals.countByUserId(user.id()) >= tier.maxGoals()) {
            throw ApiException.forbidden(
                    "Your %s plan allows %d goals. Upgrade to Premium for unlimited goal planning."
                            .formatted(tier, tier.maxGoals()));
        }

        Goal goal = goals.save(Goal.builder()
                .userId(user.id())
                .title(request.title().trim())
                .description(request.description())
                .goalType(request.goalType())
                .targetAmount(request.targetAmount())
                .currentAmount(Optional.ofNullable(request.currentAmount()).orElse(BigDecimal.ZERO))
                .monthlyContribution(Optional.ofNullable(request.monthlyContribution()).orElse(BigDecimal.ZERO))
                .targetDate(request.targetDate())
                .priority(Optional.ofNullable(request.priority()).orElse(Enums.Priority.MEDIUM))
                .status(Enums.GoalStatus.ACTIVE)
                .build());

        activity.record(user.id(), user.email(), "GOAL_CREATED",
                "Created \"%s\" targeting %s by %s".formatted(goal.getTitle(), goal.getTargetAmount(), goal.getTargetDate()));

        log.info("Goal {} created for user {}", goal.getId(), user.id());
        return view(goal, user.id());
    }

    @Transactional
    public Responses.GoalView update(Long goalId, AuthUser user, Requests.Goal request) {
        Goal goal = owned(goalId, user.id());

        goal.setTitle(request.title().trim());
        goal.setDescription(request.description());
        goal.setGoalType(request.goalType());
        goal.setTargetAmount(request.targetAmount());
        Optional.ofNullable(request.currentAmount()).ifPresent(goal::setCurrentAmount);
        Optional.ofNullable(request.monthlyContribution()).ifPresent(goal::setMonthlyContribution);
        goal.setTargetDate(request.targetDate());
        Optional.ofNullable(request.priority()).ifPresent(goal::setPriority);
        goal.refreshStatus();

        activity.record(user.id(), user.email(), "GOAL_UPDATED", "Updated \"%s\"".formatted(goal.getTitle()));
        return view(goals.save(goal), user.id());
    }

    @Transactional(readOnly = true)
    public Responses.GoalView get(Long goalId, Long userId) {
        return view(owned(goalId, userId), userId);
    }

    /** Ordered by the entity's natural ordering: highest priority, nearest date first. */
    @Transactional(readOnly = true)
    public List<Responses.GoalView> listAll(Long userId) {
        BigDecimal rate = riskService.returnAssumption(userId);
        return goals.findByUserId(userId).stream()
                .sorted(Comparator.naturalOrder())
                .map(goal -> Responses.GoalView.from(goal, rate))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<Responses.GoalView> list(Long userId, Enums.GoalStatus status,
                                                 Enums.GoalType type, int page, int size) {
        BigDecimal rate = riskService.returnAssumption(userId);
        return PageResponse.of(
                goals.findFiltered(userId, status, type, ProductService.pageable(page, size, "targetDate", "asc")),
                goal -> Responses.GoalView.from(goal, rate));
    }

    @Transactional
    public Responses.GoalView contribute(Long goalId, AuthUser user, Requests.Contribution request) {
        Goal goal = owned(goalId, user.id());
        if (goal.getStatus() == Enums.GoalStatus.CANCELLED) {
            throw ApiException.badRequest("This goal has been cancelled and cannot receive contributions");
        }

        Enums.GoalStatus before = goal.getStatus();
        goal.contribute(request.amount());
        Goal saved = goals.save(goal);

        activity.record(user.id(), user.email(), "GOAL_CONTRIBUTION",
                "Contributed %s to \"%s\"".formatted(request.amount(), saved.getTitle()));

        // Celebrate exactly once, on the transition rather than on every later save
        if (saved.getStatus() == Enums.GoalStatus.ACHIEVED && before != Enums.GoalStatus.ACHIEVED) {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_NOTIFY,
                    new Events.Notify(user.id(), "Goal achieved",
                            "You reached your target for \"%s\".".formatted(saved.getTitle()),
                            "SUCCESS", "/goals"));
            log.info("User {} achieved goal {}", user.id(), saved.getId());
        }
        return view(saved, user.id());
    }

    @Transactional
    public void delete(Long goalId, AuthUser user) {
        Goal goal = owned(goalId, user.id());
        goals.delete(goal);
        activity.record(user.id(), user.email(), "GOAL_DELETED", "Deleted \"%s\"".formatted(goal.getTitle()));
    }

    /** Nightly re-evaluation against each goal's glide path. */
    @Transactional
    public int refreshAllStatuses() {
        List<Goal> trackable = goals.findTrackable();
        trackable.forEach(Goal::refreshStatus);
        goals.saveAll(trackable);

        long behind = trackable.stream().filter(g -> g.getStatus() == Enums.GoalStatus.BEHIND).count();
        log.info("Refreshed {} goal statuses; {} behind schedule", trackable.size(), behind);
        return trackable.size();
    }

    // ---------------- helpers ----------------

    Goal ownedEntity(Long goalId, Long userId) {
        return owned(goalId, userId);
    }

    private Goal owned(Long goalId, Long userId) {
        return goals.findByIdAndUserId(goalId, userId).orElseThrow(() -> ApiException.notFound("Goal"));
    }

    private Responses.GoalView view(Goal goal, Long userId) {
        return Responses.GoalView.from(goal, riskService.returnAssumption(userId));
    }
}
