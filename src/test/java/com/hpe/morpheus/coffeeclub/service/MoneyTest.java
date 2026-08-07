package com.hpe.morpheus.coffeeclub.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Monetary scaling")
class MoneyTest {

    @Test
    @DisplayName("every amount is scaled to two decimal places, rounding half up")
    void scalesToTwoDecimalPlaces() {
        assertThat(Money.scale(new BigDecimal("3.5"))).isEqualByComparingTo("3.50");
        assertThat(Money.scale(new BigDecimal("3.505"))).isEqualByComparingTo("3.51");
        assertThat(Money.scale(new BigDecimal("3.504"))).isEqualByComparingTo("3.50");
        assertThat(Money.scale(new BigDecimal("3.50")).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("null is treated as zero")
    void nullBecomesZero() {
        assertThat(Money.scale(null)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("only amounts above zero count as a participating order")
    void detectsPositiveAmounts() {
        assertThat(Money.isPositive(new BigDecimal("0.01"))).isTrue();
        assertThat(Money.isPositive(new BigDecimal("0.00"))).isFalse();
        assertThat(Money.isPositive(new BigDecimal("-1.00"))).isFalse();
        assertThat(Money.isPositive(null)).isFalse();
    }
}
