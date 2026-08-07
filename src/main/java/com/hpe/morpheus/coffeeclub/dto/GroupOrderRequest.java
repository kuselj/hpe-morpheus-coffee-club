package com.hpe.morpheus.coffeeclub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The whole group order table, submitted in display order. Row order matters: it is the
 * tie-breaker when two coworkers share the same (lowest) fairness balance.
 */
public record GroupOrderRequest(

        @NotEmpty(message = "At least 1 person is required. Click the 'Add Person' button to add an individual.")
        @Size(max = 50, message = "A group order cannot contain more than 50 people.")
        List<@Valid OrderLineRequest> lines
) {
}
