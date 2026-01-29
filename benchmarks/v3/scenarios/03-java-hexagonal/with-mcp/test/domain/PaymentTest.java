package com.payment.domain.entity;

import com.payment.domain.exception.InvalidPaymentStateException;
import com.payment.domain.valueobject.Money;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment Entity")
class PaymentTest {

    private static final String ORDER_ID = "order-123";
    private static final String CUSTOMER_ID = "customer-456";
    private Money validAmount;

    @BeforeEach
    void setUp() {
        validAmount = Money.usd(new BigDecimal("100.00"));
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create payment with valid data")
        void shouldCreatePaymentWithValidData() {
            Payment payment = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);

            assertThat(payment.getId()).isNotNull();
            assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(payment.getCustomerId()).isEqualTo(CUSTOMER_ID);
            assertThat(payment.getAmount()).isEqualTo(validAmount);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getRefundedAmount().getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            Money zeroAmount = Money.usd(BigDecimal.ZERO);

            assertThatThrownBy(() -> Payment.create(ORDER_ID, CUSTOMER_ID, zeroAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment amount must be positive");
        }

        @Test
        @DisplayName("should reject null order ID")
        void shouldRejectNullOrderId() {
            assertThatThrownBy(() -> Payment.create(null, CUSTOMER_ID, validAmount))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition from PENDING to PROCESSING")
        void shouldTransitionFromPendingToProcessing() {
            Payment payment = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);

            payment.markAsProcessing();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from PROCESSING to COMPLETED")
        void shouldTransitionFromProcessingToCompleted() {
            Payment payment = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);
            payment.markAsProcessing();

            payment.markAsCompleted("txn_123");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getGatewayTransactionId()).isEqualTo("txn_123");
        }

        @Test
        @DisplayName("should transition from PROCESSING to FAILED")
        void shouldTransitionFromProcessingToFailed() {
            Payment payment = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);
            payment.markAsProcessing();

            payment.markAsFailed();

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should throw when invalid transition attempted")
        void shouldThrowWhenInvalidTransitionAttempted() {
            Payment payment = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);

            assertThatThrownBy(() -> payment.markAsCompleted("txn_123"))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("Cannot transition from PENDING to COMPLETED");
        }
    }

    @Nested
    @DisplayName("Refunds")
    class Refunds {

        private Payment completedPayment;

        @BeforeEach
        void setUp() {
            completedPayment = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);
            completedPayment.markAsProcessing();
            completedPayment.markAsCompleted("txn_123");
        }

        @Test
        @DisplayName("should process full refund")
        void shouldProcessFullRefund() {
            completedPayment.refund(validAmount);

            assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(completedPayment.getRefundedAmount()).isEqualTo(validAmount);
        }

        @Test
        @DisplayName("should process partial refund")
        void shouldProcessPartialRefund() {
            Money partialAmount = Money.usd(new BigDecimal("30.00"));

            completedPayment.refund(partialAmount);

            assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
            assertThat(completedPayment.getRefundedAmount()).isEqualTo(partialAmount);
        }

        @Test
        @DisplayName("should allow multiple partial refunds")
        void shouldAllowMultiplePartialRefunds() {
            Money firstRefund = Money.usd(new BigDecimal("30.00"));
            Money secondRefund = Money.usd(new BigDecimal("70.00"));

            completedPayment.refund(firstRefund);
            completedPayment.refund(secondRefund);

            assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(completedPayment.getRefundedAmount()).isEqualTo(validAmount);
        }

        @Test
        @DisplayName("should reject refund exceeding original amount")
        void shouldRejectRefundExceedingOriginalAmount() {
            Money excessiveAmount = Money.usd(new BigDecimal("150.00"));

            assertThatThrownBy(() -> completedPayment.refund(excessiveAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds original");
        }

        @Test
        @DisplayName("should not allow refund on pending payment")
        void shouldNotAllowRefundOnPendingPayment() {
            Payment pending = Payment.create(ORDER_ID, CUSTOMER_ID, validAmount);

            assertThatThrownBy(() -> pending.refund(validAmount))
                .isInstanceOf(InvalidPaymentStateException.class);
        }
    }
}
