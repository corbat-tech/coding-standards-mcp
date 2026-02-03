package com.example.saga.application.saga.step;

import com.example.saga.application.port.OrderService;
import com.example.saga.application.saga.*;

public class CreateOrderStep implements SagaStep {

    private final OrderService orderService;

    public CreateOrderStep(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public String getName() { return "CreateOrder"; }

    @Override
    public StepResult execute(SagaContext context) {
        try {
            orderService.createOrder(context.getOrder());
            return StepResult.success();
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }

    @Override
    public void compensate(SagaContext context) {
        orderService.cancelOrder(context.getOrder().getId());
    }
}
