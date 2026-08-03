package com.investwise.investment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One portfolio per user, holding the aggregate of every position. */
@Entity
@Table(name = "portfolios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "holdings")
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    @Column(name = "total_invested", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInvested = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "current_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValue = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** ArrayList: holdings are read positionally for display far more often than searched. */
    @Builder.Default
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Holding> holdings = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void addHolding(Holding holding) {
        holdings.add(holding);
        holding.setPortfolio(this);
    }

    public BigDecimal gain() {
        return currentValue.subtract(totalInvested);
    }

    public BigDecimal gainPct() {
        if (totalInvested.signum() == 0) return BigDecimal.ZERO;
        return gain().multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);
    }

    /** The single place the roll-up is derived from the child holdings. */
    public void recalculate() {
        totalInvested = holdings.stream().map(Holding::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        currentValue = holdings.stream().map(Holding::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
