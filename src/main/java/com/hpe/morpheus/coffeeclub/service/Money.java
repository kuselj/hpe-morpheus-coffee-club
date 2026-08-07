package com.hpe.morpheus.coffeeclub.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Every monetary value in the application is a {@link BigDecimal} with a scale of 2.
 */
public final class Money {

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Money() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
