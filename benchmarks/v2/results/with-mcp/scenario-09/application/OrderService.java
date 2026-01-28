package com.example.order.application;

import com.example.order.domain.aggregate.Order;
import com.example.order.domain.event.DomainEvent;
import com.example.order.infrastructure.EventStore;
import com.example.order.projection.OrderProjection;
import com.example.order.projection.OrderProjector;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OrderService {
    private final EventStore eventStore;
    private final OrderProjector projector;
    private final Clock clock;

    public OrderService(EventStore eventStore, OrderProjector projector, Clock clock) {
        this.eventStore = eventStore;
        this.projector = projector;
        this.clock = clock;
    }

    public Order createOrder(String customerId) {
        String orderId = UUID.randomUUID().toString();
        Order order = Order.create(orderId, customerId, clock.instant());

        saveAndProject(order);
        return order;
    }

    public Order addItem(String orderId, String productId, String productName,
                         int quantity, BigDecimal unitPrice) {
        Order order = loadOrder(orderId);
        order.addItem(productId, productName, quantity, unitPrice, clock.instant());

        saveAndProject(order);
        return order;
    }

    public Order shipOrder(String orderId, String trackingNumber, String carrier) {
        Order order = loadOrder(orderId);
        order.ship(trackingNumber, carrier, clock.instant());

        saveAndProject(order);
        return order;
    }

    public Optional<OrderProjection> getOrderProjection(String orderId) {
        return projector.getProjection(orderId);
    }

    public List<OrderProjection> getAllOrders() {
        return projector.getAllProjections();
    }

    private Order loadOrder(String orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId);
        if (events.isEmpty()) {
            throw new OrderNotFoundException(orderId);
        }

        Order order = new Order();
        order.rehydrate(events);
        return order;
    }

    private void saveAndProject(Order order) {
        List<DomainEvent> uncommitted = order.getUncommittedEvents();
        int expectedVersion = order.getVersion() - uncommitted.size();

        eventStore.append(order.getId(), uncommitted, expectedVersion);

        for (DomainEvent event : uncommitted) {
            projector.apply(event);
        }

        order.clearUncommittedEvents();
    }
}
