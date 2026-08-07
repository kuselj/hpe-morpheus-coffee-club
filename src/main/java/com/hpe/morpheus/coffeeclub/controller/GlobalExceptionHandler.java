package com.hpe.morpheus.coffeeclub.controller;

import com.hpe.morpheus.coffeeclub.dto.ApiErrorResponse;
import com.hpe.morpheus.coffeeclub.dto.ApiErrorResponse.FieldErrorDetail;
import com.hpe.morpheus.coffeeclub.exception.OrderValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates validation and unexpected failures into the uniform {@link ApiErrorResponse} shape the
 * UI knows how to render, including per-row field errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Matches the {@code lines[3].price} style paths produced by bean validation. */
    private static final Pattern LINE_PATH = Pattern.compile("^lines\\[(\\d+)]\\.(\\w+)$");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorDetail> details = new ArrayList<>();

        exception.getBindingResult().getGlobalErrors().forEach(error ->
                details.add(new FieldErrorDetail(null, null, error.getDefaultMessage())));

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            Matcher matcher = LINE_PATH.matcher(error.getField());
            if (matcher.matches()) {
                details.add(new FieldErrorDetail(
                        Integer.valueOf(matcher.group(1)), matcher.group(2), error.getDefaultMessage()));
            } else {
                details.add(new FieldErrorDetail(null, error.getField(), error.getDefaultMessage()));
            }
        });

        // Anything not tied to a specific table row (a global error, or a problem with the table
        // as a whole such as it being empty) makes a better headline than a per-cell message.
        String headline = details.stream()
                .filter(detail -> detail.lineIndex() == null)
                .map(FieldErrorDetail::message)
                .findFirst()
                .orElse("Please correct the highlighted fields and try again.");

        return respond(HttpStatus.BAD_REQUEST, headline, details);
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderValidation(OrderValidationException exception) {
        return respond(HttpStatus.BAD_REQUEST, exception.getMessage(), exception.getFieldErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.warn("Rejected malformed request body: {}", exception.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "The request could not be read. Please check the values entered.",
                List.of());
    }

    /**
     * Routing failures must keep their own status. Without these the catch-all below would turn
     * every unknown URL into a 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException exception) {
        return respond(HttpStatus.NOT_FOUND, "No such resource: " + exception.getResourcePath(), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED,
                "%s is not supported for this endpoint.".formatted(exception.getMethod()), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type %s is not supported. Use application/json.".formatted(exception.getContentType()),
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected failure handling request", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on the server. Please try again.", List.of());
    }

    private ResponseEntity<ApiErrorResponse> respond(HttpStatus status,
                                                     String message,
                                                     List<FieldErrorDetail> details) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(), status.value(), message, details));
    }
}
