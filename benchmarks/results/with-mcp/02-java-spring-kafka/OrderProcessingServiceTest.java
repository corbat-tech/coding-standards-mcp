package com.orders.application.service;

import com.orders.domain.event.OrderCreatedEvent;
import com.orders.domain.exception.OrderValidationException;
import com.orders.domain.model.OrderStatus;
import com.orders.domain.model.ProcessedOrder;
import com.orders.domain.port.out.ProcessedOrderRepository;
import com.orders.domain.service.InventoryService;
import com.orders.domain.service.NotificationService;
import com.orders.domain.service.OrderValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderProcessingService")
class OrderProcessingServiceTest {

    @Mock
    private ProcessedOrderRepository orderRepository;

    @Mock
    private OrderValidationService validationService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderProcessingService orderProcessingService;

    private OrderCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new OrderCreatedEvent(
            "order-123",
            "customer-456",
            List.of(new OrderCreatedEvent.OrderItem("prod-1", "Product", 2, new BigDecimal("25.00"))),
            new BigDecimal("50.00"),
            Instant.now()
        );
    }

    @Nested
    @DisplayName("processOrder")
    class ProcessOrder {

        @Test
        @DisplayName("should_process_new_order_successfully")
        void should_process_new_order_successfully() {
            // Arrange
            when(orderRepository.findById("order-123")).thenReturn(Optional.empty());
            when(orderRepository.save(any(ProcessedOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(validationService).validate(testEvent);
            doNothing().when(inventoryService).reserveInventory(testEvent);
            doNothing().when(notificationService).sendOrderConfirmation(testEvent);

            // Act
            orderProcessingService.processOrder(testEvent);

            // Assert
            verify(validationService).validate(testEvent);
            verify(inventoryService).reserveInventory(testEvent);
            verify(notificationService).sendOrderConfirmation(testEvent);
        }

        @Test
        @DisplayName("should_skip_already_processed_order")
        void should_skip_already_processed_order() {
            // Arrange
            ProcessedOrder completedOrder = new ProcessedOrder(
                "order-123", "customer-456", new BigDecimal("50.00"), Instant.now());
            completedOrder.markAsCompleted();
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(completedOrder));

            // Act
            orderProcessingService.processOrder(testEvent);

            // Assert
            verify(validationService, never()).validate(any());
            verify(inventoryService, never()).reserveInventory(any());
        }

        @Test
        @DisplayName("should_mark_order_as_completed_when_successful")
        void should_mark_order_as_completed_when_successful() {
            // Arrange
            when(orderRepository.findById("order-123")).thenReturn(Optional.empty());
            ArgumentCaptor<ProcessedOrder> captor = ArgumentCaptor.forClass(ProcessedOrder.class);
            when(orderRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            orderProcessingService.processOrder(testEvent);

            // Assert
            List<ProcessedOrder> savedOrders = captor.getAllValues();
            ProcessedOrder finalOrder = savedOrders.get(savedOrders.size() - 1);
            assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("should_mark_order_as_failed_when_validation_fails")
        void should_mark_order_as_failed_when_validation_fails() {
            // Arrange
            when(orderRepository.findById("order-123")).thenReturn(Optional.empty());
            when(orderRepository.save(any(ProcessedOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new OrderValidationException("Invalid order"))
                .when(validationService).validate(testEvent);

            // Act & Assert
            assertThatThrownBy(() -> orderProcessingService.processOrder(testEvent))
                .isInstanceOf(OrderValidationException.class);

            verify(inventoryService).releaseInventory(testEvent);
        }

        @Test
        @DisplayName("should_release_inventory_when_processing_fails")
        void should_release_inventory_when_processing_fails() {
            // Arrange
            when(orderRepository.findById("order-123")).thenReturn(Optional.empty());
            when(orderRepository.save(any(ProcessedOrder.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Inventory error"))
                .when(inventoryService).reserveInventory(testEvent);

            // Act & Assert
            assertThatThrownBy(() -> orderProcessingService.processOrder(testEvent))
                .isInstanceOf(RuntimeException.class);

            verify(inventoryService).releaseInventory(testEvent);
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        @Test
        @DisplayName("should_not_reprocess_completed_order")
        void should_not_reprocess_completed_order() {
            // Arrange
            ProcessedOrder existingOrder = new ProcessedOrder(
                "order-123", "customer-456", new BigDecimal("50.00"), Instant.now());
            existingOrder.markAsCompleted();
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(existingOrder));

            // Act
            orderProcessingService.processOrder(testEvent);

            // Assert
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_reprocess_failed_order")
        void should_reprocess_failed_order() {
            // Arrange
            ProcessedOrder failedOrder = new ProcessedOrder(
                "order-123", "customer-456", new BigDecimal("50.00"), Instant.now());
            failedOrder.markAsFailed("Previous error");
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(failedOrder));
            when(orderRepository.save(any(ProcessedOrder.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            orderProcessingService.processOrder(testEvent);

            // Assert
            verify(validationService).validate(testEvent);
        }
    }
}
