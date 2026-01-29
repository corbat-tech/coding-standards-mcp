package com.example.saga.domain.entity;

import com.example.saga.domain.valueobject.OrderItem;
import com.example.saga.domain.valueobject.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Order entity representing an order in the fulfillment process.
 */
public final class Order {

    private final String id;
    private final String customerId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;
    private final OrderStatus status;
    private final Instant createdAt;

    private Order(String id, String customerId, List<OrderItem> items,
                  BigDecimal totalAmount, OrderStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.items = Collections.unmodifiableList(Objects.requireNonNull(items, "Items cannot be null"));
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    /**
     * Creates a new order with PENDING status.
     */
    public static Order create(String id, String customerId, List<OrderItem> items, BigDecimal totalAmount) {
        return new Order(id, customerId, items, totalAmount, OrderStatus.PENDING, Instant.now());
    }

    /**
     * Creates a copy of the order with a new status.
     */
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerId, items, totalAmount, newStatus, createdAt);
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order{" +
               "id='" + id + '\'' +
               ", customerId='" + customerId + '\'' +
               ", items=" + items +
               ", totalAmount=" + totalAmount +
               ", status=" + status +
               ", createdAt=" + createdAt +
               '}';
    }
}
