package com.example.order.domain.event;

import java.time.Instant;

public class OrderCreatedEvent extends DomainEvent {
    private final String customerId;

    public OrderCreatedEvent(String orderId, String customerId, int version, Instant occurredAt) {
        super(orderId, version, occurredAt);
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public String getEventType() {
        return "OrderCreated";
    }
}
