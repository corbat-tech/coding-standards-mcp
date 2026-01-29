package com.example.order.domain.event;

import com.example.order.domain.valueobject.OrderId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when a new Order is created.
 */
public final class OrderCreated implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredOn;
    private final OrderId orderId;
    private final String customerId;

    public OrderCreated(OrderId orderId, String customerId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getEventType() {
        return "OrderCreated";
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderCreated that = (OrderCreated) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "OrderCreated{" +
                "eventId=" + eventId +
                ", occurredOn=" + occurredOn +
                ", orderId=" + orderId +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}
