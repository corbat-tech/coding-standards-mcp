package com.example.order.domain.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderCreatedEvent")
class OrderCreatedEventTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create event with valid data")
        void should_create_event_with_valid_data() {
            // Given
            List<OrderCreatedEvent.OrderItem> items = List.of(
                new OrderCreatedEvent.OrderItem("PROD-1", "Product 1", 2, new BigDecimal("10.00"))
            );

            // When
            OrderCreatedEvent event = OrderCreatedEvent.create(
                "ORD-001", "CUST-001", items, new BigDecimal("20.00")
            );

            // Then
            assertThat(event.eventId()).isNotNull();
            assertThat(event.orderId()).isEqualTo("ORD-001");
            assertThat(event.customerId()).isEqualTo("CUST-001");
            assertThat(event.items()).hasSize(1);
            assertThat(event.totalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(event.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject null eventId")
        void should_reject_null_event_id() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                null, "ORD-001", "CUST-001",
                List.of(createValidItem()),
                new BigDecimal("10.00"),
                Instant.now()
            )).isInstanceOf(NullPointerException.class)
              .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("should reject empty items list")
        void should_reject_empty_items() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                "EVT-001", "ORD-001", "CUST-001",
                Collections.emptyList(),
                new BigDecimal("10.00"),
                Instant.now()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("should reject non-positive total amount")
        void should_reject_non_positive_total() {
            assertThatThrownBy(() -> new OrderCreatedEvent(
                "EVT-001", "ORD-001", "CUST-001",
                List.of(createValidItem()),
                BigDecimal.ZERO,
                Instant.now()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should make defensive copy of items list")
        void should_make_defensive_copy() {
            // When
            OrderCreatedEvent event = OrderCreatedEvent.create(
                "ORD-001", "CUST-001",
                List.of(createValidItem()),
                new BigDecimal("10.00")
            );

            // Then - items list should be immutable
            assertThatThrownBy(() -> event.items().add(createValidItem()))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("OrderItem")
    class OrderItemTests {

        @Test
        @DisplayName("should create valid order item")
        void should_create_valid_item() {
            // When
            OrderCreatedEvent.OrderItem item = new OrderCreatedEvent.OrderItem(
                "PROD-1", "Product 1", 5, new BigDecimal("10.00")
            );

            // Then
            assertThat(item.productId()).isEqualTo("PROD-1");
            assertThat(item.productName()).isEqualTo("Product 1");
            assertThat(item.quantity()).isEqualTo(5);
            assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("should reject non-positive quantity")
        void should_reject_non_positive_quantity() {
            assertThatThrownBy(() -> new OrderCreatedEvent.OrderItem(
                "PROD-1", "Product 1", 0, new BigDecimal("10.00")
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("should reject non-positive unit price")
        void should_reject_non_positive_unit_price() {
            assertThatThrownBy(() -> new OrderCreatedEvent.OrderItem(
                "PROD-1", "Product 1", 5, new BigDecimal("-1.00")
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("positive");
        }
    }

    private OrderCreatedEvent.OrderItem createValidItem() {
        return new OrderCreatedEvent.OrderItem(
            "PROD-1", "Product 1", 1, new BigDecimal("10.00")
        );
    }
}
