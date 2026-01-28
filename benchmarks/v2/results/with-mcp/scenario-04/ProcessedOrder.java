package com.example.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ProcessedOrder(
    String id,
    String orderId,
    String customerId,
    BigDecimal totalAmount,
    ProcessingStatus status,
    String failureReason,
    Instant processedAt
) {
    public static ProcessedOrder success(
        String id,
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        Instant processedAt
    ) {
        return new ProcessedOrder(
            id, orderId, customerId, totalAmount,
            ProcessingStatus.SUCCESS, null, processedAt
        );
    }

    public static ProcessedOrder failure(
        String id,
        String orderId,
        String customerId,
        BigDecimal totalAmount,
        String failureReason,
        Instant processedAt
    ) {
        return new ProcessedOrder(
            id, orderId, customerId, totalAmount,
            ProcessingStatus.FAILED, failureReason, processedAt
        );
    }
}
