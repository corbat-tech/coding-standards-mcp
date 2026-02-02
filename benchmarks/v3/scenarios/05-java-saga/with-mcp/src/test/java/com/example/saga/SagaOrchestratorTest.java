package com.example.saga;

import com.example.saga.application.port.*;
import com.example.saga.application.saga.*;
import com.example.saga.application.saga.step.*;
import com.example.saga.domain.entity.*;
import com.example.saga.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

    @Mock private OrderService orderService;
    @Mock private InventoryService inventoryService;
    @Mock private PaymentService paymentService;
    @Mock private ShippingService shippingService;

    private SagaOrchestrator orchestrator;
    private SagaContext context;

    @BeforeEach
    void setUp() {
        List<SagaStep> steps = List.of(
                new CreateOrderStep(orderService),
                new ReserveInventoryStep(inventoryService),
                new ProcessPaymentStep(paymentService),
                new ShipOrderStep(shippingService)
        );
        orchestrator = new SagaOrchestrator(steps);

        Order order = new Order(
                OrderId.generate(),
                "customer-1",
                List.of(new OrderItem("P1", 2, Money.of(25.00))),
                Money.of(50.00)
        );
        context = new SagaContext(order, "123 Main St");
    }

    @Test
    void shouldCompleteAllStepsSuccessfully() {
        when(paymentService.processPayment(any(), any(), any())).thenReturn("pay-123");
        when(shippingService.createShipment(any(), any())).thenReturn("ship-123");

        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.success()).isTrue();
        verify(orderService).createOrder(any());
        verify(inventoryService).reserveInventory(any(), any());
        verify(paymentService).processPayment(any(), any(), any());
        verify(shippingService).createShipment(any(), any());
    }

    @Test
    void shouldCompensateWhenInventoryFails() {
        doThrow(new RuntimeException("Out of stock")).when(inventoryService).reserveInventory(any(), any());

        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepName()).isEqualTo("ReserveInventory");
        assertThat(result.compensatedSteps()).containsExactly("CreateOrder");
        verify(orderService).cancelOrder(any());
    }

    @Test
    void shouldCompensateWhenPaymentFails() {
        doThrow(new RuntimeException("Payment declined")).when(paymentService).processPayment(any(), any(), any());

        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepName()).isEqualTo("ProcessPayment");
        assertThat(result.compensatedSteps()).containsExactly("ReserveInventory", "CreateOrder");
        verify(inventoryService).releaseInventory(any(), any());
        verify(orderService).cancelOrder(any());
    }

    @Test
    void shouldCompensateWhenShippingFails() {
        when(paymentService.processPayment(any(), any(), any())).thenReturn("pay-123");
        doThrow(new RuntimeException("Shipping unavailable")).when(shippingService).createShipment(any(), any());

        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepName()).isEqualTo("ShipOrder");
        assertThat(result.compensatedSteps()).containsExactly("ProcessPayment", "ReserveInventory", "CreateOrder");
        verify(paymentService).refundPayment("pay-123");
        verify(inventoryService).releaseInventory(any(), any());
        verify(orderService).cancelOrder(any());
    }

    @Test
    void shouldHandleCreateOrderFailure() {
        doThrow(new RuntimeException("DB error")).when(orderService).createOrder(any());

        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.failedStepName()).isEqualTo("CreateOrder");
        assertThat(result.compensatedSteps()).isEmpty();
        verify(inventoryService, never()).reserveInventory(any(), any());
    }
}
