package com.example.order.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event representing an order creation.
 * Immutable value object following DDD principles.
 */
public record OrderCreatedEvent(
    String eventId,
    String orderId,
    String customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    Instant occurredAt
) {

    public OrderCreatedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
    }

    public static OrderCreatedEvent create(
            String orderId,
            String customerId,
            List<OrderItem> items,
            BigDecimal totalAmount) {
        return new OrderCreatedEvent(
            UUID.randomUUID().toString(),
            orderId,
            customerId,
            List.copyOf(items),
            totalAmount,
            Instant.now()
        );
    }

    /**
     * Nested record for order line items.
     */
    public record OrderItem(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
    ) {
        public OrderItem {
            Objects.requireNonNull(productId, "productId must not be null");
            Objects.requireNonNull(productName, "productName must not be null");
            Objects.requireNonNull(unitPrice, "unitPrice must not be null");

            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Unit price must be positive");
            }
        }
    }
}
