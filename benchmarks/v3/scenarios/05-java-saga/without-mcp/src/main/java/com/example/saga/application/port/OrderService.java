package com.example.saga.application.port;

import com.example.saga.domain.entity.Order;
import com.example.saga.domain.valueobject.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for order management operations.
 */
public interface OrderService {

    /**
     * Creates a new order.
     *
     * @param customerId  the customer ID
     * @param items       the order items
     * @param totalAmount the total order amount
     * @return the created order
     */
    Order createOrder(String customerId, List<OrderItem> items, BigDecimal totalAmount);

    /**
     * Cancels an existing order.
     *
     * @param orderId the order ID to cancel
     */
    void cancelOrder(String orderId);

    /**
     * Retrieves an order by ID.
     *
     * @param orderId the order ID
     * @return the order if found
     */
    Optional<Order> getOrder(String orderId);
}
