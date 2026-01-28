package com.example.order.domain.aggregate;

import com.example.order.domain.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private String id;
    private String customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private String trackingNumber;
    private String carrier;
    private int version;

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    public Order() {
        this.items = new ArrayList<>();
        this.version = 0;
    }

    public static Order create(String orderId, String customerId, Instant now) {
        Order order = new Order();
        order.apply(new OrderCreatedEvent(orderId, customerId, 1, now));
        return order;
    }

    public void addItem(String productId, String productName, int quantity, BigDecimal unitPrice, Instant now) {
        if (status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot add items to shipped order");
        }
        apply(new ItemAddedEvent(
            id, productId, productName, quantity, unitPrice, version + 1, now
        ));
    }

    public void ship(String trackingNumber, String carrier, Instant now) {
        if (status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order already shipped");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot ship empty order");
        }
        apply(new OrderShippedEvent(id, trackingNumber, carrier, version + 1, now));
    }

    public void apply(DomainEvent event) {
        when(event);
        version = event.getVersion();
        uncommittedEvents.add(event);
    }

    public void rehydrate(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            when(event);
            version = event.getVersion();
        }
    }

    private void when(DomainEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            this.id = e.getAggregateId();
            this.customerId = e.getCustomerId();
            this.status = OrderStatus.CREATED;
        } else if (event instanceof ItemAddedEvent e) {
            this.items.add(new OrderItem(
                e.getProductId(),
                e.getProductName(),
                e.getQuantity(),
                e.getUnitPrice()
            ));
        } else if (event instanceof OrderShippedEvent e) {
            this.status = OrderStatus.SHIPPED;
            this.trackingNumber = e.getTrackingNumber();
            this.carrier = e.getCarrier();
        }
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public int getVersion() {
        return version;
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
