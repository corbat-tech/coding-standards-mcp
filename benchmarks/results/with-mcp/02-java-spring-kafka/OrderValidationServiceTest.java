package com.orders.domain.service;

import com.orders.domain.event.OrderCreatedEvent;
import com.orders.domain.exception.OrderValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderValidationService")
class OrderValidationServiceTest {

    private OrderValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new OrderValidationService();
    }

    private OrderCreatedEvent createEvent(
            String orderId, String customerId, List<OrderCreatedEvent.OrderItem> items, BigDecimal total) {
        return new OrderCreatedEvent(orderId, customerId, items, total, Instant.now());
    }

    private OrderCreatedEvent.OrderItem createItem(Integer quantity, BigDecimal price) {
        return new OrderCreatedEvent.OrderItem("prod-1", "Product", quantity, price);
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("should_pass_when_valid_event")
        void should_pass_when_valid_event() {
            // Arrange
            OrderCreatedEvent event = createEvent(
                "order-1", "customer-1",
                List.of(createItem(2, new BigDecimal("25.00"))),
                new BigDecimal("50.00")
            );

            // Act & Assert
            assertThatCode(() -> validationService.validate(event))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should_throw_when_order_id_is_null")
        void should_throw_when_order_id_is_null() {
            // Arrange
            OrderCreatedEvent event = createEvent(
                null, "customer-1",
                List.of(createItem(1, new BigDecimal("10.00"))),
                new BigDecimal("10.00")
            );

            // Act & Assert
            assertThatThrownBy(() -> validationService.validate(event))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Order ID");
        }

        @Test
        @DisplayName("should_throw_when_customer_id_is_blank")
        void should_throw_when_customer_id_is_blank() {
            // Arrange
            OrderCreatedEvent event = createEvent(
                "order-1", "  ",
                List.of(createItem(1, new BigDecimal("10.00"))),
                new BigDecimal("10.00")
            );

            // Act & Assert
            assertThatThrownBy(() -> validationService.validate(event))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Customer ID");
        }

        @Test
        @DisplayName("should_throw_when_items_is_empty")
        void should_throw_when_items_is_empty() {
            // Arrange
            OrderCreatedEvent event = createEvent(
                "order-1", "customer-1",
                Collections.emptyList(),
                new BigDecimal("10.00")
            );

            // Act & Assert
            assertThatThrownBy(() -> validationService.validate(event))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("should_throw_when_item_quantity_is_zero")
        void should_throw_when_item_quantity_is_zero() {
            // Arrange
            OrderCreatedEvent event = createEvent(
                "order-1", "customer-1",
                List.of(createItem(0, new BigDecimal("10.00"))),
                new BigDecimal("10.00")
            );

            // Act & Assert
            assertThatThrownBy(() -> validationService.validate(event))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("quantity");
        }

        @Test
        @DisplayName("should_throw_when_total_exceeds_maximum")
        void should_throw_when_total_exceeds_maximum() {
            // Arrange
            OrderCreatedEvent event = createEvent(
                "order-1", "customer-1",
                List.of(createItem(1, new BigDecimal("100.00"))),
                new BigDecimal("15000.00")
            );

            // Act & Assert
            assertThatThrownBy(() -> validationService.validate(event))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("exceeds maximum");
        }
    }
}
