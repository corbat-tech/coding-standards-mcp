package application.command;

import domain.valueobject.OrderId;

import java.util.Objects;

/**
 * Command to confirm an order.
 */
public final class ConfirmOrderCommand {

    private final OrderId orderId;

    public ConfirmOrderCommand(OrderId orderId) {
        this.orderId = Objects.requireNonNull(orderId, "OrderId cannot be null");
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
