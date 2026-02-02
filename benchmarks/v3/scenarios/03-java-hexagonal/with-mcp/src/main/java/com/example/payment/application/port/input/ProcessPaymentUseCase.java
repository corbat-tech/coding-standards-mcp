package com.example.payment.application.port.input;

import com.example.payment.domain.entity.Payment;
import java.math.BigDecimal;

public interface ProcessPaymentUseCase {
    Payment process(String orderId, BigDecimal amount, String currency, String cardToken);
}
