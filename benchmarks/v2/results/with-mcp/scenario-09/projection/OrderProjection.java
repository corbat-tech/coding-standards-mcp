package com.example.order.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderProjection(
    String orderId,
    String customerId,
    String status,
    List<OrderItemProjection> items,
    BigDecimal totalAmount,
    String trackingNumber,
    String carrier,
    Instant createdAt,
    Instant shippedAt
) {
    public record OrderItemProjection(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}
