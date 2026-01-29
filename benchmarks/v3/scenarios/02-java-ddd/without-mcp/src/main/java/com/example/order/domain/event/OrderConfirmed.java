package com.example.order.domain.event;

import com.example.order.domain.valueobject.Money;
import com.example.order.domain.valueobject.OrderId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when an Order is confirmed.
 */
public final class OrderConfirmed implements DomainEvent {

    private final UUID eventId;
    private final Instant occurredOn;
    private final OrderId orderId;
    private final Money totalAmount;
    private final int itemCount;

    public OrderConfirmed(OrderId orderId, Money totalAmount, int itemCount) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "TotalAmount cannot be null");
        this.itemCount = itemCount;
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
        return "OrderConfirmed";
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public int getItemCount() {
        return itemCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderConfirmed that = (OrderConfirmed) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "OrderConfirmed{" +
                "eventId=" + eventId +
                ", occurredOn=" + occurredOn +
                ", orderId=" + orderId +
                ", totalAmount=" + totalAmount +
                ", itemCount=" + itemCount +
                '}';
    }
}
