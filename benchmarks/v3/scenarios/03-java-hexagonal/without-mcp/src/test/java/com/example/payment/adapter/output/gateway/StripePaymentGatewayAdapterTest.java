package com.example.payment.adapter.output.gateway;

import com.example.payment.application.port.output.PaymentGateway.PaymentGatewayRequest;
import com.example.payment.application.port.output.PaymentGateway.PaymentGatewayResponse;
import com.example.payment.application.port.output.PaymentGateway.RefundGatewayRequest;
import com.example.payment.application.port.output.PaymentGateway.RefundGatewayResponse;
import com.example.payment.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StripePaymentGatewayAdapter.
 */
@DisplayName("StripePaymentGatewayAdapter")
class StripePaymentGatewayAdapterTest {

    private StripePaymentGatewayAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StripePaymentGatewayAdapter("sk_test_mock", 30000);
        adapter.resetSimulation();
    }

    @Nested
    @DisplayName("Process Payment")
    class ProcessPayment {

        @Test
        @DisplayName("should process payment successfully")
        void shouldProcessPaymentSuccessfully() {
            PaymentGatewayRequest request = new PaymentGatewayRequest(
                    "payment-123",
                    Money.of(new BigDecimal("100.00"), "USD"),
                    "valid_card_token",
                    "customer-456",
                    "order-789"
            );

            PaymentGatewayResponse response = adapter.processPayment(request);

            assertTrue(response.success());
            assertNotNull(response.transactionId());
            assertTrue(response.transactionId().startsWith("ch_"));
            assertNull(response.errorCode());
            assertNull(response.errorMessage());
        }

        @Test
        @DisplayName("should fail with declined card test number")
        void shouldFailWithDeclinedCardTestNumber() {
            PaymentGatewayRequest request = new PaymentGatewayRequest(
                    "payment-123",
                    Money.of(new BigDecimal("100.00"), "USD"),
                    "4000000000000002",
                    "customer-456",
                    "order-789"
            );

            PaymentGatewayResponse response = adapter.processPayment(request);

            assertFalse(response.success());
            assertNull(response.transactionId());
            assertEquals("card_declined", response.errorCode());
        }

        @Test
        @DisplayName("should fail with insufficient funds test number")
        void shouldFailWithInsufficientFundsTestNumber() {
            PaymentGatewayRequest request = new PaymentGatewayRequest(
                    "payment-123",
                    Money.of(new BigDecimal("100.00"), "USD"),
                    "4000000000009995",
                    "customer-456",
                    "order-789"
            );

            PaymentGatewayResponse response = adapter.processPayment(request);

            assertFalse(response.success());
            assertEquals("insufficient_funds", response.errorCode());
        }

        @Test
        @DisplayName("should fail when simulation is enabled")
        void shouldFailWhenSimulationIsEnabled() {
            adapter.setSimulateFailure(true);
            adapter.setSimulatedError("test_error", "Test error message");

            PaymentGatewayRequest request = new PaymentGatewayRequest(
                    "payment-123",
                    Money.of(new BigDecimal("100.00"), "USD"),
                    "valid_card_token",
                    "customer-456",
                    "order-789"
            );

            PaymentGatewayResponse response = adapter.processPayment(request);

            assertFalse(response.success());
            assertEquals("test_error", response.errorCode());
            assertEquals("Test error message", response.errorMessage());
        }
    }

    @Nested
    @DisplayName("Process Refund")
    class ProcessRefund {

        @Test
        @DisplayName("should process refund successfully")
        void shouldProcessRefundSuccessfully() {
            RefundGatewayRequest request = new RefundGatewayRequest(
                    "ch_original123456789012345678",
                    Money.of(new BigDecimal("50.00"), "USD"),
                    "Customer request"
            );

            RefundGatewayResponse response = adapter.processRefund(request);

            assertTrue(response.success());
            assertNotNull(response.refundTransactionId());
            assertTrue(response.refundTransactionId().startsWith("re_"));
            assertNull(response.errorCode());
        }

        @Test
        @DisplayName("should fail with invalid transaction ID")
        void shouldFailWithInvalidTransactionId() {
            RefundGatewayRequest request = new RefundGatewayRequest(
                    "invalid_txn_id",
                    Money.of(new BigDecimal("50.00"), "USD"),
                    "Customer request"
            );

            RefundGatewayResponse response = adapter.processRefund(request);

            assertFalse(response.success());
            assertEquals("invalid_charge", response.errorCode());
        }

        @Test
        @DisplayName("should fail with null transaction ID")
        void shouldFailWithNullTransactionId() {
            RefundGatewayRequest request = new RefundGatewayRequest(
                    null,
                    Money.of(new BigDecimal("50.00"), "USD"),
                    "Customer request"
            );

            RefundGatewayResponse response = adapter.processRefund(request);

            assertFalse(response.success());
            assertEquals("invalid_charge", response.errorCode());
        }

        @Test
        @DisplayName("should fail when simulation is enabled")
        void shouldFailWhenSimulationIsEnabled() {
            adapter.setSimulateFailure(true);
            adapter.setSimulatedError("refund_error", "Refund failed");

            RefundGatewayRequest request = new RefundGatewayRequest(
                    "ch_original123456789012345678",
                    Money.of(new BigDecimal("50.00"), "USD"),
                    "Customer request"
            );

            RefundGatewayResponse response = adapter.processRefund(request);

            assertFalse(response.success());
            assertEquals("refund_error", response.errorCode());
        }
    }

    @Test
    @DisplayName("should reset simulation state")
    void shouldResetSimulationState() {
        adapter.setSimulateFailure(true);
        adapter.setSimulatedError("custom_error", "Custom message");

        adapter.resetSimulation();

        PaymentGatewayRequest request = new PaymentGatewayRequest(
                "payment-123",
                Money.of(new BigDecimal("100.00"), "USD"),
                "valid_card_token",
                "customer-456",
                "order-789"
        );

        PaymentGatewayResponse response = adapter.processPayment(request);
        assertTrue(response.success());
    }
}
