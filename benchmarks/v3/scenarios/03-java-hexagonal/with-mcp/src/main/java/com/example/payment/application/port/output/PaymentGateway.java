package com.example.payment.application.port.output;

import com.example.payment.domain.valueobject.Money;

public interface PaymentGateway {
    GatewayResponse charge(String cardToken, Money amount);
    GatewayResponse refund(String transactionId, Money amount);

    record GatewayResponse(boolean success, String transactionId, String errorMessage) {
        public static GatewayResponse success(String transactionId) {
            return new GatewayResponse(true, transactionId, null);
        }

        public static GatewayResponse failure(String errorMessage) {
            return new GatewayResponse(false, null, errorMessage);
        }
    }
}
