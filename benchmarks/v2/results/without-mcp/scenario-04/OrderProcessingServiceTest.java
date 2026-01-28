package com.example.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderProcessingService service;

    @BeforeEach
    void setUp() {
        service = new OrderProcessingService(orderRepository);
    }

    @Test
    void processOrder_ValidOrder_ReturnsProcessedOrder() {
        OrderEvent event = createValidOrderEvent();
        when(orderRepository.existsByOrderId(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedOrder result = service.processOrder(event);

        assertEquals(event.getOrderId(), result.getOrderId());
        assertEquals(ProcessedOrder.ProcessingStatus.PROCESSED, result.getStatus());
        verify(orderRepository).save(any());
    }

    @Test
    void processOrder_DuplicateOrder_ReturnsExisting() {
        OrderEvent event = createValidOrderEvent();
        ProcessedOrder existing = new ProcessedOrder(
            event.getOrderId(),
            event.getCustomerId(),
            event.getTotalAmount(),
            ProcessedOrder.ProcessingStatus.PROCESSED
        );

        when(orderRepository.existsByOrderId(event.getOrderId())).thenReturn(true);
        when(orderRepository.findByOrderId(event.getOrderId())).thenReturn(Optional.of(existing));

        ProcessedOrder result = service.processOrder(event);

        assertEquals(existing, result);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void processOrder_MissingOrderId_SavesAsFailed() {
        OrderEvent event = createValidOrderEvent();
        event.setOrderId(null);
        when(orderRepository.existsByOrderId(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedOrder result = service.processOrder(event);

        assertEquals(ProcessedOrder.ProcessingStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void processOrder_MissingCustomerId_SavesAsFailed() {
        OrderEvent event = createValidOrderEvent();
        event.setCustomerId(null);
        when(orderRepository.existsByOrderId(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedOrder result = service.processOrder(event);

        assertEquals(ProcessedOrder.ProcessingStatus.FAILED, result.getStatus());
    }

    @Test
    void processOrder_EmptyItems_SavesAsFailed() {
        OrderEvent event = createValidOrderEvent();
        event.setItems(List.of());
        when(orderRepository.existsByOrderId(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedOrder result = service.processOrder(event);

        assertEquals(ProcessedOrder.ProcessingStatus.FAILED, result.getStatus());
    }

    @Test
    void processOrder_NegativeAmount_SavesAsFailed() {
        OrderEvent event = createValidOrderEvent();
        event.setTotalAmount(new BigDecimal("-10.00"));
        when(orderRepository.existsByOrderId(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessedOrder result = service.processOrder(event);

        assertEquals(ProcessedOrder.ProcessingStatus.FAILED, result.getStatus());
    }

    private OrderEvent createValidOrderEvent() {
        OrderEvent event = new OrderEvent();
        event.setOrderId("order-123");
        event.setCustomerId("customer-456");
        event.setItems(List.of(
            new OrderEvent.OrderItem("product-1", 2, new BigDecimal("25.00"))
        ));
        event.setTotalAmount(new BigDecimal("50.00"));
        return event;
    }
}
