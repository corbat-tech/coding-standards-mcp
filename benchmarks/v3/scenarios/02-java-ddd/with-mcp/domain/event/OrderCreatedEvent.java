package domain.event;

import domain.valueobject.OrderId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when a new Order is created.
 */
public final class OrderCreatedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "order.created";

    private final UUID eventId;
    private final Instant occurredAt;
    private final OrderId orderId;
    private final String customerId;

    public OrderCreatedEvent(OrderId orderId, String customerId) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "CustomerId cannot be null");
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
               "eventId=" + eventId +
               ", occurredAt=" + occurredAt +
               ", orderId=" + orderId +
               ", customerId='" + customerId + '\'' +
               '}';
    }
}
