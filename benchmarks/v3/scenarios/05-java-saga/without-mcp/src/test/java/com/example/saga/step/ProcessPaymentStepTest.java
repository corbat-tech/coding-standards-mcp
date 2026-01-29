package com.example.saga.step;

import com.example.saga.application.port.PaymentService;
import com.example.saga.application.saga.CompensationResult;
import com.example.saga.application.saga.SagaContext;
import com.example.saga.application.saga.StepResult;
import com.example.saga.application.saga.step.ProcessPaymentStep;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessPaymentStep")
class ProcessPaymentStepTest {

    @Mock
    private PaymentService paymentService;

    private ProcessPaymentStep step;

    private List<OrderItem> testItems;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        step = new ProcessPaymentStep(paymentService);

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
        assertThat(step.getName()).isEqualTo("ProcessPayment");
    }

    @Nested
    @DisplayName("Execute")
    class Execute {

        @Test
        @DisplayName("should process payment successfully")
        void shouldProcessPaymentSuccessfully() {
            when(paymentService.processPayment(anyString(), anyString(), any(BigDecimal.class)))
                    .thenReturn("txn-101");

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).contains("txn-101");
            assertThat(result.getError()).isEmpty();

            verify(paymentService).processPayment("order-123", "customer-456", new BigDecimal("100.00"));
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

            verify(paymentService, never()).processPayment(anyString(), anyString(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("should fail when payment service throws exception")
        void shouldFailWhenPaymentServiceThrowsException() {
            when(paymentService.processPayment(anyString(), anyString(), any(BigDecimal.class)))
                    .thenThrow(new RuntimeException("Payment declined"));

            SagaContext context = SagaContext.create("order-123")
                    .withOrder(testOrder);

            StepResult<String> result = step.execute(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("PAYMENT_PROCESSING_FAILED");
            assertThat(result.getError().get().isRetryable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Compensate")
    class Compensate {

        @Test
        @DisplayName("should refund payment successfully")
        void shouldRefundPaymentSuccessfully() {
            SagaContext context = SagaContext.create("order-123")
                    .withPaymentTransaction("txn-101");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getError()).isEmpty();

            verify(paymentService).refundPayment("txn-101");
        }

        @Test
        @DisplayName("should succeed when no transaction in context")
        void shouldSucceedWhenNoTransactionInContext() {
            SagaContext context = SagaContext.create("order-123");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isTrue();

            verify(paymentService, never()).refundPayment(anyString());
        }

        @Test
        @DisplayName("should fail when refund throws exception")
        void shouldFailWhenRefundThrowsException() {
            doThrow(new RuntimeException("Refund failed"))
                    .when(paymentService).refundPayment(anyString());

            SagaContext context = SagaContext.create("order-123")
                    .withPaymentTransaction("txn-101");

            CompensationResult result = step.compensate(context);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getError()).isPresent();
            assertThat(result.getError().get().getCode()).isEqualTo("PAYMENT_REFUND_FAILED");
        }
    }
}
