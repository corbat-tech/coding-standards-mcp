package com.example.orderprocessing.consumer;

import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.exception.InsufficientStockException;
import com.example.orderprocessing.exception.ProductNotFoundException;
import com.example.orderprocessing.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryEventConsumerTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private Acknowledgment acknowledgment;

    private InventoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new InventoryEventConsumer(inventoryService);
    }

    @Test
    @DisplayName("Should process event and acknowledge when successful")
    void shouldProcessEventAndAcknowledge() {
        // Given
        OrderCreatedEvent event = createTestEvent();
        ConsumerRecord<String, OrderCreatedEvent> record = createConsumerRecord(event);

        when(inventoryService.processOrderCreatedEvent(event)).thenReturn(true);

        // When
        consumer.consumeOrderCreatedEvent(record, acknowledgment);

        // Then
        verify(inventoryService).processOrderCreatedEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should acknowledge duplicate events")
    void shouldAcknowledgeDuplicateEvents() {
        // Given
        OrderCreatedEvent event = createTestEvent();
        ConsumerRecord<String, OrderCreatedEvent> record = createConsumerRecord(event);

        when(inventoryService.processOrderCreatedEvent(event)).thenReturn(false); // Duplicate

        // When
        consumer.consumeOrderCreatedEvent(record, acknowledgment);

        // Then
        verify(inventoryService).processOrderCreatedEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException for missing product")
    void shouldThrowProductNotFoundExceptionForMissingProduct() {
        // Given
        OrderCreatedEvent event = createTestEvent();
        ConsumerRecord<String, OrderCreatedEvent> record = createConsumerRecord(event);

        when(inventoryService.processOrderCreatedEvent(event))
                .thenThrow(new ProductNotFoundException("PROD-001"));

        // When/Then
        assertThatThrownBy(() -> consumer.consumeOrderCreatedEvent(record, acknowledgment))
                .isInstanceOf(ProductNotFoundException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should throw InsufficientStockException for insufficient stock")
    void shouldThrowInsufficientStockExceptionForInsufficientStock() {
        // Given
        OrderCreatedEvent event = createTestEvent();
        ConsumerRecord<String, OrderCreatedEvent> record = createConsumerRecord(event);

        when(inventoryService.processOrderCreatedEvent(event))
                .thenThrow(new InsufficientStockException("PROD-001", 10, 5));

        // When/Then
        assertThatThrownBy(() -> consumer.consumeOrderCreatedEvent(record, acknowledgment))
                .isInstanceOf(InsufficientStockException.class);

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("Should throw exception for unexpected errors")
    void shouldThrowExceptionForUnexpectedErrors() {
        // Given
        OrderCreatedEvent event = createTestEvent();
        ConsumerRecord<String, OrderCreatedEvent> record = createConsumerRecord(event);

        when(inventoryService.processOrderCreatedEvent(event))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then
        assertThatThrownBy(() -> consumer.consumeOrderCreatedEvent(record, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

        verify(acknowledgment, never()).acknowledge();
    }

    private OrderCreatedEvent createTestEvent() {
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-001")
                                .productName("Test Product")
                                .quantity(5)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("50.00"))
                .createdAt(Instant.now())
                .build();
    }

    private ConsumerRecord<String, OrderCreatedEvent> createConsumerRecord(OrderCreatedEvent event) {
        return new ConsumerRecord<>(
                "order-created",
                0,
                0L,
                event.getOrderId(),
                event
        );
    }
}
