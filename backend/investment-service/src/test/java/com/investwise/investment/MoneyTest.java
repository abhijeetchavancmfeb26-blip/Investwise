package com.investwise.investment;

import com.investwise.investment.service.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** The arithmetic a user would notice being wrong, checked against independently computed values. */
@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("compounds a lump sum: 100000 at 12% for 10 years")
    void futureValue() {
        assertThat(Money.futureValue(new BigDecimal("100000"), new BigDecimal("12"), 10).doubleValue())
                .isCloseTo(310584.82, within(1.0));
    }

    @Test
    @DisplayName("values a 5000 monthly SIP at 12% over 10 years at roughly 11.6 lakh")
    void sipFutureValue() {
        BigDecimal result = Money.sipFutureValue(new BigDecimal("5000"), new BigDecimal("12"), 120);
        assertThat(result.doubleValue()).isCloseTo(1_150_193.0, within(2000.0));
        assertThat(result).isGreaterThan(new BigDecimal("600000"));
    }

    @Test
    @DisplayName("falls back to simple accumulation at a zero rate")
    void sipAtZeroRate() {
        assertThat(Money.sipFutureValue(new BigDecimal("1000"), BigDecimal.ZERO, 12))
                .isEqualByComparingTo(new BigDecimal("12000.00"));
    }

    @Test
    @DisplayName("requiredSip and sipFutureValue are exact inverses")
    void inverses() {
        BigDecimal target = new BigDecimal("2500000");
        BigDecimal rate = new BigDecimal("12");

        BigDecimal required = Money.requiredSip(target, rate, 180);
        assertThat(Money.sipFutureValue(required, rate, 180).doubleValue())
                .isCloseTo(target.doubleValue(), within(50.0));
    }

    @ParameterizedTest(name = "invested {0} worth {1} -> {2}%")
    @CsvSource({"100000, 150000, 50.00", "100000, 80000, -20.00", "100000, 100000, 0.00"})
    void returnPct(String invested, String current, String expected) {
        assertThat(Money.returnPct(new BigDecimal(invested), new BigDecimal(current)))
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    @DisplayName("returns zero rather than dividing by zero on an empty portfolio")
    void handlesZeroInvested() {
        assertThat(Money.returnPct(BigDecimal.ZERO, new BigDecimal("1000")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("computes CAGR: doubling over 5 years is about 14.87%")
    void cagr() {
        assertThat(Money.cagr(new BigDecimal("100000"), new BigDecimal("200000"), 5.0).doubleValue())
                .isCloseTo(14.87, within(0.05));
    }

    @Test
    @DisplayName("uses absolute return for holdings under a year old")
    void annualisedUnderOneYear() {
        assertThat(Money.annualised(new BigDecimal("100000"), new BigDecimal("110000"),
                LocalDate.now().minusMonths(6)))
                .isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("inflates a target: 10 lakh at 6% over 10 years becomes about 17.9 lakh")
    void inflate() {
        assertThat(Money.inflate(new BigDecimal("1000000"), 6.0, 10).doubleValue())
                .isCloseTo(1_790_847.0, within(500.0));
    }

    @ParameterizedTest(name = "rule of 72: {0}% doubles in {1} years")
    @CsvSource({"12, 6.00", "8, 9.00", "6, 12.00"})
    void yearsToDouble(String rate, String expected) {
        assertThat(Money.yearsToDouble(new BigDecimal(rate)))
                .isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    @DisplayName("guards against null and negative input instead of throwing")
    void handlesDegenerateInput() {
        assertThat(Money.futureValue(null, new BigDecimal("12"), 5)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(Money.sipFutureValue(new BigDecimal("-100"), new BigDecimal("12"), 12))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(Money.yearsToDouble(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
