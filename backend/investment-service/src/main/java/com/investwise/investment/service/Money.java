package com.investwise.investment.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Time-value-of-money arithmetic.
 * <p>
 * {@code BigDecimal} throughout, because {@code double} accumulates
 * representation error across compounding periods and the output is shown to a
 * customer as their retirement corpus. The one exception is CAGR, which needs a
 * fractional exponent {@code BigDecimal} cannot express; the result returns to
 * {@code BigDecimal} immediately.
 */
public final class Money {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private Money() { }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Future value of a lump sum: {@code FV = PV * (1 + r)^n}. */
    public static BigDecimal futureValue(BigDecimal principal, BigDecimal annualRatePct, int years) {
        if (principal == null || principal.signum() <= 0 || years <= 0) {
            return principal == null ? BigDecimal.ZERO : principal;
        }
        BigDecimal rate = annualRatePct.divide(HUNDRED, 10, RoundingMode.HALF_UP);
        return round(principal.multiply(BigDecimal.ONE.add(rate).pow(years, MC)));
    }

    /** Future value of a monthly SIP: {@code FV = P * [((1 + i)^n - 1) / i]}. */
    public static BigDecimal sipFutureValue(BigDecimal monthly, BigDecimal annualRatePct, int months) {
        if (monthly == null || monthly.signum() <= 0 || months <= 0) return BigDecimal.ZERO;

        BigDecimal monthlyRate = annualRatePct
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 10, RoundingMode.HALF_UP);

        if (monthlyRate.signum() == 0) {
            return round(monthly.multiply(BigDecimal.valueOf(months)));
        }
        BigDecimal compound = BigDecimal.ONE.add(monthlyRate).pow(months, MC);
        return round(monthly.multiply(compound.subtract(BigDecimal.ONE).divide(monthlyRate, MC)));
    }

    /** The monthly contribution a target requires. Exact inverse of sipFutureValue. */
    public static BigDecimal requiredSip(BigDecimal target, BigDecimal annualRatePct, int months) {
        if (target == null || target.signum() <= 0 || months <= 0) return BigDecimal.ZERO;

        BigDecimal monthlyRate = annualRatePct
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 10, RoundingMode.HALF_UP);

        if (monthlyRate.signum() == 0) {
            return target.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        BigDecimal compound = BigDecimal.ONE.add(monthlyRate).pow(months, MC);
        return target.divide(compound.subtract(BigDecimal.ONE).divide(monthlyRate, MC), 2, RoundingMode.HALF_UP);
    }

    /** Simple return: {@code (current - invested) / invested * 100}. */
    public static BigDecimal returnPct(BigDecimal invested, BigDecimal current) {
        if (invested == null || invested.signum() == 0) return BigDecimal.ZERO;
        return current.subtract(invested).multiply(HUNDRED).divide(invested, 2, RoundingMode.HALF_UP);
    }

    /** Compound annual growth rate. */
    public static BigDecimal cagr(BigDecimal invested, BigDecimal current, double years) {
        if (invested == null || invested.signum() <= 0 || years <= 0) return BigDecimal.ZERO;
        double ratio = current.doubleValue() / invested.doubleValue();
        if (ratio <= 0) return BigDecimal.valueOf(-100).setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf((Math.pow(ratio, 1.0 / years) - 1.0) * 100.0)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Annualised return, falling back to absolute return under one year. */
    public static BigDecimal annualised(BigDecimal invested, BigDecimal current, LocalDate since) {
        long days = ChronoUnit.DAYS.between(since, LocalDate.now());
        return days < 365 ? returnPct(invested, current) : cagr(invested, current, days / 365.0);
    }

    /** Restates an amount in future rupees at the given inflation rate. */
    public static BigDecimal inflate(BigDecimal amount, double inflationPct, int years) {
        if (amount == null || years <= 0) return amount;
        BigDecimal factor = BigDecimal.ONE
                .add(BigDecimal.valueOf(inflationPct).divide(HUNDRED, 10, RoundingMode.HALF_UP))
                .pow(years, MC);
        return round(amount.multiply(factor));
    }

    /** Years for money to double, by the rule of 72. */
    public static BigDecimal yearsToDouble(BigDecimal annualRatePct) {
        if (annualRatePct == null || annualRatePct.signum() <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(72).divide(annualRatePct, 2, RoundingMode.HALF_UP);
    }
}
