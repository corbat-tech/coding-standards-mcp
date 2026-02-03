package com.example.saga.application.saga;

public record StepResult(boolean success, String errorMessage) {

    public static StepResult success() {
        return new StepResult(true, null);
    }

    public static StepResult failure(String errorMessage) {
        return new StepResult(false, errorMessage);
    }
}
