package com.example.saga.application.saga;

import com.example.saga.domain.exception.SagaException;

import java.util.Optional;

/**
 * Result of a saga step execution.
 *
 * @param <T> the type of data returned on success
 */
public final class StepResult<T> {

    private final boolean success;
    private final T data;
    private final SagaException error;

    private StepResult(boolean success, T data, SagaException error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /**
     * Creates a successful result with data.
     */
    public static <T> StepResult<T> success(T data) {
        return new StepResult<>(true, data, null);
    }

    /**
     * Creates a successful result without data.
     */
    public static <T> StepResult<T> success() {
        return new StepResult<>(true, null, null);
    }

    /**
     * Creates a failure result with an error.
     */
    public static <T> StepResult<T> failure(SagaException error) {
        return new StepResult<>(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public Optional<T> getData() {
        return Optional.ofNullable(data);
    }

    public Optional<SagaException> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public String toString() {
        return "StepResult{" +
               "success=" + success +
               ", data=" + data +
               ", error=" + error +
               '}';
    }
}
