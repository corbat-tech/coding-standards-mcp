package com.example.transfer.application.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SagaOrchestrator<T> {
    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final List<SagaStep<T>> steps;
    private final List<SagaStep<T>> executedSteps;

    public SagaOrchestrator(List<SagaStep<T>> steps) {
        this.steps = steps;
        this.executedSteps = new ArrayList<>();
    }

    public void execute(T context) {
        for (SagaStep<T> step : steps) {
            try {
                log.info("Executing step: {}", step.getName());
                step.execute(context);
                executedSteps.add(step);
                log.info("Step completed: {}", step.getName());
            } catch (Exception e) {
                log.error("Step failed: {} - {}", step.getName(), e.getMessage());
                rollback(context);
                throw new SagaExecutionException(
                    "Saga failed at step: " + step.getName(),
                    e
                );
            }
        }
    }

    private void rollback(T context) {
        log.info("Starting rollback for {} executed steps", executedSteps.size());

        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            SagaStep<T> step = executedSteps.get(i);
            try {
                log.info("Compensating step: {}", step.getName());
                step.compensate(context);
                log.info("Compensation completed: {}", step.getName());
            } catch (Exception e) {
                log.error("Compensation failed for step: {} - {}",
                    step.getName(), e.getMessage());
            }
        }
    }
}
