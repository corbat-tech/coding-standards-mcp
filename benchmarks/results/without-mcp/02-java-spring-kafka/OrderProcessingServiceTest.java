package com.orders.service;

import com.orders.entity.ProcessedOrder;
import com.orders.event.OrderCreatedEvent;
import com.orders.exception.OrderValidationException;
import com.orders.repository.ProcessedOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private ProcessedOrderRepository processedOrderRepository;

    @Mock
    private OrderValidationService orderValidationService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderProcessingService orderProcessingService;

    private OrderCreatedEvent validEvent;

    @BeforeEach
    void setUp() {
        OrderCreatedEvent.OrderItem item = new OrderCreatedEvent.OrderItem(
                "PROD-001", "Test Product", 2, new BigDecimal("29.99")
        );
        validEvent = new OrderCreatedEvent(
                "ORDER-123",
                "CUST-456",
                Arrays.asList(item),
                new BigDecimal("59.98"),
                Instant.now()
        );
    }

    @Test
    void processOrder_ValidOrder_ProcessesSuccessfully() {
        when(processedOrderRepository.existsByOrderId("ORDER-123")).thenReturn(false);
        when(processedOrderRepository.findById("ORDER-123")).thenReturn(Optional.empty());
        when(processedOrderRepository.save(any(ProcessedOrder.class))).thenAnswer(i -> i.getArgument(0));

        doNothing().when(orderValidationService).validate(any(OrderCreatedEvent.class));
        doNothing().when(inventoryService).updateInventory(any(OrderCreatedEvent.class));
        doNothing().when(notificationService).sendOrderConfirmation(any(OrderCreatedEvent.class));

        assertDoesNotThrow(() -> orderProcessingService.processOrder(validEvent));

        verify(orderValidationService, times(1)).validate(validEvent);
        verify(inventoryService, times(1)).updateInventory(validEvent);
        verify(notificationService, times(1)).sendOrderConfirmation(validEvent);
        verify(processedOrderRepository, times(2)).save(any(ProcessedOrder.class));
    }

    @Test
    void processOrder_DuplicateOrder_SkipsProcessing() {
        when(processedOrderRepository.existsByOrderId("ORDER-123")).thenReturn(true);

        orderProcessingService.processOrder(validEvent);

        verify(orderValidationService, never()).validate(any());
        verify(inventoryService, never()).updateInventory(any());
        verify(notificationService, never()).sendOrderConfirmation(any());
    }

    @Test
    void processOrder_ValidationFails_ThrowsException() {
        when(processedOrderRepository.existsByOrderId("ORDER-123")).thenReturn(false);
        when(processedOrderRepository.findById("ORDER-123")).thenReturn(Optional.empty());
        when(processedOrderRepository.save(any(ProcessedOrder.class))).thenAnswer(i -> i.getArgument(0));

        doThrow(new OrderValidationException("Invalid order"))
                .when(orderValidationService).validate(any(OrderCreatedEvent.class));

        assertThrows(OrderValidationException.class, () -> orderProcessingService.processOrder(validEvent));

        verify(inventoryService, never()).updateInventory(any());
        verify(notificationService, never()).sendOrderConfirmation(any());
    }

    @Test
    void processOrder_InventoryUpdateFails_ThrowsException() {
        when(processedOrderRepository.existsByOrderId("ORDER-123")).thenReturn(false);
        when(processedOrderRepository.findById("ORDER-123")).thenReturn(Optional.empty());
        when(processedOrderRepository.save(any(ProcessedOrder.class))).thenAnswer(i -> i.getArgument(0));

        doNothing().when(orderValidationService).validate(any(OrderCreatedEvent.class));
        doThrow(new RuntimeException("Inventory error"))
                .when(inventoryService).updateInventory(any(OrderCreatedEvent.class));

        assertThrows(RuntimeException.class, () -> orderProcessingService.processOrder(validEvent));

        verify(notificationService, never()).sendOrderConfirmation(any());
    }

    @Test
    void isAlreadyProcessed_ExistingOrder_ReturnsTrue() {
        when(processedOrderRepository.existsByOrderId("ORDER-123")).thenReturn(true);

        assertTrue(orderProcessingService.isAlreadyProcessed("ORDER-123"));
    }

    @Test
    void isAlreadyProcessed_NewOrder_ReturnsFalse() {
        when(processedOrderRepository.existsByOrderId("ORDER-999")).thenReturn(false);

        assertFalse(orderProcessingService.isAlreadyProcessed("ORDER-999"));
    }
}
