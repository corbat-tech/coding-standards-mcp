package com.example.saga.domain.exception;

/**
 * Exception thrown when a saga step fails during execution.
 */
public class SagaException extends RuntimeException {

    private final String code;
    private final String stepName;
    private final boolean retryable;

    public SagaException(String message, String code, String stepName, boolean retryable) {
        super(message);
        this.code = code;
        this.stepName = stepName;
        this.retryable = retryable;
    }

    public SagaException(String message, String code, String stepName, boolean retryable, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.stepName = stepName;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public String getStepName() {
        return stepName;
    }

    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return "SagaException{" +
               "code='" + code + '\'' +
               ", stepName='" + stepName + '\'' +
               ", retryable=" + retryable +
               ", message='" + getMessage() + '\'' +
               '}';
    }
}
