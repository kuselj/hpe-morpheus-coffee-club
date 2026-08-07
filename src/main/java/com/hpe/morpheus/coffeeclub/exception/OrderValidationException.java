package com.hpe.morpheus.coffeeclub.exception;

import com.hpe.morpheus.coffeeclub.dto.ApiErrorResponse.FieldErrorDetail;

import java.util.List;

/**
 * Raised for business rules that bean validation cannot express, such as duplicate names within a
 * single submission or a group order in which nobody is actually ordering.
 */
public class OrderValidationException extends RuntimeException {

    private final transient List<FieldErrorDetail> fieldErrors;

    public OrderValidationException(String message) {
        this(message, List.of());
    }

    public OrderValidationException(String message, List<FieldErrorDetail> fieldErrors) {
        super(message);
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }
}
