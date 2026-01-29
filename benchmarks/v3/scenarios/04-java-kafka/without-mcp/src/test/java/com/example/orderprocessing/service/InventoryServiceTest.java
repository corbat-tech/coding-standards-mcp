package com.example.orderprocessing.service;

import com.example.orderprocessing.domain.entity.InventoryItem;
import com.example.orderprocessing.domain.entity.ProcessedEvent;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.domain.repository.InventoryRepository;
import com.example.orderprocessing.domain.repository.ProcessedEventRepository;
import com.example.orderprocessing.exception.InsufficientStockException;
import com.example.orderprocessing.exception.ProductNotFoundException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Captor
    private ArgumentCaptor<InventoryItem> inventoryCaptor;

    @Captor
    private ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, processedEventRepository);
    }

    @Test
    @DisplayName("Should process OrderCreatedEvent and reserve stock")
    void shouldProcessEventAndReserveStock() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String productId = "PROD-001";

        InventoryItem inventoryItem = InventoryItem.builder()
                .productId(productId)
                .productName("Test Product")
                .quantityAvailable(100)
                .quantityReserved(0)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(productId)
                                .productName("Test Product")
                                .quantity(10)
                                .unitPrice(new BigDecimal("25.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("250.00"))
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inventoryRepository.findByIdWithLock(productId)).thenReturn(Optional.of(inventoryItem));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = inventoryService.processOrderCreatedEvent(event);

        // Then
        assertThat(result).isTrue();

        verify(inventoryRepository).save(inventoryCaptor.capture());
        InventoryItem savedItem = inventoryCaptor.getValue();
        assertThat(savedItem.getQuantityReserved()).isEqualTo(10);
        assertThat(savedItem.getEffectiveAvailable()).isEqualTo(90);

        verify(processedEventRepository).save(processedEventCaptor.capture());
        ProcessedEvent savedEvent = processedEventCaptor.getValue();
        assertThat(savedEvent.getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("Should skip duplicate event (idempotency)")
    void shouldSkipDuplicateEvent() {
        // Given
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of())
                .totalAmount(BigDecimal.ZERO)
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        boolean result = inventoryService.processOrderCreatedEvent(event);

        // Then
        assertThat(result).isFalse();
        verify(inventoryRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product does not exist")
    void shouldThrowProductNotFoundException() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String nonExistentProductId = "NON-EXISTENT";

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(nonExistentProductId)
                                .productName("Non-existent Product")
                                .quantity(5)
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("50.00"))
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inventoryRepository.findByIdWithLock(nonExistentProductId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> inventoryService.processOrderCreatedEvent(event))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(nonExistentProductId);
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock is insufficient")
    void shouldThrowInsufficientStockException() {
        // Given
        String eventId = UUID.randomUUID().toString();
        String productId = "PROD-001";

        InventoryItem inventoryItem = InventoryItem.builder()
                .productId(productId)
                .productName("Limited Stock Product")
                .quantityAvailable(5)
                .quantityReserved(0)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId(productId)
                                .productName("Limited Stock Product")
                                .quantity(10) // Requesting more than available
                                .unitPrice(new BigDecimal("10.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inventoryRepository.findByIdWithLock(productId)).thenReturn(Optional.of(inventoryItem));

        // When/Then
        assertThatThrownBy(() -> inventoryService.processOrderCreatedEvent(event))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining(productId)
                .hasMessageContaining("Requested: 10")
                .hasMessageContaining("Available: 5");
    }

    @Test
    @DisplayName("Should process multiple items in a single event")
    void shouldProcessMultipleItems() {
        // Given
        String eventId = UUID.randomUUID().toString();

        InventoryItem itemA = InventoryItem.builder()
                .productId("PROD-A")
                .productName("Product A")
                .quantityAvailable(50)
                .quantityReserved(0)
                .build();

        InventoryItem itemB = InventoryItem.builder()
                .productId("PROD-B")
                .productName("Product B")
                .quantityAvailable(30)
                .quantityReserved(0)
                .build();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(UUID.randomUUID().toString())
                .customerId("CUST-001")
                .items(List.of(
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-A")
                                .productName("Product A")
                                .quantity(5)
                                .unitPrice(new BigDecimal("10.00"))
                                .build(),
                        OrderCreatedEvent.OrderItem.builder()
                                .productId("PROD-B")
                                .productName("Product B")
                                .quantity(3)
                                .unitPrice(new BigDecimal("20.00"))
                                .build()
                ))
                .totalAmount(new BigDecimal("110.00"))
                .build();

        when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(inventoryRepository.findByIdWithLock("PROD-A")).thenReturn(Optional.of(itemA));
        when(inventoryRepository.findByIdWithLock("PROD-B")).thenReturn(Optional.of(itemB));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processedEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        boolean result = inventoryService.processOrderCreatedEvent(event);

        // Then
        assertThat(result).isTrue();
        verify(inventoryRepository, times(2)).save(any());

        assertThat(itemA.getQuantityReserved()).isEqualTo(5);
        assertThat(itemB.getQuantityReserved()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should add new inventory item")
    void shouldAddNewInventoryItem() {
        // Given
        String productId = "NEW-PROD";
        String productName = "New Product";
        int quantity = 100;

        when(inventoryRepository.findById(productId)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        InventoryItem result = inventoryService.addOrUpdateInventory(productId, productName, quantity);

        // Then
        verify(inventoryRepository).save(inventoryCaptor.capture());
        InventoryItem saved = inventoryCaptor.getValue();
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getProductName()).isEqualTo(productName);
        assertThat(saved.getQuantityAvailable()).isEqualTo(quantity);
        assertThat(saved.getQuantityReserved()).isZero();
    }

    @Test
    @DisplayName("Should update existing inventory item")
    void shouldUpdateExistingInventoryItem() {
        // Given
        String productId = "EXISTING-PROD";
        InventoryItem existing = InventoryItem.builder()
                .productId(productId)
                .productName("Old Name")
                .quantityAvailable(50)
                .quantityReserved(10)
                .build();

        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        InventoryItem result = inventoryService.addOrUpdateInventory(productId, "New Name", 30);

        // Then
        verify(inventoryRepository).save(inventoryCaptor.capture());
        InventoryItem saved = inventoryCaptor.getValue();
        assertThat(saved.getProductName()).isEqualTo("New Name");
        assertThat(saved.getQuantityAvailable()).isEqualTo(80); // 50 + 30
        assertThat(saved.getQuantityReserved()).isEqualTo(10); // Unchanged
    }

    @Test
    @DisplayName("Should check if event is processed")
    void shouldCheckIfEventIsProcessed() {
        // Given
        String eventId = UUID.randomUUID().toString();
        when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        boolean result = inventoryService.isEventProcessed(eventId);

        // Then
        assertThat(result).isTrue();
    }
}
