package com.example.payment.application.port.output;

import com.example.payment.domain.entity.Payment;
import com.example.payment.domain.valueobject.PaymentId;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(PaymentId id);
}
