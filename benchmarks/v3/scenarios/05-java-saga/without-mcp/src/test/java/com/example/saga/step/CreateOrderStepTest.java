package com.example.saga.step;

import com.example.saga.application.port.OrderService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.StepResult;
import com.example.saga.application.saga.step.CreateOrderStep;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderStep")
class CreateOrderStepTest {

    @Mock
    private OrderService orderService;

    private CreateOrderStep step;

    private List<OrderItem> testItems;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        step = new CreateOrderStep(orderService);

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
        assertThat(step.getName()).isEqualTo("CreateOrder");
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should create order successfully")
        void shouldCreateOrderSuccessfully() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenReturn(testOrder);

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            StepResult<Order> result = step.execute(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).contains(testOrder);
            assertThat(result.getError()).isEmpty();

            verify(orderService).createOrder("customer-456", testItems, new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should fail when order service throws exception")
        void shouldFailWhenOrderServiceThrowsException() {
            when(orderService.createOrder(anyString(), anyList(), any(BigDecimal.class)))
                    .thenThrow(new RuntimeException("Database error"));

            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("customerId", "customer-456")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            StepResult<Order> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getData()).isEmpty();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("ORDER_CREATION_FAILED");
            assertThat(result.getError().get().getStepName()).isEqualTo("CreateOrder");
            assertThat(result.getError().get().isRetryable()).isTrue();
        }

        @Test
        @DisplayName("should fail when customer ID is missing from context")
        void shouldFailWhenCustomerIdIsMissing() {
            SagaContext context = SagaContext.create("order-123")
                    .withMetadata("items", testItems)
                    .withMetadata("totalAmount", new BigDecimal("100.00"));

            StepResult<Order> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();

            verify(orderService, never()).createOrder(anyString(), anyList(), any(BigDecimal.class));
        }
    }

    @Nested
    @DisplayName("Compensate")
    class Compensate {

        @Test
        @DisplayName("should cancel order successfully")
        void shouldCancelOrderSuccessfully() {
            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getError()).isEmpty();

            verify(orderService).cancelOrder("order-123");
        }

        @Test
        @DisplayName("should succeed when no order in context")
        void shouldSucceedWhenNoOrderInContext() {
            SagaContext context = SagaContext.create("order-123");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();

            verify(orderService, never()).cancelOrder(anyString());
        }

        @Test
        @DisplayName("should fail when order cancellation throws exception")
        void shouldFailWhenOrderCancellationThrowsException() {
            doThrow(new RuntimeException("Cancellation failed"))
                    .when(orderService).cancelOrder(anyString());

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("ORDER_CANCELLATION_FAILED");
        }
    }
}
