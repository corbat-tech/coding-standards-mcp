package com.example.saga.application.saga;

import java.util.List;

public record SagaExecutionResult(
        boolean success,
        String failedStepName,
        String errorMessage,
        List<String> compensatedSteps
) {
    public static SagaExecutionResult success() {
        return new SagaExecutionResult(true, null, null, List.of());
    }

    public static SagaExecutionResult failure(String failedStep, String error, List<String> compensated) {
        return new SagaExecutionResult(false, failedStep, error, compensated);
    }
}
