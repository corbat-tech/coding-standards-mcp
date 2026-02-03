package com.example.saga.application.saga.step;

import com.example.saga.application.port.InventoryService;
import com.example.saga.application.saga.*;
import com.example.saga.domain.entity.Order;

public class ReserveInventoryStep implements SagaStep {

    private final InventoryService inventoryService;

    public ReserveInventoryStep(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public String getName() { return "ReserveInventory"; }

    @Override
    public StepResult execute(SagaContext context) {
        try {
            Order order = context.getOrder();
            inventoryService.reserveInventory(order.getId(), order.getItems());
            return StepResult.success();
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }

    @Override
    public void compensate(SagaContext context) {
        Order order = context.getOrder();
        inventoryService.releaseInventory(order.getId(), order.getItems());
    }
}
