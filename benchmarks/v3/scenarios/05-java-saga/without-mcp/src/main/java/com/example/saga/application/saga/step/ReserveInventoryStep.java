package com.example.saga.application.saga.step;

import com.example.saga.application.port.InventoryService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.SagaStep;
import com.example.saga.application.saga.StepResult;
import com.example.saga.domain.entity.Order;
import com.example.saga.domain.exception.SagaException;

import java.util.ArrayList;

/**
 * Saga step for reserving inventory.
 */
public class ReserveInventoryStep implements SagaStep<String> {

    private static final String STEP_NAME = "ReserveInventory";

    private final InventoryService inventoryService;

    public ReserveInventoryStep(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public String getName() {
        return STEP_NAME;
    }

    @Override
    public StepResult<String> execute(SagaContext context) {
        try {
            Order order = context.getOrder()
                    .orElseThrow(() -> new SagaException(
                            "Order not found in context",
                            "ORDER_NOT_FOUND",
                            STEP_NAME,
                            false
                    ));

            String reservationId = inventoryService.reserveInventory(
                    context.getOrderId(),
                    new ArrayList<>(order.getItems())
            );

            return StepResult.success(reservationId);
        } catch (SagaException e) {
            return StepResult.failure(e);
        } catch (Exception e) {
            return StepResult.failure(new SagaException(
                    "Failed to reserve inventory: " + e.getMessage(),
                    "INVENTORY_RESERVATION_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }

    @Override
    public CompensationResult compensate(SagaContext context) {
        try {
            context.getInventoryReservationId().ifPresent(
                    inventoryService::releaseInventory
            );
            return CompensationResult.success();
        } catch (Exception e) {
            return CompensationResult.failure(new SagaException(
                    "Failed to release inventory: " + e.getMessage(),
                    "INVENTORY_RELEASE_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }
}
