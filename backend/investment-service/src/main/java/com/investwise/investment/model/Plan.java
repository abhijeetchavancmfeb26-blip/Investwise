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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** A purchasable plan. Features are pipe-delimited to keep the schema flat. */
@Entity
@Table(name = "plans")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.Tier tier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Builder.Default
    @Column(name = "max_goals", nullable = false)
    private Integer maxGoals = 3;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public List<String> featureList() {
        return (features == null || features.isBlank())
                ? List.of()
                : Arrays.stream(features.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Effective monthly cost, so the pricing page can compare annual plans fairly. */
    public BigDecimal monthlyEquivalent() {
        return (durationMonths == null || durationMonths == 0)
                ? price
                : price.divide(BigDecimal.valueOf(durationMonths), 2, RoundingMode.HALF_UP);
    }

    public boolean isFree() {
        return price == null || price.signum() == 0;
    }
}
