package com.example.payment.adapter.input.rest.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for error responses.
 */
public record ErrorResponse(
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, LocalDateTime.now());
    }

    public static ErrorResponse of(String error, String message, List<String> details) {
        return new ErrorResponse(error, message, details, LocalDateTime.now());
    }
}
