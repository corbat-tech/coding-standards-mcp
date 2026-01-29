package domain.event;

import domain.valueobject.Money;
import domain.valueobject.OrderId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event raised when an Order is confirmed.
 */
public final class OrderConfirmedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "order.confirmed";

    private final UUID eventId;
    private final Instant occurredAt;
    private final OrderId orderId;
    private final Money totalAmount;
    private final int itemCount;

    public OrderConfirmedEvent(OrderId orderId, Money totalAmount, int itemCount) {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "TotalAmount cannot be null");
        this.itemCount = itemCount;
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

    public Money getTotalAmount() {
        return totalAmount;
    }

    public int getItemCount() {
        return itemCount;
    }

    @Override
    public String toString() {
        return "OrderConfirmedEvent{" +
               "eventId=" + eventId +
               ", occurredAt=" + occurredAt +
               ", orderId=" + orderId +
               ", totalAmount=" + totalAmount +
               ", itemCount=" + itemCount +
               '}';
    }
}
