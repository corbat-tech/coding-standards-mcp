package domain.exception;

import domain.valueobject.OrderId;

/**
 * Exception thrown when an order cannot be found.
 */
public class OrderNotFoundException extends DomainException {

    private final OrderId orderId;

    public OrderNotFoundException(OrderId orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    public OrderId getOrderId() {
        return orderId;
    }
}
