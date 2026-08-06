package com.investwise.user.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns every exception into the same JSON shape.
 * <p>
 * Four handlers replace the original fifteen: one for our own exception, one for
 * bean validation, one for access denial, and a catch-all. Everything else was
 * already producing a sensible default.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Error body. {@code fieldErrors} is populated only on validation failures. */
    public record ErrorResponse(boolean success, int status, String message,
                                String path, Map<String, String> fieldErrors,
                                LocalDateTime timestamp) { }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        log.warn("{} at {} -> {}", ex.getStatus(), request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            fieldErrors.putIfAbsent(field, error.getDefaultMessage());
        });
        return build(HttpStatus.BAD_REQUEST,
                "Validation failed for %d field(s)".formatted(fieldErrors.size()), request, fieldErrors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to do that", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong. Please try again.", request, Map.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                false, status.value(), message, request.getRequestURI(), fields, LocalDateTime.now()));
    }
}
