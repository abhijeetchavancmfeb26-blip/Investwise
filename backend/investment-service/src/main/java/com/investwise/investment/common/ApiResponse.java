package com.investwise.investment.common;

import java.time.LocalDateTime;

/**
 * Uniform envelope returned by every endpoint.
 * <p>
 * A record rather than a Lombok-built class: it is immutable, needs no builder,
 * and the three static factories cover every case the controllers actually use.
 */
public record ApiResponse<T>(boolean success, String message, T data, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Success", data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, LocalDateTime.now());
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null, LocalDateTime.now());
    }
}
