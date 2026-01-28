package com.orders.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
    String orderId,
    String customerId,
    List<OrderItem> items,
    BigDecimal totalAmount,
    Instant timestamp
) {
    public record OrderItem(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
    ) {}
}
