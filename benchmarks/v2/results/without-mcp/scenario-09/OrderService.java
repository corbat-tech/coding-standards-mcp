package com.example.eventsourcing;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderService {
    private final EventStore eventStore;
    private final OrderProjection projection;

    public OrderService(EventStore eventStore, OrderProjection projection) {
        this.eventStore = eventStore;
        this.projection = projection;
    }

    public String createOrder(String customerId) {
        String orderId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, customerId, 1);
        eventStore.append(event);
        projection.apply(event);
        return orderId;
    }

    public void addItem(String orderId, String productId, String productName, int quantity, BigDecimal price) {
        Order order = getOrder(orderId);
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot add items to a shipped order");
        }

        ItemAddedEvent event = new ItemAddedEvent(orderId, productId, productName, quantity, price, order.getVersion() + 1);
        eventStore.append(event);
        projection.apply(event);
    }

    public void shipOrder(String orderId, String shippingAddress, String trackingNumber) {
        Order order = getOrder(orderId);
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order is already shipped");
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot ship an empty order");
        }

        OrderShippedEvent event = new OrderShippedEvent(orderId, shippingAddress, trackingNumber, order.getVersion() + 1);
        eventStore.append(event);
        projection.apply(event);
    }

    public Order getOrder(String orderId) {
        var events = eventStore.getEvents(orderId);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return Order.reconstruct(events);
    }
}
