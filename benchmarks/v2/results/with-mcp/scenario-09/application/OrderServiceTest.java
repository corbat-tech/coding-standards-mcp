package com.example.order.application;

import com.example.order.domain.aggregate.Order;
import com.example.order.domain.aggregate.OrderStatus;
import com.example.order.infrastructure.InMemoryEventStore;
import com.example.order.projection.OrderProjection;
import com.example.order.projection.OrderProjector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {
    private InMemoryEventStore eventStore;
    private OrderProjector projector;
    private OrderService service;

    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneId.UTC);

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore();
        projector = new OrderProjector();
        service = new OrderService(eventStore, projector, FIXED_CLOCK);
    }

    @Test
    void should_create_order_when_valid_customer() {
        // Arrange
        String customerId = "customer-123";

        // Act
        Order order = service.createOrder(customerId);

        // Assert
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getVersion()).isEqualTo(1);
    }

    @Test
    void should_persist_event_when_order_created() {
        // Arrange & Act
        Order order = service.createOrder("customer-123");

        // Assert
        var events = eventStore.getEvents(order.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("OrderCreated");
    }

    @Test
    void should_update_projection_when_order_created() {
        // Arrange & Act
        Order order = service.createOrder("customer-123");

        // Assert
        Optional<OrderProjection> projection = service.getOrderProjection(order.getId());
        assertThat(projection).isPresent();
        assertThat(projection.get().status()).isEqualTo("CREATED");
    }

    @Test
    void should_add_item_when_order_exists() {
        // Arrange
        Order order = service.createOrder("customer-123");

        // Act
        Order updated = service.addItem(
            order.getId(), "prod-1", "Widget", 2, BigDecimal.valueOf(25)
        );

        // Assert
        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void should_persist_item_added_event() {
        // Arrange
        Order order = service.createOrder("customer-123");

        // Act
        service.addItem(order.getId(), "prod-1", "Widget", 2, BigDecimal.valueOf(25));

        // Assert
        var events = eventStore.getEvents(order.getId());
        assertThat(events).hasSize(2);
        assertThat(events.get(1).getEventType()).isEqualTo("ItemAdded");
    }

    @Test
    void should_ship_order_when_has_items() {
        // Arrange
        Order order = service.createOrder("customer-123");
        service.addItem(order.getId(), "prod-1", "Widget", 1, BigDecimal.TEN);

        // Act
        Order shipped = service.shipOrder(order.getId(), "TRACK-123", "FedEx");

        // Assert
        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(shipped.getTrackingNumber()).isEqualTo("TRACK-123");
    }

    @Test
    void should_throw_when_shipping_empty_order() {
        // Arrange
        Order order = service.createOrder("customer-123");

        // Act & Assert
        assertThatThrownBy(() ->
            service.shipOrder(order.getId(), "TRACK-123", "FedEx")
        ).isInstanceOf(IllegalStateException.class)
         .hasMessageContaining("empty order");
    }

    @Test
    void should_rehydrate_order_from_events() {
        // Arrange
        Order order = service.createOrder("customer-123");
        service.addItem(order.getId(), "prod-1", "Widget", 2, BigDecimal.valueOf(25));
        service.addItem(order.getId(), "prod-2", "Gadget", 1, BigDecimal.valueOf(100));

        // Create new service with same event store but fresh projector
        OrderProjector newProjector = new OrderProjector();
        OrderService newService = new OrderService(eventStore, newProjector, FIXED_CLOCK);

        // Rebuild projections from events
        var events = eventStore.getEvents(order.getId());
        events.forEach(newProjector::apply);

        // Act
        Optional<OrderProjection> projection = newService.getOrderProjection(order.getId());

        // Assert
        assertThat(projection).isPresent();
        assertThat(projection.get().items()).hasSize(2);
        assertThat(projection.get().totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    void should_throw_when_order_not_found() {
        // Act & Assert
        assertThatThrownBy(() ->
            service.addItem("non-existent", "prod-1", "Widget", 1, BigDecimal.TEN)
        ).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void should_maintain_version_across_events() {
        // Arrange
        Order order = service.createOrder("customer-123");
        service.addItem(order.getId(), "prod-1", "Widget", 1, BigDecimal.TEN);
        Order finalOrder = service.addItem(order.getId(), "prod-2", "Gadget", 1, BigDecimal.valueOf(20));

        // Assert
        assertThat(finalOrder.getVersion()).isEqualTo(3);
    }
}
