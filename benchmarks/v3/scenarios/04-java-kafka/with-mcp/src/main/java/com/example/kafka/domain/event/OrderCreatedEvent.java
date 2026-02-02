package com.example.kafka.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        String orderId,
        List<OrderItem> items,
        Instant occurredAt
) {
    public OrderCreatedEvent(String orderId, List<OrderItem> items) {
        this(UUID.randomUUID().toString(), orderId, items, Instant.now());
    }

    public record OrderItem(String productId, int quantity, BigDecimal price) {}
}
