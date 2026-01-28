package com.example.order.domain.event;

import java.time.Instant;

public class OrderShippedEvent extends DomainEvent {
    private final String trackingNumber;
    private final String carrier;

    public OrderShippedEvent(
        String orderId,
        String trackingNumber,
        String carrier,
        int version,
        Instant occurredAt
    ) {
        super(orderId, version, occurredAt);
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    @Override
    public String getEventType() {
        return "OrderShipped";
    }
}
