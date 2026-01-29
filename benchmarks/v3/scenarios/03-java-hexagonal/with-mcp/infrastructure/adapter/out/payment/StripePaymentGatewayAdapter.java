package com.payment.infrastructure.adapter.out.payment;

import com.payment.domain.port.output.PaymentGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stripe payment gateway adapter (mock implementation).
 * Secondary/driven adapter for payment processing.
 */
@Component
public class StripePaymentGatewayAdapter implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGatewayAdapter.class);

    @Value("${stripe.api.key:sk_test_mock}")
    private String apiKey;

    @Value("${stripe.simulate.failure:false}")
    private boolean simulateFailure;

    @Override
    public ChargeResult charge(ChargeRequest request) {
        log.info("Processing Stripe charge for order: {}, amount: {}",
            request.orderId(), request.amount());

        if (simulateFailure) {
            return ChargeResult.failure("card_declined", "Your card was declined");
        }

        // Simulate Stripe API call
        String transactionId = "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        log.info("Stripe charge successful: {}", transactionId);
        return ChargeResult.success(transactionId);
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        log.info("Processing Stripe refund for transaction: {}, amount: {}",
            request.originalTransactionId(), request.amount());

        if (simulateFailure) {
            return RefundResult.failure("refund_failed", "Refund could not be processed");
        }

        // Simulate Stripe API call
        String refundId = "re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        log.info("Stripe refund successful: {}", refundId);
        return RefundResult.success(refundId);
    }
}
