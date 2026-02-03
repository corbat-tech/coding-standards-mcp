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
class SagaStepsTest {

    @Mock private OrderService orderService;
    @Mock private InventoryService inventoryService;
    @Mock private PaymentService paymentService;
    @Mock private ShippingService shippingService;

    private SagaContext context;

    @BeforeEach
    void setUp() {
        Order order = new Order(
                OrderId.generate(),
                "customer-1",
                List.of(new OrderItem("P1", 2, Money.of(25.00))),
                Money.of(50.00)
        );
        context = new SagaContext(order, "123 Main St");
    }

    @Test
    void createOrderStepShouldReturnSuccessOnSuccess() {
        CreateOrderStep step = new CreateOrderStep(orderService);

        StepResult result = step.execute(context);

        assertThat(result.success()).isTrue();
        verify(orderService).createOrder(context.getOrder());
    }

    @Test
    void createOrderStepShouldCompensateByCancelling() {
        CreateOrderStep step = new CreateOrderStep(orderService);

        step.compensate(context);

        verify(orderService).cancelOrder(context.getOrder().getId());
    }

    @Test
    void reserveInventoryStepShouldReturnFailureOnException() {
        doThrow(new RuntimeException("No stock")).when(inventoryService).reserveInventory(any(), any());
        ReserveInventoryStep step = new ReserveInventoryStep(inventoryService);

        StepResult result = step.execute(context);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("No stock");
    }

    @Test
    void processPaymentStepShouldStorePaymentIdInContext() {
        when(paymentService.processPayment(any(), any(), any())).thenReturn("pay-456");
        ProcessPaymentStep step = new ProcessPaymentStep(paymentService);

        step.execute(context);

        assertThat(context.<String>get("paymentId")).isEqualTo("pay-456");
    }

    @Test
    void processPaymentStepShouldRefundOnCompensate() {
        when(paymentService.processPayment(any(), any(), any())).thenReturn("pay-789");
        ProcessPaymentStep step = new ProcessPaymentStep(paymentService);
        step.execute(context);

        step.compensate(context);

        verify(paymentService).refundPayment("pay-789");
    }

    @Test
    void shipOrderStepShouldCancelShipmentOnCompensate() {
        when(shippingService.createShipment(any(), any())).thenReturn("ship-123");
        ShipOrderStep step = new ShipOrderStep(shippingService);
        step.execute(context);

        step.compensate(context);

        verify(shippingService).cancelShipment("ship-123");
    }
}
