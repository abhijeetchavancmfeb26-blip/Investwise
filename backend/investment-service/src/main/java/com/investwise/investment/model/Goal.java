package com.investwise.investment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A dated, quantified financial objective.
 * <p>
 * The progress arithmetic lives on the entity because it depends on nothing but
 * the goal's own fields. Putting it in a service would invite two callers to
 * compute it differently.
 */
@Entity
@Table(name = "goals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal implements Comparable<Goal> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 40)
    private Enums.GoalType goalType;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Builder.Default
    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "monthly_contribution", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyContribution = BigDecimal.ZERO;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Priority priority = Enums.Priority.MEDIUM;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.GoalStatus status = Enums.GoalStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ------------------------------------------------------------------

    public BigDecimal progressPct() {
        if (targetAmount.signum() == 0) return BigDecimal.ZERO;
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    public BigDecimal shortfall() {
        return targetAmount.subtract(currentAmount).max(BigDecimal.ZERO);
    }

    public long monthsRemaining() {
        return Math.max(ChronoUnit.MONTHS.between(LocalDate.now(), targetDate), 0);
    }

    public void contribute(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Contribution must be a positive amount");
        }
        currentAmount = currentAmount.add(amount);
        refreshStatus();
    }

    /**
     * Re-evaluates status against the linear glide path expected by today, so
     * "behind schedule" is a fact rather than a feeling.
     */
    public void refreshStatus() {
        if (currentAmount.compareTo(targetAmount) >= 0) {
            status = Enums.GoalStatus.ACHIEVED;
            return;
        }
        if (status == Enums.GoalStatus.CANCELLED) return;

        LocalDate start = createdAt == null ? LocalDate.now() : createdAt.toLocalDate();
        long totalMonths = ChronoUnit.MONTHS.between(start, targetDate);
        if (totalMonths <= 0) {
            status = Enums.GoalStatus.ACTIVE;
            return;
        }
        long elapsed = totalMonths - monthsRemaining();
        BigDecimal expected = targetAmount.multiply(BigDecimal.valueOf(elapsed))
                .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);

        status = currentAmount.compareTo(expected) >= 0
                ? Enums.GoalStatus.ON_TRACK : Enums.GoalStatus.BEHIND;
    }

    /** Most urgent and highest priority first. */
    @Override
    public int compareTo(Goal other) {
        int byPriority = other.priority.ordinal() - this.priority.ordinal();
        return byPriority != 0 ? byPriority : targetDate.compareTo(other.targetDate);
    }
}
