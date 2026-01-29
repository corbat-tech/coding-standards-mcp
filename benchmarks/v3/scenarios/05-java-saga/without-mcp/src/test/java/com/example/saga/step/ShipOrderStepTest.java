package com.example.saga.step;

import com.example.saga.application.port.ShippingService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.StepResult;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipOrderStep")
class ShipOrderStepTest {

    @Mock
    private ShippingService shippingService;

    private ShipOrderStep step;

    private List<OrderItem> testItems;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        step = new ShipOrderStep(shippingService);

        testItems = Arrays.asList(
                new OrderItem("prod-1", 2, new BigDecimal("50.00"))
        );

        testOrder = Order.create(
                "order-123",
                "customer-456",
                testItems,
                new BigDecimal("100.00")
        );
    }

    @Test
    @DisplayName("should have correct step name")
    void shouldHaveCorrectStepName() {
        assertThat(step.getName()).isEqualTo("ShipOrder");
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should create shipment successfully")
        void shouldCreateShipmentSuccessfully() {
            when(shippingService.createShipment(anyString(), anyString()))
                    .thenReturn("tracking-202");

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).contains("tracking-202");
            assertThat(result.getError()).isEmpty();

            verify(shippingService).createShipment("order-123", "customer-456");
        }

        @Test
        @DisplayName("should fail when order is not in context")
        void shouldFailWhenOrderIsNotInContext() {
            SagaContext context = SagaContext.create("order-123");

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("ORDER_NOT_FOUND");
            assertThat(result.getError().get().isRetryable()).isFalse();

            verify(shippingService, never()).createShipment(anyString(), anyString());
        }

        @Test
        @DisplayName("should fail when shipping service throws exception")
        void shouldFailWhenShippingServiceThrowsException() {
            when(shippingService.createShipment(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Shipping unavailable"));

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("SHIPPING_FAILED");
            assertThat(result.getError().get().isRetryable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Compensate")
    class Compensate {

        @Test
        @DisplayName("should cancel shipment successfully")
        void shouldCancelShipmentSuccessfully() {
            SagaContext context = SagaContext.create("order-123")
                    .withShipmentTracking("tracking-202");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getError()).isEmpty();

            verify(shippingService).cancelShipment("tracking-202");
        }

        @Test
        @DisplayName("should succeed when no tracking in context")
        void shouldSucceedWhenNoTrackingInContext() {
            SagaContext context = SagaContext.create("order-123");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();

            verify(shippingService, never()).cancelShipment(anyString());
        }

        @Test
        @DisplayName("should fail when shipment cancellation throws exception")
        void shouldFailWhenShipmentCancellationThrowsException() {
            doThrow(new RuntimeException("Cancellation failed"))
                    .when(shippingService).cancelShipment(anyString());

            SagaContext context = SagaContext.create("order-123")
                    .withShipmentTracking("tracking-202");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("SHIPMENT_CANCELLATION_FAILED");
        }
    }
}
