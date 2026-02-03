package com.example.saga.domain.entity;

import com.example.saga.domain.valueobject.Money;
import com.example.saga.domain.valueobject.OrderId;
import java.util.List;

public class Order {
    private final OrderId id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Money totalAmount;
    private OrderStatus status;

    public Order(OrderId id, String customerId, List<OrderItem> items, Money totalAmount) {
        this.id = id;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
    }

    public OrderId getId() { return id; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public Money getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }

    public void confirm() { this.status = OrderStatus.CONFIRMED; }
    public void cancel() { this.status = OrderStatus.CANCELLED; }
    public void ship() { this.status = OrderStatus.SHIPPED; }

    public enum OrderStatus { PENDING, CONFIRMED, CANCELLED, SHIPPED }
}
