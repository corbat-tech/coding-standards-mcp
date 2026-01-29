package com.example.order.application.service;

import com.example.order.application.port.in.ProcessOrderEventUseCase.ProcessingResult;
import com.example.order.application.port.out.InventoryRepository;
import com.example.order.application.port.out.ProcessedEventRepository;
import com.example.order.domain.events.OrderCreatedEvent;
import com.example.order.domain.exception.InsufficientStockException;
import com.example.order.domain.model.InventoryItem;
import com.example.order.domain.model.ProcessedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService")
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, processedEventRepository);
    }

    @Nested
    @DisplayName("processOrderCreated")
    class ProcessOrderCreated {

        @Test
        @DisplayName("should update inventory when order event is received")
        void should_update_inventory_when_order_event_received() {
            // Given
            OrderCreatedEvent event = createOrderEvent("event-1", "PROD-001", 5);
            InventoryItem inventory = new InventoryItem("PROD-001", "Test Product", 100);

            when(processedEventRepository.existsByEventId("event-1")).thenReturn(false);
            when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            ProcessingResult result = inventoryService.processOrderCreated(event);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.skipped()).isFalse();

            verify(inventoryRepository).save(inventory);
            assertThat(inventory.getAvailableQuantity()).isEqualTo(95);
            assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("should skip duplicate events for idempotency")
        void should_skip_duplicate_events_idempotency() {
            // Given
            OrderCreatedEvent event = createOrderEvent("event-1", "PROD-001", 5);
            when(processedEventRepository.existsByEventId("event-1")).thenReturn(true);

            // When
            ProcessingResult result = inventoryService.processOrderCreated(event);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.skipped()).isTrue();
            assertThat(result.message()).contains("idempotent");

            verify(inventoryRepository, never()).findByProductId(any());
            verify(inventoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return failure when stock is insufficient")
        void should_handle_insufficient_stock_gracefully() {
            // Given
            OrderCreatedEvent event = createOrderEvent("event-1", "PROD-001", 150);
            InventoryItem inventory = new InventoryItem("PROD-001", "Test Product", 100);

            when(processedEventRepository.existsByEventId("event-1")).thenReturn(false);
            when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventory));
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            ProcessingResult result = inventoryService.processOrderCreated(event);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Insufficient stock");

            // Verify failure was recorded
            ArgumentCaptor<ProcessedEvent> captor =
                ArgumentCaptor.forClass(ProcessedEvent.class);
            verify(processedEventRepository).save(captor.capture());
            assertThat(captor.getValue().success()).isFalse();
        }

        @Test
        @DisplayName("should throw exception when product not found")
        void should_throw_when_product_not_found() {
            // Given
            OrderCreatedEvent event = createOrderEvent("event-1", "UNKNOWN", 5);

            when(processedEventRepository.existsByEventId("event-1")).thenReturn(false);
            when(inventoryRepository.findByProductId("UNKNOWN")).thenReturn(Optional.empty());
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            assertThatThrownBy(() -> inventoryService.processOrderCreated(event))
                .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("should reserve stock for multiple items")
        void should_reserve_stock_for_multiple_items() {
            // Given
            OrderCreatedEvent event = createMultiItemOrderEvent("event-1");
            InventoryItem item1 = new InventoryItem("PROD-001", "Product 1", 100);
            InventoryItem item2 = new InventoryItem("PROD-002", "Product 2", 50);

            when(processedEventRepository.existsByEventId("event-1")).thenReturn(false);
            when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(item1));
            when(inventoryRepository.findByProductId("PROD-002"))
                .thenReturn(Optional.of(item2));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            ProcessingResult result = inventoryService.processOrderCreated(event);

            // Then
            assertThat(result.success()).isTrue();
            verify(inventoryRepository, times(2)).save(any());
            assertThat(item1.getReservedQuantity()).isEqualTo(10);
            assertThat(item2.getReservedQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("should record successful processing")
        void should_record_successful_processing() {
            // Given
            OrderCreatedEvent event = createOrderEvent("event-1", "PROD-001", 5);
            InventoryItem inventory = new InventoryItem("PROD-001", "Test Product", 100);

            when(processedEventRepository.existsByEventId("event-1")).thenReturn(false);
            when(inventoryRepository.findByProductId("PROD-001"))
                .thenReturn(Optional.of(inventory));
            when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            inventoryService.processOrderCreated(event);

            // Then
            ArgumentCaptor<ProcessedEvent> captor =
                ArgumentCaptor.forClass(ProcessedEvent.class);
            verify(processedEventRepository).save(captor.capture());

            ProcessedEvent recorded = captor.getValue();
            assertThat(recorded.eventId()).isEqualTo("event-1");
            assertThat(recorded.success()).isTrue();
        }

        private OrderCreatedEvent createOrderEvent(
                String eventId, String productId, int quantity) {
            return new OrderCreatedEvent(
                eventId,
                "ORD-001",
                "CUST-001",
                List.of(new OrderCreatedEvent.OrderItem(
                    productId, "Test Product", quantity, new BigDecimal("10.00")
                )),
                new BigDecimal("100.00"),
                Instant.now()
            );
        }

        private OrderCreatedEvent createMultiItemOrderEvent(String eventId) {
            return new OrderCreatedEvent(
                eventId,
                "ORD-001",
                "CUST-001",
                List.of(
                    new OrderCreatedEvent.OrderItem(
                        "PROD-001", "Product 1", 10, new BigDecimal("10.00")),
                    new OrderCreatedEvent.OrderItem(
                        "PROD-002", "Product 2", 5, new BigDecimal("20.00"))
                ),
                new BigDecimal("200.00"),
                Instant.now()
            );
        }
    }
}
