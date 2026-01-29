package com.example.saga.application.saga;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Result of a saga execution.
 */
public final class SagaExecutionResult {

    private final boolean success;
    private final SagaContext context;
    private final List<String> completedSteps;
    private final String failedStep;
    private final Exception error;
    private final CompensationSummary compensationResults;

    private SagaExecutionResult(Builder builder) {
        this.success = builder.success;
        this.context = builder.context;
        this.completedSteps = Collections.unmodifiableList(builder.completedSteps);
        this.failedStep = builder.failedStep;
        this.error = builder.error;
        this.compensationResults = builder.compensationResults;
    }

    /**
     * Creates a successful result.
     */
    public static SagaExecutionResult success(SagaContext context, List<String> completedSteps) {
        return new Builder()
                .success(true)
                .context(context)
                .completedSteps(completedSteps)
                .build();
    }

    /**
     * Creates a failure result.
     */
    public static SagaExecutionResult failure(SagaContext context, List<String> completedSteps,
                                               String failedStep, Exception error,
                                               CompensationSummary compensationResults) {
        return new Builder()
                .success(false)
                .context(context)
                .completedSteps(completedSteps)
                .failedStep(failedStep)
                .error(error)
                .compensationResults(compensationResults)
                .build();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public SagaContext getContext() {
        return context;
    }

    public List<String> getCompletedSteps() {
        return completedSteps;
    }

    public Optional<String> getFailedStep() {
        return Optional.ofNullable(failedStep);
    }

    public Optional<Exception> getError() {
        return Optional.ofNullable(error);
    }

    public Optional<CompensationSummary> getCompensationResults() {
        return Optional.ofNullable(compensationResults);
    }

    @Override
    public String toString() {
        return "SagaExecutionResult{" +
               "success=" + success +
               ", context=" + context +
               ", completedSteps=" + completedSteps +
               ", failedStep='" + failedStep + '\'' +
               ", error=" + error +
               ", compensationResults=" + compensationResults +
               '}';
    }

    private static class Builder {
        private boolean success;
        private SagaContext context;
        private List<String> completedSteps = Collections.emptyList();
        private String failedStep;
        private Exception error;
        private CompensationSummary compensationResults;

        Builder success(boolean success) {
            this.success = success;
            return this;
        }

        Builder context(SagaContext context) {
            this.context = context;
            return this;
        }

        Builder completedSteps(List<String> completedSteps) {
            this.completedSteps = completedSteps;
            return this;
        }

        Builder failedStep(String failedStep) {
            this.failedStep = failedStep;
            return this;
        }

        Builder error(Exception error) {
            this.error = error;
            return this;
        }

        Builder compensationResults(CompensationSummary compensationResults) {
            this.compensationResults = compensationResults;
            return this;
        }

        SagaExecutionResult build() {
            return new SagaExecutionResult(this);
        }
    }
}
