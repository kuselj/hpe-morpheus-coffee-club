package com.hpe.morpheus.coffeeclub.dto;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error payload for every failed API call.
 *
 * @param timestamp    when the failure happened
 * @param status       HTTP status code
 * @param message      headline message, safe to show to the user
 * @param fieldErrors  per-field problems, keyed back to the submitted row index where applicable
 */
public record ApiErrorResponse(Instant timestamp,
                               int status,
                               String message,
                               List<FieldErrorDetail> fieldErrors) {

    /**
     * @param lineIndex zero-based index of the offending table row, or {@code null} when the
     *                  problem is not tied to a specific row
     * @param field     logical field name ({@code name}, {@code drink}, {@code price}, ...)
     * @param message   human-readable explanation
     */
    public record FieldErrorDetail(Integer lineIndex, String field, String message) {
    }
}
