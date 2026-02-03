package com.example.saga.application.saga.step;

import com.example.saga.application.port.ShippingService;
import com.example.saga.application.saga.*;

public class ShipOrderStep implements SagaStep {

    private static final String SHIPMENT_ID_KEY = "shipmentId";
    private final ShippingService shippingService;

    public ShipOrderStep(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @Override
    public String getName() { return "ShipOrder"; }

    @Override
    public StepResult execute(SagaContext context) {
        try {
            String shipmentId = shippingService.createShipment(
                    context.getOrder().getId(), context.getShippingAddress());
            context.put(SHIPMENT_ID_KEY, shipmentId);
            return StepResult.success();
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }

    @Override
    public void compensate(SagaContext context) {
        if (context.has(SHIPMENT_ID_KEY)) {
            shippingService.cancelShipment(context.get(SHIPMENT_ID_KEY));
        }
    }
}
