package com.example.orderprocessing.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published when a new order is created.
 * Contains all necessary information for downstream services to process the order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {

    /**
     * Unique identifier for this event - used for idempotency
     */
    private String eventId;

    /**
     * The order identifier
     */
    private String orderId;

    /**
     * Customer who placed the order
     */
    private String customerId;

    /**
     * Items included in the order
     */
    private List<OrderItem> items;

    /**
     * Total order amount
     */
    private BigDecimal totalAmount;

    /**
     * Timestamp when the order was created
     */
    private Instant createdAt;

    /**
     * Represents a single item in the order
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
    }

    /**
     * Factory method to create a new OrderCreatedEvent with auto-generated eventId
     */
    public static OrderCreatedEvent create(String orderId, String customerId,
                                           List<OrderItem> items, BigDecimal totalAmount) {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .customerId(customerId)
                .items(items)
                .totalAmount(totalAmount)
                .createdAt(Instant.now())
                .build();
    }
}
