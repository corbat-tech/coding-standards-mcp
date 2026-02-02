package com.example.payment.application.port.input;

import com.example.payment.domain.entity.Payment;

public interface GetPaymentStatusUseCase {
    Payment getStatus(String paymentId);
}
