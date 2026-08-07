package com.hpe.morpheus.coffeeclub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One row of the group order table as submitted by the UI.
 *
 * @param name      coworker name, required
 * @param drink     drink description, required
 * @param price     cost of this person's drink today; {@code 0.00} means "not ordering today"
 * @param isRemoved when {@code true} the person is dropped from all future pre-populations
 */
public record OrderLineRequest(

        @NotBlank(message = "Name is required.")
        @Size(max = 60, message = "Name must be 60 characters or fewer.")
        @Pattern(regexp = "^[\\p{L}][\\p{L} .'\\-]*$",
                message = "Name may only contain letters, spaces, apostrophes, hyphens and periods.")
        String name,

        @NotBlank(message = "Drink is required.")
        @Size(max = 80, message = "Drink must be 80 characters or fewer.")
        String drink,

        @NotNull(message = "Price is required.")
        @DecimalMin(value = "0.00", message = "Price cannot be negative.")
        @DecimalMax(value = "999.99", message = "Price cannot exceed 999.99.")
        @Digits(integer = 3, fraction = 2, message = "Price must have at most 2 decimal places.")
        BigDecimal price,

        @NotNull(message = "Remove flag is required.")
        Boolean isRemoved
) {
}
