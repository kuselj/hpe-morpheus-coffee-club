package com.hpe.morpheus.coffeeclub.dto;

import java.math.BigDecimal;

/**
 * Lifetime fairness figures for one coworker.
 *
 * @param name           coworker name as most recently recorded
 * @param totalPaid      lifetime sum of {@code total_paid_today}
 * @param totalConsumed  lifetime sum of {@code price}
 * @param netDifference  {@code totalPaid - totalConsumed}; the lowest value pays next
 */
public record CoworkerBalance(String name,
                              BigDecimal totalPaid,
                              BigDecimal totalConsumed,
                              BigDecimal netDifference) {
}
