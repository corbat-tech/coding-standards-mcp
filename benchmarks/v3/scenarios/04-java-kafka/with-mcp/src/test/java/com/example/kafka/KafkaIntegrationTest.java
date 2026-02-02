package com.example.kafka;

import com.example.kafka.application.inventory.InventoryService;
import com.example.kafka.domain.event.OrderCreatedEvent;
import com.example.kafka.infrastructure.kafka.OrderEventKafkaConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaIntegrationTest {

    @Mock private InventoryService inventoryService;
    @Mock private Acknowledgment acknowledgment;

    @Test
    void shouldProcessOrderCreatedEvent() {
        OrderEventKafkaConsumer consumer = new OrderEventKafkaConsumer(inventoryService);

        OrderCreatedEvent event = new OrderCreatedEvent("order-1",
                List.of(new OrderCreatedEvent.OrderItem("P1", 2, new BigDecimal("25.00"))));

        consumer.handleOrderCreated(event, acknowledgment);

        verify(inventoryService).reserveStock(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeOnFailure() {
        OrderEventKafkaConsumer consumer = new OrderEventKafkaConsumer(inventoryService);
        doThrow(new RuntimeException("Test error")).when(inventoryService).reserveStock(any());

        OrderCreatedEvent event = new OrderCreatedEvent("order-1",
                List.of(new OrderCreatedEvent.OrderItem("P1", 2, new BigDecimal("25.00"))));

        try {
            consumer.handleOrderCreated(event, acknowledgment);
        } catch (RuntimeException ignored) {}

        verify(acknowledgment, never()).acknowledge();
    }
}
