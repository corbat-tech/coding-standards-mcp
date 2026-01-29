package com.example.saga.application.saga;

import com.example.saga.domain.exception.SagaException;

import java.util.Optional;

/**
 * Result of a saga step compensation action.
 */
public final class CompensationResult {

    private final boolean success;
    private final SagaException error;

    private CompensationResult(boolean success, SagaException error) {
        this.success = success;
        this.error = error;
    }

    /**
     * Creates a successful compensation result.
     */
    public static CompensationResult success() {
        return new CompensationResult(true, null);
    }

    /**
     * Creates a failed compensation result with an error.
     */
    public static CompensationResult failure(SagaException error) {
        return new CompensationResult(false, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public Optional<SagaException> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public String toString() {
        return "CompensationResult{" +
               "success=" + success +
               ", error=" + error +
               '}';
    }
}
