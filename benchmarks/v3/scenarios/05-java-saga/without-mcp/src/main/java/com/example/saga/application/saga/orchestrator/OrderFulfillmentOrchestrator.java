package com.example.saga.application.saga.orchestrator;

import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.CompensationSummary;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.SagaExecutionResult;
import com.example.saga.application.saga.SagaOrchestrator;
import com.example.saga.application.saga.SagaStep;
import com.example.saga.application.saga.StepResult;
import com.example.saga.domain.entity.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrator for order fulfillment saga.
 * Executes steps in sequence and handles compensation on failure.
 */
public class OrderFulfillmentOrchestrator implements SagaOrchestrator {

    private final List<SagaStep<?>> steps;

    public OrderFulfillmentOrchestrator(List<SagaStep<?>> steps) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    @Override
    public SagaExecutionResult execute(SagaContext initialContext) {
        SagaContext context = initialContext;
        List<String> completedSteps = new ArrayList<>();
        List<SagaStep<?>> executedSteps = new ArrayList<>();

        for (SagaStep<?> step : steps) {
            StepResult<?> result = step.execute(context);

            if (result.isFailure()) {
                CompensationSummary compensationResults = compensate(executedSteps, context);

                return SagaExecutionResult.failure(
                        context,
                        completedSteps,
                        step.getName(),
                        result.getError().orElse(null),
                        compensationResults
                );
            }

            completedSteps.add(step.getName());
            executedSteps.add(step);
            context = updateContext(context, step.getName(), result.getData().orElse(null));
        }

        return SagaExecutionResult.success(context, completedSteps);
    }

    @Override
    public List<SagaStep<?>> getSteps() {
        return steps;
    }

    /**
     * Compensates executed steps in reverse order.
     */
    private CompensationSummary compensate(List<SagaStep<?>> executedSteps, SagaContext context) {
        if (executedSteps.isEmpty()) {
            return CompensationSummary.empty();
        }

        List<String> completedCompensations = new ArrayList<>();
        List<String> failedCompensations = new ArrayList<>();

        // Compensate in reverse order
        for (int i = executedSteps.size() - 1; i >= 0; i--) {
            SagaStep<?> step = executedSteps.get(i);
            CompensationResult result = step.compensate(context);

            if (result.isSuccess()) {
                completedCompensations.add(step.getName());
            } else {
                failedCompensations.add(step.getName());
            }
        }

        return CompensationSummary.of(completedCompensations, failedCompensations);
    }

    /**
     * Updates the context with the result of a step execution.
     */
    private SagaContext updateContext(SagaContext context, String stepName, Object data) {
        if (data == null) {
            return context;
        }

        switch (stepName) {
            case "CreateOrder":
                return context.withOrder((Order) data);
            case "ReserveInventory":
                return context.withInventoryReservation((String) data);
            case "ProcessPayment":
                return context.withPaymentTransaction((String) data);
            case "ShipOrder":
                return context.withShipmentTracking((String) data);
            default:
                return context;
        }
    }
}
