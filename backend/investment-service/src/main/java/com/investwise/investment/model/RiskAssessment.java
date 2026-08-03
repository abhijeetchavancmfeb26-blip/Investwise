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
import java.time.LocalDateTime;

/**
 * A completed questionnaire and its outcome.
 * <p>
 * Append-only: a new submission inserts a new row rather than mutating the last
 * one, so the advice given at any past date stays reconstructible.
 */
@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer age;

    @Column(name = "annual_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "monthly_surplus", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlySurplus;

    @Builder.Default
    @Column(nullable = false)
    private Integer dependents = 0;

    @Column(name = "horizon_years", nullable = false)
    private Integer horizonYears;

    @Enumerated(EnumType.STRING)
    @Column(name = "knowledge_level", nullable = false, length = 20)
    private Enums.Knowledge knowledgeLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "loss_tolerance", nullable = false, length = 20)
    private Enums.LossTolerance lossTolerance;

    @Builder.Default
    @Column(name = "has_emergency_fund", nullable = false)
    private boolean hasEmergencyFund = false;

    @Builder.Default
    @Column(name = "has_health_insurance", nullable = false)
    private boolean hasHealthInsurance = false;

    @Column(nullable = false)
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Enums.RiskProfile profile;

    @Column(name = "equity_pct", nullable = false)
    private Integer equityPct;

    @Column(name = "debt_pct", nullable = false)
    private Integer debtPct;

    @Column(name = "gold_pct", nullable = false)
    private Integer goldPct;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
