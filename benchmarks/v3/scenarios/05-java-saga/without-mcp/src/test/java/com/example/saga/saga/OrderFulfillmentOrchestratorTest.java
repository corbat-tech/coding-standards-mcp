package com.example.saga.saga;

import com.example.saga.application.port.InventoryService;
import com.example.saga.application.port.OrderService;
import com.example.saga.application.port.PaymentService;
import com.example.saga.application.port.ShippingService;
import com.example.saga.application.saga.CompensationSummary;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.SagaExecutionResult;
import com.example.saga.application.saga.SagaStep;
import com.example.saga.application.saga.orchestrator.OrderFulfillmentOrchestrator;
import com.example.saga.application.saga.step.CreateOrderStep;
import com.example.saga.application.saga.step.ProcessPaymentStep;
import com.example.saga.application.saga.step.ReserveInventoryStep;
import com.example.saga.application.saga.step.ShipOrderStep;
import com.example.saga.domain.entity.Order;
import com.example.saga.domain.valueobject.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderFulfillmentOrchestrator")
class OrderFulfillmentOrchestratorTest {

    @Mock
    private OrderService orderService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ShippingService shippingService;

    private OrderFulfillmentOrchestrator orchestrator;

    private List<OrderItem> testItems;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testItems = Arrays.asList(
                new OrderItem("prod-1", 2, new BigDecimal("50.00"))
        );

        testOrder = Order.create(
                "order-123",
                "customer-456",
                testItems,
                new BigDecimal("100.00")
        );

        // Create saga steps
        CreateOrderStep createOrderStep = new CreateOrderStep(orderService);
        ReserveInventoryStep reserveInventoryStep = new ReserveInventoryStep(inventoryService);
        ProcessPaymentStep processPaymentStep = new ProcessPaymentStep(paymentService);
        ShipOrderStep shipOrderStep = new ShipOrderStep(shippingService);

        List<SagaStep<?>> steps = Arrays.asList(
                createOrderStep,
                reserveInventoryStep,
                processPaymentStep,
                shipOrderStep
        );

        orchestrator = new OrderFulfillmentOrchestrator(steps);
    }

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @BeforeEach
        void setUpMocks() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenReturn(testOrder);
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenReturn("reservation-789");
            when(paymentService.processPayment(anyString(), anyString(), any(BigDecimal.class)))
                    .thenReturn("txn-101");
            when(shippingService.createShipment(anyString(), anyString()))
                    .thenReturn("tracking-202");
        }

        @Test
        @DisplayName("should complete saga successfully when all steps pass")
        void shouldCompleteSagaSuccessfullyWhenAllStepsPass() {
            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            SagaExecutionResult result = orchestrator.execute(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getCompletedSteps()).containsExactly(
                    "CreateOrder",
                    "ReserveInventory",
                    "ProcessPayment",
                    "ShipOrder"
            );
            assertThat(result.getFailedStep()).isEmpty();
            assertThat(result.getError()).isEmpty();
            assertThat(result.getContext().getOrder()).isPresent();
            assertThat(result.getContext().getInventoryReservationId())
                    .contains("reservation-789");
            assertThat(result.getContext().getPaymentTransactionId())
                    .contains("txn-101");
            assertThat(result.getContext().getShipmentTrackingId())
                    .contains("tracking-202");
        }

        @Test
        @DisplayName("should track execution state through saga")
        void shouldTrackExecutionStateThroughSaga() {
            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            orchestrator.execute(context);

            verify(orderService).createOrder(
                    eq("customer-456"),
                    eq(testItems),
                    eq(new BigDecimal("100.00"))
            );
            verify(inventoryService).reserveInventory(eq("order-123"), anyList());
            verify(paymentService).processPayment(
                    eq("order-123"),
                    eq("customer-456"),
                    eq(new BigDecimal("100.00"))
            );
            verify(shippingService).createShipment(eq("order-123"), eq("customer-456"));
        }
    }

    @Nested
    @DisplayName("Failure Scenarios with Compensation")
    class FailureScenarios {

        @Test
        @DisplayName("should compensate when create order fails")
        void shouldCompensateWhenCreateOrderFails() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenThrow(new RuntimeException("Order creation failed"));

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            SagaExecutionResult result = orchestrator.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailedStep()).contains("CreateOrder");
            assertThat(result.getCompletedSteps()).isEmpty();
            // No compensation needed since first step failed
            assertThat(result.getCompensationResults())
                    .isPresent()
                    .get()
                    .extracting(CompensationSummary::getCompletedCompensations)
                    .asList()
                    .isEmpty();
        }

        @Test
        @DisplayName("should compensate create order when reserve inventory fails")
        void shouldCompensateCreateOrderWhenReserveInventoryFails() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenReturn(testOrder);
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenThrow(new RuntimeException("Inventory not available"));

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            SagaExecutionResult result = orchestrator.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailedStep()).contains("ReserveInventory");
            assertThat(result.getCompletedSteps()).containsExactly("CreateOrder");
            assertThat(result.getCompensationResults())
                    .isPresent()
                    .get()
                    .extracting(CompensationSummary::isTriggered)
                    .isEqualTo(true);
            assertThat(result.getCompensationResults().get().getCompletedCompensations())
                    .containsExactly("CreateOrder");

            verify(orderService).cancelOrder(eq("order-123"));
        }

        @Test
        @DisplayName("should compensate inventory and order when payment fails")
        void shouldCompensateInventoryAndOrderWhenPaymentFails() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenReturn(testOrder);
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenReturn("reservation-789");
            when(paymentService.processPayment(anyString(), anyString(), any(BigDecimal.class)))
                    .thenThrow(new RuntimeException("Payment declined"));

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            SagaExecutionResult result = orchestrator.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailedStep()).contains("ProcessPayment");
            assertThat(result.getCompletedSteps()).containsExactly("CreateOrder", "ReserveInventory");
            assertThat(result.getCompensationResults())
                    .isPresent()
                    .get()
                    .extracting(CompensationSummary::isTriggered)
                    .isEqualTo(true);
            assertThat(result.getCompensationResults().get().getCompletedCompensations())
                    .contains("ReserveInventory", "CreateOrder");

            verify(inventoryService).releaseInventory(eq("reservation-789"));
            verify(orderService).cancelOrder(eq("order-123"));
        }

        @Test
        @DisplayName("should compensate all steps when shipping fails")
        void shouldCompensateAllStepsWhenShippingFails() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenReturn(testOrder);
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenReturn("reservation-789");
            when(paymentService.processPayment(anyString(), anyString(), any(BigDecimal.class)))
                    .thenReturn("txn-101");
            when(shippingService.createShipment(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Shipping unavailable"));

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            SagaExecutionResult result = orchestrator.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getFailedStep()).contains("ShipOrder");
            assertThat(result.getCompletedSteps()).containsExactly(
                    "CreateOrder",
                    "ReserveInventory",
                    "ProcessPayment"
            );
            assertThat(result.getCompensationResults())
                    .isPresent()
                    .get()
                    .extracting(CompensationSummary::isTriggered)
                    .isEqualTo(true);
            assertThat(result.getCompensationResults().get().getCompletedCompensations())
                    .hasSize(3);

            verify(paymentService).refundPayment(eq("txn-101"));
            verify(inventoryService).releaseInventory(eq("reservation-789"));
            verify(orderService).cancelOrder(eq("order-123"));
        }

        @Test
        @DisplayName("should handle compensation failure gracefully")
        void shouldHandleCompensationFailureGracefully() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenReturn(testOrder);
            when(inventoryService.reserveInventory(anyString(), anyList()))
                    .thenReturn("reservation-789");
            when(paymentService.processPayment(anyString(), anyString(), any(BigDecimal.class)))
                    .thenReturn("txn-101");
            when(shippingService.createShipment(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Shipping unavailable"));
            doThrow(new RuntimeException("Refund failed"))
                    .when(paymentService).refundPayment(anyString());

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            SagaExecutionResult result = orchestrator.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getCompensationResults())
                    .isPresent()
                    .get()
                    .extracting(CompensationSummary::isTriggered)
                    .isEqualTo(true);
            assertThat(result.getCompensationResults().get().getFailedCompensations())
                    .contains("ProcessPayment");
            // Other compensations should still complete
            assertThat(result.getCompensationResults().get().getCompletedCompensations())
                    .contains("ReserveInventory", "CreateOrder");
        }
    }

    @Nested
    @DisplayName("Orchestrator Configuration")
    class OrchestratorConfiguration {

        @Test
        @DisplayName("should return registered steps")
        void shouldReturnRegisteredSteps() {
            List<SagaStep<?>> steps = orchestrator.getSteps();

            assertThat(steps).hasSize(4);
            assertThat(steps)
                    .extracting(SagaStep::getName)
                    .containsExactly(
                            "CreateOrder",
                            "ReserveInventory",
                            "ProcessPayment",
                            "ShipOrder"
                    );
        }
    }
}
