package com.example.order.domain.entity;

import com.example.order.domain.event.*;
import com.example.order.domain.exception.*;
import com.example.order.domain.valueobject.*;

import java.util.*;

public class Order {

    private final OrderId id;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final List<DomainEvent> domainEvents;

    private Order(OrderId id) {
        this.id = id;
        this.items = new ArrayList<>();
        this.status = OrderStatus.DRAFT;
        this.domainEvents = new ArrayList<>();
    }

    public static Order create() {
        OrderId id = OrderId.generate();
        Order order = new Order(id);
        order.domainEvents.add(new OrderCreatedEvent(id));
        return order;
    }

    public void addItem(String productId, String productName, Money unitPrice, Quantity quantity) {
        if (!status.canAddItems()) {
            throw new InvalidOrderStateException("add items", status);
        }
        items.add(new OrderItem(productId, productName, unitPrice, quantity));
    }

    public void confirm() {
        if (!status.canConfirm()) {
            throw new InvalidOrderStateException("confirm", status);
        }
        Money total = calculateTotal();
        if (total.isLessThanMinimumOrder()) {
            throw new MinimumOrderValueException(total);
        }
        this.status = OrderStatus.CONFIRMED;
        domainEvents.add(new OrderConfirmedEvent(id, total));
    }

    public void ship() {
        if (!status.canShip()) {
            throw new InvalidOrderStateException("ship", status);
        }
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        if (!status.canDeliver()) {
            throw new InvalidOrderStateException("deliver", status);
        }
        this.status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (!status.canCancel()) {
            throw new InvalidOrderStateException("cancel", status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public Money calculateTotal() {
        return items.stream()
                .map(OrderItem::calculateTotal)
                .reduce(Money.ZERO, Money::add);
    }

    public OrderId getId() { return id; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public OrderStatus getStatus() { return status; }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
