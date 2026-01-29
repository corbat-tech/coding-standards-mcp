package com.example.payment.domain;

import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.exception.InvalidPaymentOperationException;
import com.example.payment.domain.valueobject.Money;
import com.example.payment.domain.valueobject.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Payment domain entity.
 */
@DisplayName("Payment Domain Entity")
class PaymentTest {

    private Money amount;

    @BeforeEach
    void setUp() {
        amount = Money.of(new BigDecimal("100.00"), "USD");
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create payment with valid data")
        void shouldCreatePaymentWithValidData() {
            Payment payment = Payment.create("order-123", "customer-456", amount);

            assertNotNull(payment.getId());
            assertEquals("order-123", payment.getOrderId());
            assertEquals("customer-456", payment.getCustomerId());
            assertEquals(amount, payment.getAmount());
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
            assertTrue(payment.getRefundedAmount().isZero());
            assertNotNull(payment.getCreatedAt());
            assertNotNull(payment.getUpdatedAt());
        }

        @Test
        @DisplayName("should throw exception for null order ID")
        void shouldThrowExceptionForNullOrderId() {
            assertThrows(NullPointerException.class, () ->
                    Payment.create(null, "customer-456", amount));
        }

        @Test
        @DisplayName("should throw exception for null customer ID")
        void shouldThrowExceptionForNullCustomerId() {
            assertThrows(NullPointerException.class, () ->
                    Payment.create("order-123", null, amount));
        }

        @Test
        @DisplayName("should throw exception for zero amount")
        void shouldThrowExceptionForZeroAmount() {
            Money zeroAmount = Money.zero("USD");

            assertThrows(IllegalArgumentException.class, () ->
                    Payment.create("order-123", "customer-456", zeroAmount));
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition from PENDING to PROCESSING")
        void shouldTransitionFromPendingToProcessing() {
            Payment payment = Payment.create("order-123", "customer-456", amount);

            payment.startProcessing();

            assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
        }

        @Test
        @DisplayName("should not allow processing when not PENDING")
        void shouldNotAllowProcessingWhenNotPending() {
            Payment payment = Payment.create("order-123", "customer-456", amount);
            payment.startProcessing();
            payment.complete("txn-123");

            assertThrows(InvalidPaymentOperationException.class, payment::startProcessing);
        }

        @Test
        @DisplayName("should transition from PROCESSING to COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            Payment payment = Payment.create("order-123", "customer-456", amount);
            payment.startProcessing();

            payment.complete("txn-123");

            assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
            assertEquals("txn-123", payment.getGatewayTransactionId());
        }

        @Test
        @DisplayName("should not allow completion when not PROCESSING")
        void shouldNotAllowCompletionWhenNotProcessing() {
            Payment payment = Payment.create("order-123", "customer-456", amount);

            assertThrows(InvalidPaymentOperationException.class, () ->
                    payment.complete("txn-123"));
        }

        @Test
        @DisplayName("should transition from PROCESSING to FAILED")
        void shouldTransitionFromProcessingToFailed() {
            Payment payment = Payment.create("order-123", "customer-456", amount);
            payment.startProcessing();

            payment.fail("Card declined");

            assertEquals(PaymentStatus.FAILED, payment.getStatus());
            assertEquals("Card declined", payment.getFailureReason());
        }

        @Test
        @DisplayName("should allow failure from PENDING state")
        void shouldAllowFailureFromPendingState() {
            Payment payment = Payment.create("order-123", "customer-456", amount);

            payment.fail("Invalid card");

            assertEquals(PaymentStatus.FAILED, payment.getStatus());
        }
    }

    @Nested
    @DisplayName("Refund Operations")
    class RefundOperations {

        private Payment completedPayment;

        @BeforeEach
        void setUp() {
            completedPayment = Payment.create("order-123", "customer-456", amount);
            completedPayment.startProcessing();
            completedPayment.complete("txn-123");
        }

        @Test
        @DisplayName("should initiate refund for completed payment")
        void shouldInitiateRefundForCompletedPayment() {
            Money refundAmount = Money.of(new BigDecimal("50.00"), "USD");

            completedPayment.initiateRefund(refundAmount);

            assertEquals(PaymentStatus.REFUND_PENDING, completedPayment.getStatus());
        }

        @Test
        @DisplayName("should complete partial refund")
        void shouldCompletePartialRefund() {
            Money refundAmount = Money.of(new BigDecimal("50.00"), "USD");
            completedPayment.initiateRefund(refundAmount);

            completedPayment.completeRefund(refundAmount);

            assertEquals(PaymentStatus.PARTIALLY_REFUNDED, completedPayment.getStatus());
            assertEquals(refundAmount, completedPayment.getRefundedAmount());
        }

        @Test
        @DisplayName("should complete full refund")
        void shouldCompleteFullRefund() {
            completedPayment.initiateRefund(amount);

            completedPayment.completeRefund(amount);

            assertEquals(PaymentStatus.REFUNDED, completedPayment.getStatus());
            assertEquals(amount, completedPayment.getRefundedAmount());
        }

        @Test
        @DisplayName("should allow multiple partial refunds")
        void shouldAllowMultiplePartialRefunds() {
            Money firstRefund = Money.of(new BigDecimal("30.00"), "USD");
            Money secondRefund = Money.of(new BigDecimal("30.00"), "USD");

            completedPayment.initiateRefund(firstRefund);
            completedPayment.completeRefund(firstRefund);

            assertTrue(completedPayment.canRefund(secondRefund));

            completedPayment.initiateRefund(secondRefund);
            completedPayment.completeRefund(secondRefund);

            assertEquals(PaymentStatus.PARTIALLY_REFUNDED, completedPayment.getStatus());
            assertEquals(Money.of(new BigDecimal("60.00"), "USD"), completedPayment.getRefundedAmount());
        }

        @Test
        @DisplayName("should not allow refund exceeding original amount")
        void shouldNotAllowRefundExceedingOriginalAmount() {
            Money excessRefund = Money.of(new BigDecimal("150.00"), "USD");

            assertThrows(InvalidPaymentOperationException.class, () ->
                    completedPayment.initiateRefund(excessRefund));
        }

        @Test
        @DisplayName("should not allow refund for pending payment")
        void shouldNotAllowRefundForPendingPayment() {
            Payment pendingPayment = Payment.create("order-123", "customer-456", amount);
            Money refundAmount = Money.of(new BigDecimal("50.00"), "USD");

            assertFalse(pendingPayment.canRefund(refundAmount));
            assertThrows(InvalidPaymentOperationException.class, () ->
                    pendingPayment.initiateRefund(refundAmount));
        }

        @Test
        @DisplayName("should calculate refundable amount correctly")
        void shouldCalculateRefundableAmountCorrectly() {
            Money firstRefund = Money.of(new BigDecimal("40.00"), "USD");
            completedPayment.initiateRefund(firstRefund);
            completedPayment.completeRefund(firstRefund);

            Money refundable = completedPayment.getRefundableAmount();

            assertEquals(Money.of(new BigDecimal("60.00"), "USD"), refundable);
        }
    }
}
