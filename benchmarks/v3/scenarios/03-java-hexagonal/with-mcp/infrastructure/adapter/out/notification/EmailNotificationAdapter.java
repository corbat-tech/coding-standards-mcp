package com.payment.infrastructure.adapter.out.notification;

import com.payment.domain.entity.Payment;
import com.payment.domain.port.output.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Email notification adapter.
 * Secondary/driven adapter for sending notifications.
 */
@Component
public class EmailNotificationAdapter implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    @Override
    public void notifyPaymentSuccess(Payment payment) {
        log.info("Sending payment success email to customer: {}, payment: {}, amount: {}",
            payment.getCustomerId(), payment.getId(), payment.getAmount());
        // In production: integrate with email service (SendGrid, AWS SES, etc.)
    }

    @Override
    public void notifyPaymentFailure(Payment payment, String reason) {
        log.info("Sending payment failure email to customer: {}, payment: {}, reason: {}",
            payment.getCustomerId(), payment.getId(), reason);
    }

    @Override
    public void notifyRefundProcessed(Payment payment) {
        log.info("Sending refund notification to customer: {}, payment: {}, refunded: {}",
            payment.getCustomerId(), payment.getId(), payment.getRefundedAmount());
    }
}
