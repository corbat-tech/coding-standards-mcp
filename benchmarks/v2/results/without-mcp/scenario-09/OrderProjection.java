package com.example.eventsourcing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderProjection {
    private final Map<String, OrderView> orders = new HashMap<>();

    public static class OrderView {
        private final String orderId;
        private final String customerId;
        private int itemCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private String status;
        private String trackingNumber;

        public OrderView(String orderId, String customerId) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.status = "CREATED";
        }

        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public int getItemCount() { return itemCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getStatus() { return status; }
        public String getTrackingNumber() { return trackingNumber; }

        void addItem(int quantity, BigDecimal price) {
            this.itemCount += quantity;
            this.totalAmount = this.totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));
        }

        void markShipped(String trackingNumber) {
            this.status = "SHIPPED";
            this.trackingNumber = trackingNumber;
        }
    }

    public void apply(Event event) {
        if (event instanceof OrderCreatedEvent e) {
            orders.put(e.getAggregateId(), new OrderView(e.getAggregateId(), e.getCustomerId()));
        } else if (event instanceof ItemAddedEvent e) {
            OrderView view = orders.get(e.getAggregateId());
            if (view != null) {
                view.addItem(e.getQuantity(), e.getPrice());
            }
        } else if (event instanceof OrderShippedEvent e) {
            OrderView view = orders.get(e.getAggregateId());
            if (view != null) {
                view.markShipped(e.getTrackingNumber());
            }
        }
    }

    public Optional<OrderView> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public List<OrderView> getAllOrders() {
        return List.copyOf(orders.values());
    }

    public List<OrderView> getOrdersByStatus(String status) {
        return orders.values().stream()
            .filter(o -> o.getStatus().equals(status))
            .toList();
    }
}
