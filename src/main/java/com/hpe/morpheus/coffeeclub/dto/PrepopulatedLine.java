package com.hpe.morpheus.coffeeclub.dto;

import java.math.BigDecimal;

/**
 * A pre-populated table row derived from order history.
 *
 * @param name  coworker name carried over from the most recent order
 * @param drink drink carried over from the most recent order for that person
 * @param price the most recent price greater than zero for that person, or {@code 0.00}
 */
public record PrepopulatedLine(String name, String drink, BigDecimal price) {
}
