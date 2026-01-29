package com.example.payment.adapter.output.gateway;

import com.example.payment.application.port.output.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock Stripe payment gateway adapter.
 * In a real implementation, this would integrate with the Stripe API.
 */
@Component
public class StripePaymentGatewayAdapter implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGatewayAdapter.class);

    private final String apiKey;
    private final int timeout;

    // For testing: allows simulating failures
    private boolean simulateFailure = false;
    private String simulatedErrorCode = "card_declined";
    private String simulatedErrorMessage = "Your card was declined";

    public StripePaymentGatewayAdapter(
            @Value("${payment.gateway.stripe.api-key:sk_test_mock}") String apiKey,
            @Value("${payment.gateway.stripe.timeout:30000}") int timeout) {
        this.apiKey = apiKey;
        this.timeout = timeout;
    }

    @Override
    public PaymentGatewayResponse processPayment(PaymentGatewayRequest request) {
        log.info("Processing payment through Stripe gateway: paymentId={}, amount={} {}",
                request.paymentId(), request.amount().getAmount(), request.amount().getCurrencyCode());

        // Simulate API call delay
        simulateApiLatency();

        // Check for simulated failure
        if (simulateFailure) {
            log.warn("Simulated payment failure for paymentId={}", request.paymentId());
            return PaymentGatewayResponse.failure(simulatedErrorCode, simulatedErrorMessage);
        }

        // Simulate specific card number failures for testing
        if ("4000000000000002".equals(request.paymentMethod())) {
            log.warn("Test card declined for paymentId={}", request.paymentId());
            return PaymentGatewayResponse.failure("card_declined", "Your card was declined.");
        }

        if ("4000000000009995".equals(request.paymentMethod())) {
            log.warn("Test card insufficient funds for paymentId={}", request.paymentId());
            return PaymentGatewayResponse.failure("insufficient_funds", "Your card has insufficient funds.");
        }

        // Successful payment
        String transactionId = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Payment successful: paymentId={}, transactionId={}", request.paymentId(), transactionId);

        return PaymentGatewayResponse.success(transactionId);
    }

    @Override
    public RefundGatewayResponse processRefund(RefundGatewayRequest request) {
        log.info("Processing refund through Stripe gateway: originalTxn={}, amount={} {}",
                request.originalTransactionId(), request.amount().getAmount(), request.amount().getCurrencyCode());

        // Simulate API call delay
        simulateApiLatency();

        // Check for simulated failure
        if (simulateFailure) {
            log.warn("Simulated refund failure for originalTxn={}", request.originalTransactionId());
            return RefundGatewayResponse.failure(simulatedErrorCode, simulatedErrorMessage);
        }

        // Validate original transaction ID format
        if (request.originalTransactionId() == null || !request.originalTransactionId().startsWith("ch_")) {
            log.error("Invalid original transaction ID: {}", request.originalTransactionId());
            return RefundGatewayResponse.failure("invalid_charge", "No such charge: " + request.originalTransactionId());
        }

        // Successful refund
        String refundTransactionId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        log.info("Refund successful: originalTxn={}, refundTxn={}",
                request.originalTransactionId(), refundTransactionId);

        return RefundGatewayResponse.success(refundTransactionId);
    }

    private void simulateApiLatency() {
        try {
            // Simulate network latency (50-200ms)
            Thread.sleep((long) (50 + Math.random() * 150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Methods for testing
    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void setSimulatedError(String errorCode, String errorMessage) {
        this.simulatedErrorCode = errorCode;
        this.simulatedErrorMessage = errorMessage;
    }

    public void resetSimulation() {
        this.simulateFailure = false;
        this.simulatedErrorCode = "card_declined";
        this.simulatedErrorMessage = "Your card was declined";
    }
}
