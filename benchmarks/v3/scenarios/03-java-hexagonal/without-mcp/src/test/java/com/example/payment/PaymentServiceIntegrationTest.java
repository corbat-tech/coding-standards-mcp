package com.example.payment;

import com.example.payment.adapter.output.gateway.StripePaymentGatewayAdapter;
import com.example.payment.application.port.input.GetPaymentStatusUseCase;
import com.example.payment.application.port.input.GetPaymentStatusUseCase.PaymentStatusResponse;
import com.example.payment.application.port.input.ProcessPaymentUseCase;
import com.example.payment.application.port.input.ProcessPaymentUseCase.ProcessPaymentCommand;
import com.example.payment.application.port.input.RefundPaymentUseCase;
import com.example.payment.application.port.input.RefundPaymentUseCase.RefundPaymentCommand;
import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete payment service hexagonal architecture.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Payment Service Integration Tests")
class PaymentServiceIntegrationTest {

    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;

    @Autowired
    private RefundPaymentUseCase refundPaymentUseCase;

    @Autowired
    private GetPaymentStatusUseCase getPaymentStatusUseCase;

    @Autowired
    private StripePaymentGatewayAdapter stripeGateway;

    @BeforeEach
    void setUp() {
        stripeGateway.resetSimulation();
    }

    @Nested
    @DisplayName("Complete Payment Flow")
    class CompletePaymentFlow {

        @Test
        @DisplayName("should process payment and persist in database")
        void shouldProcessPaymentAndPersistInDatabase() {
            ProcessPaymentCommand command = new ProcessPaymentCommand(
                    "order-integration-1",
                    "customer-integration-1",
                    new BigDecimal("250.00"),
                    "USD",
                    "valid_card_token",
                    "test@example.com"
            );

            Payment result = processPaymentUseCase.processPayment(command);

            assertNotNull(result.getId());
            assertEquals(PaymentStatus.COMPLETED, result.getStatus());
            assertNotNull(result.getGatewayTransactionId());

            // Verify can retrieve from database
            PaymentStatusResponse retrieved = getPaymentStatusUseCase.getPaymentStatus(
                    result.getId().toString());
            assertEquals(result.getId().toString(), retrieved.paymentId());
            assertEquals(PaymentStatus.COMPLETED, retrieved.status());
        }

        @Test
        @DisplayName("should handle payment failure gracefully")
        void shouldHandlePaymentFailureGracefully() {
            stripeGateway.setSimulateFailure(true);
            stripeGateway.setSimulatedError("card_declined", "Your card was declined");

            ProcessPaymentCommand command = new ProcessPaymentCommand(
                    "order-fail-1",
                    "customer-fail-1",
                    new BigDecimal("100.00"),
                    "USD",
                    "invalid_card",
                    null
            );

            Payment result = processPaymentUseCase.processPayment(command);

            assertEquals(PaymentStatus.FAILED, result.getStatus());
            assertTrue(result.getFailureReason().contains("card_declined"));
        }
    }

    @Nested
    @DisplayName("Complete Refund Flow")
    class CompleteRefundFlow {

        @Test
        @DisplayName("should process full refund successfully")
        void shouldProcessFullRefundSuccessfully() {
            // First, create and complete a payment
            ProcessPaymentCommand paymentCommand = new ProcessPaymentCommand(
                    "order-refund-1",
                    "customer-refund-1",
                    new BigDecimal("150.00"),
                    "USD",
                    "valid_card_token",
                    null
            );
            Payment payment = processPaymentUseCase.processPayment(paymentCommand);

            // Then refund it
            RefundPaymentCommand refundCommand = new RefundPaymentCommand(
                    payment.getId().toString(),
                    new BigDecimal("150.00"),
                    "Customer requested refund",
                    null
            );

            Payment refundedPayment = refundPaymentUseCase.refundPayment(refundCommand);

            assertEquals(PaymentStatus.REFUNDED, refundedPayment.getStatus());
            assertEquals(new BigDecimal("150.00"), refundedPayment.getRefundedAmount().getAmount());
        }

        @Test
        @DisplayName("should process multiple partial refunds")
        void shouldProcessMultiplePartialRefunds() {
            // Create payment
            ProcessPaymentCommand paymentCommand = new ProcessPaymentCommand(
                    "order-partial-1",
                    "customer-partial-1",
                    new BigDecimal("200.00"),
                    "USD",
                    "valid_card_token",
                    null
            );
            Payment payment = processPaymentUseCase.processPayment(paymentCommand);

            // First partial refund
            RefundPaymentCommand firstRefund = new RefundPaymentCommand(
                    payment.getId().toString(),
                    new BigDecimal("50.00"),
                    "First refund",
                    null
            );
            Payment afterFirstRefund = refundPaymentUseCase.refundPayment(firstRefund);
            assertEquals(PaymentStatus.PARTIALLY_REFUNDED, afterFirstRefund.getStatus());
            assertEquals(new BigDecimal("50.00"), afterFirstRefund.getRefundedAmount().getAmount());

            // Second partial refund
            RefundPaymentCommand secondRefund = new RefundPaymentCommand(
                    payment.getId().toString(),
                    new BigDecimal("75.00"),
                    "Second refund",
                    null
            );
            Payment afterSecondRefund = refundPaymentUseCase.refundPayment(secondRefund);
            assertEquals(PaymentStatus.PARTIALLY_REFUNDED, afterSecondRefund.getStatus());
            assertEquals(new BigDecimal("125.00"), afterSecondRefund.getRefundedAmount().getAmount());
        }
    }

    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {

        @Test
        @DisplayName("should find payments by customer ID")
        void shouldFindPaymentsByCustomerId() {
            String customerId = "customer-query-test";

            // Create multiple payments for the same customer
            for (int i = 0; i < 3; i++) {
                ProcessPaymentCommand command = new ProcessPaymentCommand(
                        "order-query-" + i,
                        customerId,
                        new BigDecimal("100.00"),
                        "USD",
                        "valid_card_token",
                        null
                );
                processPaymentUseCase.processPayment(command);
            }

            List<PaymentStatusResponse> payments = getPaymentStatusUseCase.getPaymentsByCustomer(customerId);

            assertEquals(3, payments.size());
            assertTrue(payments.stream().allMatch(p -> p.customerId().equals(customerId)));
        }

        @Test
        @DisplayName("should find payments by order ID")
        void shouldFindPaymentsByOrderId() {
            String orderId = "order-unique-query";

            ProcessPaymentCommand command = new ProcessPaymentCommand(
                    orderId,
                    "customer-order-query",
                    new BigDecimal("100.00"),
                    "USD",
                    "valid_card_token",
                    null
            );
            processPaymentUseCase.processPayment(command);

            List<PaymentStatusResponse> payments = getPaymentStatusUseCase.getPaymentsByOrder(orderId);

            assertEquals(1, payments.size());
            assertEquals(orderId, payments.get(0).orderId());
        }
    }
}
