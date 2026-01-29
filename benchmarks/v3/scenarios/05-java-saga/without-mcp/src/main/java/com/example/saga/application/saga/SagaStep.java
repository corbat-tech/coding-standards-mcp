package com.example.saga.application.saga;

/**
 * Interface for a saga step with execute and compensate operations.
 *
 * @param <T> the type of data returned by the execute operation
 */
public interface SagaStep<T> {

    /**
     * Gets the name of this saga step.
     */
    String getName();

    /**
     * Executes the saga step.
     *
     * @param context the saga context containing shared state
     * @return the result of the step execution
     */
    StepResult<T> execute(SagaContext context);

    /**
     * Compensates (rolls back) the saga step.
     *
     * @param context the saga context containing shared state
     * @return the result of the compensation
     */
    CompensationResult compensate(SagaContext context);
}
