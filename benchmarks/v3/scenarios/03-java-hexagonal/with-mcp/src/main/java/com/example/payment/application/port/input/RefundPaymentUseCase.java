package com.example.payment.application.port.input;

import com.example.payment.domain.entity.Payment;

public interface RefundPaymentUseCase {
    Payment refund(String paymentId);
}
