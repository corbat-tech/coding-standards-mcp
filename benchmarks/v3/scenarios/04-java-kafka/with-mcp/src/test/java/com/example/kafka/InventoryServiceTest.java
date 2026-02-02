package com.example.kafka;

import com.example.kafka.application.inventory.InventoryServiceImpl;
import com.example.kafka.application.inventory.InsufficientStockException;
import com.example.kafka.domain.event.OrderCreatedEvent;
import com.example.kafka.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProcessedEventRepository processedEventRepository;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository, processedEventRepository);
    }

    @Test
    void shouldReserveStockForNewOrder() {
        InventoryEntity inventory = createInventory("P1", 100, 0);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId("P1")).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);

        OrderCreatedEvent event = new OrderCreatedEvent("order-1",
                List.of(new OrderCreatedEvent.OrderItem("P1", 5, new BigDecimal("10.00"))));

        inventoryService.reserveStock(event);

        assertThat(inventory.getAvailableQuantity()).isEqualTo(95);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        verify(processedEventRepository).save(any());
    }

    @Test
    void shouldSkipAlreadyProcessedEvent() {
        when(processedEventRepository.existsByEventId(any())).thenReturn(true);

        OrderCreatedEvent event = new OrderCreatedEvent("order-1",
                List.of(new OrderCreatedEvent.OrderItem("P1", 5, new BigDecimal("10.00"))));

        inventoryService.reserveStock(event);

        verify(inventoryRepository, never()).findByProductId(any());
    }

    @Test
    void shouldThrowWhenInsufficientStock() {
        InventoryEntity inventory = createInventory("P1", 3, 0);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId("P1")).thenReturn(Optional.of(inventory));

        OrderCreatedEvent event = new OrderCreatedEvent("order-1",
                List.of(new OrderCreatedEvent.OrderItem("P1", 5, new BigDecimal("10.00"))));

        assertThatThrownBy(() -> inventoryService.reserveStock(event))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("P1");
    }

    @Test
    void shouldCreateInventoryForNewProduct() {
        InventoryEntity newInventory = createInventory("P1", 100, 0);
        when(processedEventRepository.existsByEventId(any())).thenReturn(false);
        when(inventoryRepository.findByProductId("P1")).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenReturn(newInventory);

        OrderCreatedEvent event = new OrderCreatedEvent("order-1",
                List.of(new OrderCreatedEvent.OrderItem("P1", 5, new BigDecimal("10.00"))));

        inventoryService.reserveStock(event);

        verify(inventoryRepository, times(2)).save(any());
    }

    private InventoryEntity createInventory(String productId, int available, int reserved) {
        InventoryEntity entity = new InventoryEntity();
        entity.setProductId(productId);
        entity.setAvailableQuantity(available);
        entity.setReservedQuantity(reserved);
        return entity;
    }
}
