package com.example.order.projection;

import com.example.order.domain.event.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderProjector {
    private final Map<String, OrderProjectionState> projections = new ConcurrentHashMap<>();

    public void apply(DomainEvent event) {
        if (event instanceof OrderCreatedEvent e) {
            handleOrderCreated(e);
        } else if (event instanceof ItemAddedEvent e) {
            handleItemAdded(e);
        } else if (event instanceof OrderShippedEvent e) {
            handleOrderShipped(e);
        }
    }

    public Optional<OrderProjection> getProjection(String orderId) {
        OrderProjectionState state = projections.get(orderId);
        return state != null ? Optional.of(state.toProjection()) : Optional.empty();
    }

    public List<OrderProjection> getAllProjections() {
        return projections.values().stream()
            .map(OrderProjectionState::toProjection)
            .toList();
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        projections.put(event.getAggregateId(), new OrderProjectionState(
            event.getAggregateId(),
            event.getCustomerId(),
            "CREATED",
            event.getOccurredAt()
        ));
    }

    private void handleItemAdded(ItemAddedEvent event) {
        OrderProjectionState state = projections.get(event.getAggregateId());
        if (state != null) {
            state.addItem(new OrderProjection.OrderItemProjection(
                event.getProductId(),
                event.getProductName(),
                event.getQuantity(),
                event.getUnitPrice(),
                event.getUnitPrice().multiply(BigDecimal.valueOf(event.getQuantity()))
            ));
        }
    }

    private void handleOrderShipped(OrderShippedEvent event) {
        OrderProjectionState state = projections.get(event.getAggregateId());
        if (state != null) {
            state.markShipped(event.getTrackingNumber(), event.getCarrier(), event.getOccurredAt());
        }
    }

    public void clear() {
        projections.clear();
    }

    private static class OrderProjectionState {
        private final String orderId;
        private final String customerId;
        private String status;
        private final List<OrderProjection.OrderItemProjection> items = new ArrayList<>();
        private String trackingNumber;
        private String carrier;
        private final Instant createdAt;
        private Instant shippedAt;

        OrderProjectionState(String orderId, String customerId, String status, Instant createdAt) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.status = status;
            this.createdAt = createdAt;
        }

        void addItem(OrderProjection.OrderItemProjection item) {
            items.add(item);
        }

        void markShipped(String trackingNumber, String carrier, Instant shippedAt) {
            this.status = "SHIPPED";
            this.trackingNumber = trackingNumber;
            this.carrier = carrier;
            this.shippedAt = shippedAt;
        }

        OrderProjection toProjection() {
            BigDecimal total = items.stream()
                .map(OrderProjection.OrderItemProjection::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new OrderProjection(
                orderId, customerId, status,
                Collections.unmodifiableList(items),
                total, trackingNumber, carrier,
                createdAt, shippedAt
            );
        }
    }
}
