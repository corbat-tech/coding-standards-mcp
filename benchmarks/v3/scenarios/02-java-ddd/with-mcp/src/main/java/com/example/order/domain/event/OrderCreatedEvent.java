package com.example.order.domain.event;

import com.example.order.domain.valueobject.OrderId;
import java.time.Instant;

public record OrderCreatedEvent(OrderId orderId, Instant occurredOn) implements DomainEvent {

    public OrderCreatedEvent(OrderId orderId) {
        this(orderId, Instant.now());
    }
}
