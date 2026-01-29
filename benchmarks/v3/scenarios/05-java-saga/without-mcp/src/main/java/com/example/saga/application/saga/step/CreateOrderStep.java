package com.example.saga.application.saga.step;

import com.example.saga.application.port.OrderService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.SagaStep;
import com.example.saga.application.saga.StepResult;
import com.example.saga.domain.entity.Order;
import com.example.saga.domain.exception.SagaException;
import com.example.saga.domain.valueobject.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Saga step for creating an order.
 */
public class CreateOrderStep implements SagaStep<Order> {

    private static final String STEP_NAME = "CreateOrder";

    private final OrderService orderService;

    public CreateOrderStep(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public String getName() {
        return STEP_NAME;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult<Order> execute(SagaContext context) {
        try {
            String customerId = context.getMetadata("customerId", String.class)
                    .orElseThrow(() -> new IllegalStateException("Customer ID not found in context"));

            List<OrderItem> items = context.getMetadata("items", List.class)
                    .orElseThrow(() -> new IllegalStateException("Items not found in context"));

            BigDecimal totalAmount = context.getMetadata("totalAmount", BigDecimal.class)
                    .orElseThrow(() -> new IllegalStateException("Total amount not found in context"));

            Order order = orderService.createOrder(customerId, items, totalAmount);

            return StepResult.success(order);
        } catch (Exception e) {
            return StepResult.failure(new SagaException(
                    "Failed to create order: " + e.getMessage(),
                    "ORDER_CREATION_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }

    @Override
    public CompensationResult compensate(SagaContext context) {
        try {
            context.getOrder().ifPresent(order ->
                    orderService.cancelOrder(order.getId())
            );
            return CompensationResult.success();
        } catch (Exception e) {
            return CompensationResult.failure(new SagaException(
                    "Failed to cancel order: " + e.getMessage(),
                    "ORDER_CANCELLATION_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }
}
