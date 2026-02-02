package com.example.payment.infrastructure.adapter.gateway;

import com.example.payment.application.port.output.NotificationService;
import com.example.payment.domain.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationService.class);

    @Override
    public void notifyPaymentCompleted(Payment payment) {
        log.info("Payment completed: {} for order {}", payment.getId(), payment.getOrderId());
    }

    @Override
    public void notifyPaymentFailed(Payment payment) {
        log.warn("Payment failed: {} for order {}", payment.getId(), payment.getOrderId());
    }

    @Override
    public void notifyPaymentRefunded(Payment payment) {
        log.info("Payment refunded: {} for order {}", payment.getId(), payment.getOrderId());
    }
}
