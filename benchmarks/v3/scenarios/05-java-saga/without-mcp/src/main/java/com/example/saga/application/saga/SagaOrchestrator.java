package com.example.saga.application.saga;

import java.util.List;

/**
 * Interface for saga orchestrator that coordinates the execution of saga steps.
 */
public interface SagaOrchestrator {

    /**
     * Executes the saga with all registered steps.
     *
     * @param context the initial saga context
     * @return the result of the saga execution
     */
    SagaExecutionResult execute(SagaContext context);

    /**
     * Gets the list of registered steps.
     */
    List<SagaStep<?>> getSteps();
}
