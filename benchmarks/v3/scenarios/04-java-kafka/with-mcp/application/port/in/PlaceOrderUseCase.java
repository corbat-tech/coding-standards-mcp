package com.example.order.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input port for placing orders.
 * Defines the contract for order placement use case.
 */
public interface PlaceOrderUseCase {

    /**
     * Places a new order and publishes an OrderCreatedEvent.
     *
     * @param command the place order command
     * @return the result of the order placement
     */
    PlaceOrderResult placeOrder(PlaceOrderCommand command);

    /**
     * Command object for placing an order.
     */
    record PlaceOrderCommand(
        String customerId,
        List<OrderItemCommand> items
    ) {
        public PlaceOrderCommand {
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("customerId must not be blank");
            }
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Order must have at least one item");
            }
        }
    }

    /**
     * Command object for an order item.
     */
    record OrderItemCommand(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
    ) {}

    /**
     * Result of placing an order.
     */
    record PlaceOrderResult(
        String orderId,
        boolean success,
        String message
    ) {
        public static PlaceOrderResult success(String orderId) {
            return new PlaceOrderResult(orderId, true, "Order placed successfully");
        }

        public static PlaceOrderResult failure(String message) {
            return new PlaceOrderResult(null, false, message);
        }
    }
}
