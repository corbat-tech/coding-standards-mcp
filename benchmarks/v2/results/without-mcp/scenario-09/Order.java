package com.example.eventsourcing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private String orderId;
    private String customerId;
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.CREATED;
    private String shippingAddress;
    private String trackingNumber;
    private int version = 0;

    public enum OrderStatus {
        CREATED,
        SHIPPED
    }

    public static class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private BigDecimal price;

        public OrderItem(String productId, String productName, int quantity, BigDecimal price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public BigDecimal getPrice() { return price; }
    }

    public static Order reconstruct(List<Event> events) {
        Order order = new Order();
        for (Event event : events) {
            order.apply(event);
        }
        return order;
    }

    public void apply(Event event) {
        if (event instanceof OrderCreatedEvent e) {
            this.orderId = e.getAggregateId();
            this.customerId = e.getCustomerId();
            this.status = OrderStatus.CREATED;
        } else if (event instanceof ItemAddedEvent e) {
            this.items.add(new OrderItem(
                e.getProductId(),
                e.getProductName(),
                e.getQuantity(),
                e.getPrice()
            ));
        } else if (event instanceof OrderShippedEvent e) {
            this.status = OrderStatus.SHIPPED;
            this.shippingAddress = e.getShippingAddress();
            this.trackingNumber = e.getTrackingNumber();
        }
        this.version = event.getVersion();
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public String getShippingAddress() { return shippingAddress; }
    public String getTrackingNumber() { return trackingNumber; }
    public int getVersion() { return version; }

    public BigDecimal getTotalAmount() {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
