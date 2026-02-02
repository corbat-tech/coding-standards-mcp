package com.example.saga.application.port;

import com.example.saga.domain.valueobject.Money;
import com.example.saga.domain.valueobject.OrderId;

public interface PaymentService {
    String processPayment(OrderId orderId, String customerId, Money amount);
    void refundPayment(String paymentId);
}
