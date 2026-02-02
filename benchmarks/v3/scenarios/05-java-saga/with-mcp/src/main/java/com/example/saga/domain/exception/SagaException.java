package com.example.saga.domain.exception;

public class SagaException extends RuntimeException {
    private final String stepName;

    public SagaException(String stepName, String message) {
        super("Saga failed at step [" + stepName + "]: " + message);
        this.stepName = stepName;
    }

    public SagaException(String stepName, String message, Throwable cause) {
        super("Saga failed at step [" + stepName + "]: " + message, cause);
        this.stepName = stepName;
    }

    public String getStepName() { return stepName; }
}
