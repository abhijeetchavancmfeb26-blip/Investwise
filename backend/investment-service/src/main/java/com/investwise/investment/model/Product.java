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
 * A product in the catalogue.
 * <p>
 * Natural ordering is highest expected return first, so a plain sort produces a
 * sensible list; the recommendation engine overrides it with its own match score.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Comparable<Product> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Enums.Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private Enums.RiskLevel riskLevel;

    @Column(name = "expected_return", nullable = false, precision = 5, scale = 2)
    private BigDecimal expectedReturn;

    @Column(name = "min_investment", nullable = false, precision = 15, scale = 2)
    private BigDecimal minInvestment;

    @Builder.Default
    @Column(name = "lock_in_months", nullable = false)
    private Integer lockInMonths = 0;

    @Column(name = "fund_house", length = 120)
    private String fundHouse;

    @Column(name = "expense_ratio", precision = 4, scale = 2)
    private BigDecimal expenseRatio;

    @Builder.Default
    @Column(nullable = false)
    private Integer rating = 3;

    @Builder.Default
    @Column(name = "premium_only", nullable = false)
    private boolean premiumOnly = false;

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

    public boolean hasLockIn() {
        return lockInMonths != null && lockInMonths > 0;
    }

    /** Highest expected return first, ties broken by rating then name. */
    @Override
    public int compareTo(Product other) {
        int byReturn = other.expectedReturn.compareTo(this.expectedReturn);
        if (byReturn != 0) return byReturn;
        int byRating = Integer.compare(other.rating, this.rating);
        return byRating != 0 ? byRating : name.compareToIgnoreCase(other.name);
    }
}
