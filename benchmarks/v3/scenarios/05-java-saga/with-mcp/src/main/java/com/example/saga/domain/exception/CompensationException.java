package com.example.saga.domain.exception;

public class CompensationException extends RuntimeException {
    public CompensationException(String message, Throwable cause) {
        super("Compensation failed: " + message, cause);
    }
}
