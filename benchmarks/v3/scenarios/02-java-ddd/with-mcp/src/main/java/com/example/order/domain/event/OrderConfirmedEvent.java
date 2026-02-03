package com.example.order.domain.event;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;
import java.time.Instant;

public record OrderConfirmedEvent(OrderId orderId, Money total, Instant occurredOn) implements DomainEvent {

    public OrderConfirmedEvent(OrderId orderId, Money total) {
        this(orderId, total, Instant.now());
    }
}
