package com.example.order.application.service;

import com.example.order.application.port.in.PlaceOrderUseCase.OrderItemCommand;
import com.example.order.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.example.order.application.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.example.order.application.port.out.OrderEventPublisher;
import com.example.order.domain.events.OrderCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(eventPublisher);
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("should publish OrderCreatedEvent when order is placed successfully")
        void should_publish_order_created_event_when_order_placed() {
            // Given
            PlaceOrderCommand command = createValidCommand();
            doNothing().when(eventPublisher).publishOrderCreatedSync(any());

            // When
            PlaceOrderResult result = orderService.placeOrder(command);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.orderId()).startsWith("ORD-");

            ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishOrderCreatedSync(eventCaptor.capture());

            OrderCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.customerId()).isEqualTo("CUST-001");
            assertThat(publishedEvent.items()).hasSize(1);
            assertThat(publishedEvent.totalAmount())
                .isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("should return failure when event publishing fails")
        void should_return_failure_when_event_publishing_fails() {
            // Given
            PlaceOrderCommand command = createValidCommand();
            doThrow(new RuntimeException("Kafka unavailable"))
                .when(eventPublisher).publishOrderCreatedSync(any());

            // When
            PlaceOrderResult result = orderService.placeOrder(command);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.orderId()).isNull();
            assertThat(result.message()).contains("Kafka unavailable");
        }

        @Test
        @DisplayName("should calculate total amount correctly for multiple items")
        void should_calculate_total_amount_correctly() {
            // Given
            PlaceOrderCommand command = new PlaceOrderCommand(
                "CUST-001",
                List.of(
                    new OrderItemCommand("PROD-1", "Product 1", 2, new BigDecimal("10.00")),
                    new OrderItemCommand("PROD-2", "Product 2", 3, new BigDecimal("20.00"))
                )
            );
            doNothing().when(eventPublisher).publishOrderCreatedSync(any());

            // When
            orderService.placeOrder(command);

            // Then
            ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(eventPublisher).publishOrderCreatedSync(eventCaptor.capture());

            // Total = (2 * 10.00) + (3 * 20.00) = 20.00 + 60.00 = 80.00
            assertThat(eventCaptor.getValue().totalAmount())
                .isEqualByComparingTo(new BigDecimal("80.00"));
        }

        private PlaceOrderCommand createValidCommand() {
            return new PlaceOrderCommand(
                "CUST-001",
                List.of(new OrderItemCommand(
                    "PROD-001",
                    "Test Product",
                    5,
                    new BigDecimal("10.00")
                ))
            );
        }
    }
}
