package com.example.saga.application.saga;

import com.example.saga.domain.exception.CompensationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final List<SagaStep> steps;

    public SagaOrchestrator(List<SagaStep> steps) {
        this.steps = new ArrayList<>(steps);
    }

    public SagaExecutionResult execute(SagaContext context) {
        List<SagaStep> completedSteps = new ArrayList<>();

        for (SagaStep step : steps) {
            log.info("Executing step: {}", step.getName());
            StepResult result = step.execute(context);

            if (!result.success()) {
                log.warn("Step {} failed: {}", step.getName(), result.errorMessage());
                List<String> compensated = compensate(completedSteps, context);
                return SagaExecutionResult.failure(step.getName(), result.errorMessage(), compensated);
            }
            completedSteps.add(step);
        }

        log.info("Saga completed successfully");
        return SagaExecutionResult.success();
    }

    private List<String> compensate(List<SagaStep> completedSteps, SagaContext context) {
        List<String> compensated = new ArrayList<>();
        List<SagaStep> reversed = new ArrayList<>(completedSteps);
        Collections.reverse(reversed);

        for (SagaStep step : reversed) {
            try {
                log.info("Compensating step: {}", step.getName());
                step.compensate(context);
                compensated.add(step.getName());
            } catch (Exception e) {
                log.error("Compensation failed for step: {}", step.getName(), e);
                throw new CompensationException(step.getName(), e);
            }
        }
        return compensated;
    }
}
