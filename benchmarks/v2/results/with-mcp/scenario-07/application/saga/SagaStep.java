package com.example.transfer.application.saga;

public interface SagaStep<T> {
    void execute(T context);
    void compensate(T context);
    String getName();
}
