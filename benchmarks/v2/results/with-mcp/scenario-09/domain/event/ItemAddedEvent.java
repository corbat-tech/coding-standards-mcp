package com.example.order.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public class ItemAddedEvent extends DomainEvent {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal unitPrice;

    public ItemAddedEvent(
        String orderId,
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        int version,
        Instant occurredAt
    ) {
        super(orderId, version, occurredAt);
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Override
    public String getEventType() {
        return "ItemAdded";
    }
}
