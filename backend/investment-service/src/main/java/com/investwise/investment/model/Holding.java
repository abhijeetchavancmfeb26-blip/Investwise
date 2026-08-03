package com.investwise.investment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * A single position: units of one product bought at one price.
 * <p>
 * The original tracked a {@code status} enum alongside the unit count. Units of
 * zero already says "fully redeemed", so the enum was removed as redundant.
 */
@Entity
@Table(name = "holdings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "portfolio")
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Optional link tying this position to the goal it funds. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id")
    private Goal goal;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal units;

    @Column(name = "buy_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal buyPrice;

    @Column(name = "current_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "invested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal investedAmount;

    @Column(name = "current_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    // ------------------------------------------------------------------

    public BigDecimal gain() {
        return currentValue.subtract(investedAmount);
    }

    public BigDecimal gainPct() {
        if (investedAmount.signum() == 0) return BigDecimal.ZERO;
        return gain().multiply(BigDecimal.valueOf(100)).divide(investedAmount, 2, RoundingMode.HALF_UP);
    }

    public long holdingDays() {
        return ChronoUnit.DAYS.between(purchaseDate, LocalDate.now());
    }

    /** Equity gains beyond a year are long term in India. */
    public boolean isLongTerm() {
        return holdingDays() >= 365;
    }

    public boolean isRedeemed() {
        return units.signum() == 0;
    }

    public void markToMarket(BigDecimal price) {
        currentPrice = price;
        currentValue = price.multiply(units).setScale(2, RoundingMode.HALF_UP);
    }

    /** Reduces the cost basis proportionally, so the remaining position stays honest. */
    public void redeem(BigDecimal unitsToRedeem) {
        if (unitsToRedeem.compareTo(units) > 0) {
            throw new IllegalArgumentException("Cannot redeem more units than are held");
        }
        BigDecimal costPerUnit = investedAmount.divide(units, 4, RoundingMode.HALF_UP);
        units = units.subtract(unitsToRedeem);
        investedAmount = costPerUnit.multiply(units).setScale(2, RoundingMode.HALF_UP);
        currentValue = currentPrice.multiply(units).setScale(2, RoundingMode.HALF_UP);
    }
}
