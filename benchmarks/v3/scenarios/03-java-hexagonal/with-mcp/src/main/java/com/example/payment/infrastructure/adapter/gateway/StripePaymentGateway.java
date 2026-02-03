package com.example.payment.infrastructure.adapter.gateway;

import com.example.payment.application.port.output.PaymentGateway;
import com.example.payment.domain.valueobject.Money;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StripePaymentGateway implements PaymentGateway {

    @Override
    public GatewayResponse charge(String cardToken, Money amount) {
        if (cardToken == null || cardToken.isBlank()) {
            return GatewayResponse.failure("Invalid card token");
        }
        if (cardToken.startsWith("fail_")) {
            return GatewayResponse.failure("Card declined");
        }
        String transactionId = "txn_" + UUID.randomUUID().toString().substring(0, 8);
        return GatewayResponse.success(transactionId);
    }

    @Override
    public GatewayResponse refund(String transactionId, Money amount) {
        if (transactionId == null || transactionId.isBlank()) {
            return GatewayResponse.failure("Invalid transaction ID");
        }
        String refundId = "ref_" + UUID.randomUUID().toString().substring(0, 8);
        return GatewayResponse.success(refundId);
    }
}
