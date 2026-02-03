package com.example.saga.application.saga;

public interface SagaStep {
    String getName();
    StepResult execute(SagaContext context);
    void compensate(SagaContext context);
}
