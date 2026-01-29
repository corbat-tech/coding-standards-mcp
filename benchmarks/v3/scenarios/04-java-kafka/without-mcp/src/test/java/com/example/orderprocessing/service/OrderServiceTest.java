package com.example.orderprocessing.service;

import com.example.orderprocessing.domain.entity.Order;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.domain.repository.OrderRepository;
import com.example.orderprocessing.producer.OrderEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Captor
    private ArgumentCaptor<OrderCreatedEvent> eventCaptor;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderEventProducer);
    }

    @Test
    @DisplayName("Should create order and publish event")
    void shouldCreateOrderAndPublishEvent() {
        // Given
        String customerId = "CUST-001";
        List<OrderService.CreateOrderItemRequest> items = List.of(
                new OrderService.CreateOrderItemRequest("PROD-001", "Product 1", 2, new BigDecimal("25.00")),
                new OrderService.CreateOrderItemRequest("PROD-002", "Product 2", 1, new BigDecimal("50.00"))
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderEventProducer.publishOrderCreatedEvent(any())).thenReturn(CompletableFuture.completedFuture(null));

        // When
        Order result = orderService.createOrder(customerId, items);

        // Then
        verify(orderRepository).save(orderCaptor.capture());
        verify(orderEventProducer).publishOrderCreatedEvent(eventCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getCustomerId()).isEqualTo(customerId);
        assertThat(savedOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        assertThat(savedOrder.getLineItems()).hasSize(2);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        OrderCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(savedOrder.getId());
        assertThat(publishedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(publishedEvent.getItems()).hasSize(2);
        assertThat(publishedEvent.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should calculate total amount correctly")
    void shouldCalculateTotalAmountCorrectly() {
        // Given
        List<OrderService.CreateOrderItemRequest> items = List.of(
                new OrderService.CreateOrderItemRequest("PROD-001", "Product 1", 3, new BigDecimal("10.00")),
                new OrderService.CreateOrderItemRequest("PROD-002", "Product 2", 2, new BigDecimal("15.50"))
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderEventProducer.publishOrderCreatedEvent(any())).thenReturn(CompletableFuture.completedFuture(null));

        // When
        Order result = orderService.createOrder("CUST-001", items);

        // Then
        // 3 * 10.00 + 2 * 15.50 = 30.00 + 31.00 = 61.00
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("61.00"));
    }

    @Test
    @DisplayName("Should update order status")
    void shouldUpdateOrderStatus() {
        // Given
        String orderId = "ORDER-001";
        Order existingOrder = Order.builder()
                .id(orderId)
                .customerId("CUST-001")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Order result = orderService.updateOrderStatus(orderId, Order.OrderStatus.CONFIRMED);

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent order")
    void shouldThrowExceptionWhenUpdatingNonExistentOrder() {
        // Given
        String orderId = "NON-EXISTENT";
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, Order.OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Should cancel order")
    void shouldCancelOrder() {
        // Given
        String orderId = "ORDER-001";
        Order existingOrder = Order.builder()
                .id(orderId)
                .customerId("CUST-001")
                .status(Order.OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Order result = orderService.cancelOrder(orderId);

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Should get order by ID")
    void shouldGetOrderById() {
        // Given
        String orderId = "ORDER-001";
        Order order = Order.builder()
                .id(orderId)
                .customerId("CUST-001")
                .status(Order.OrderStatus.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        Optional<Order> result = orderService.getOrder(orderId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Should get orders by customer")
    void shouldGetOrdersByCustomer() {
        // Given
        String customerId = "CUST-001";
        List<Order> orders = List.of(
                Order.builder().id("ORDER-001").customerId(customerId).build(),
                Order.builder().id("ORDER-002").customerId(customerId).build()
        );

        when(orderRepository.findByCustomerId(customerId)).thenReturn(orders);

        // When
        List<Order> result = orderService.getOrdersByCustomer(customerId);

        // Then
        assertThat(result).hasSize(2);
    }
}
