package com.example.payment.application.port.output;

import com.example.payment.domain.entity.Payment;

public interface NotificationService {
    void notifyPaymentCompleted(Payment payment);
    void notifyPaymentFailed(Payment payment);
    void notifyPaymentRefunded(Payment payment);
}
