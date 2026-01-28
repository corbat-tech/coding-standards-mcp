package com.example.eventsourcing;

import java.math.BigDecimal;

public class ItemAddedEvent extends Event {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final BigDecimal price;

    public ItemAddedEvent(String orderId, String productId, String productName, int quantity, BigDecimal price, int version) {
        super(orderId, version);
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }

    @Override
    public String getEventType() {
        return "ItemAdded";
    }
}
