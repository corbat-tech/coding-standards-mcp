package com.example.saga.application.saga.step;

import com.example.saga.application.port.ShippingService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.SagaStep;
import com.example.saga.application.saga.StepResult;
import com.example.saga.domain.entity.Order;
import com.example.saga.domain.exception.SagaException;

/**
 * Saga step for shipping the order.
 */
public class ShipOrderStep implements SagaStep<String> {

    private static final String STEP_NAME = "ShipOrder";

    private final ShippingService shippingService;

    public ShipOrderStep(ShippingService shippingService) {
        this.shippingService = shippingService;
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

            String trackingId = shippingService.createShipment(
                    context.getOrderId(),
                    order.getCustomerId()
            );

            return StepResult.success(trackingId);
        } catch (SagaException e) {
            return StepResult.failure(e);
        } catch (Exception e) {
            return StepResult.failure(new SagaException(
                    "Failed to ship order: " + e.getMessage(),
                    "SHIPPING_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }

    @Override
    public CompensationResult compensate(SagaContext context) {
        try {
            context.getShipmentTrackingId().ifPresent(
                    shippingService::cancelShipment
            );
            return CompensationResult.success();
        } catch (Exception e) {
            return CompensationResult.failure(new SagaException(
                    "Failed to cancel shipment: " + e.getMessage(),
                    "SHIPMENT_CANCELLATION_FAILED",
                    STEP_NAME,
                    true,
                    e
            ));
        }
    }
}
