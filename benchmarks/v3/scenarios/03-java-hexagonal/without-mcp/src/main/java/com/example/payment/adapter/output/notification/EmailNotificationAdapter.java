package com.example.payment.adapter.output.notification;

import com.example.payment.application.port.output.NotificationService;
import com.example.payment.domain.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Email notification adapter.
 * In a real implementation, this would integrate with an email service.
 */
@Component
public class EmailNotificationAdapter implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationAdapter.class);

    private final boolean enabled;
    private final String fromEmail;

    // For testing: track sent notifications
    private int paymentConfirmationsSent = 0;
    private int paymentFailuresSent = 0;
    private int refundConfirmationsSent = 0;
    private int refundFailuresSent = 0;

    public EmailNotificationAdapter(
            @Value("${notification.service.enabled:true}") boolean enabled,
            @Value("${notification.service.email.from:noreply@example.com}") String fromEmail) {
        this.enabled = enabled;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendPaymentConfirmation(Payment payment, String customerEmail) {
        if (!enabled || customerEmail == null || customerEmail.isBlank()) {
            log.debug("Skipping payment confirmation notification: enabled={}, email={}",
                    enabled, customerEmail);
            return;
        }

        log.info("Sending payment confirmation email: from={}, to={}, paymentId={}, amount={}",
                fromEmail, customerEmail, payment.getId(), payment.getAmount());

        // In a real implementation, this would send an actual email
        // For now, we just log and track
        String subject = "Payment Confirmation - Order " + payment.getOrderId();
        String body = buildPaymentConfirmationBody(payment);

        simulateEmailSend(customerEmail, subject, body);
        paymentConfirmationsSent++;
    }

    @Override
    public void sendPaymentFailure(Payment payment, String customerEmail) {
        if (!enabled || customerEmail == null || customerEmail.isBlank()) {
            log.debug("Skipping payment failure notification: enabled={}, email={}",
                    enabled, customerEmail);
            return;
        }

        log.info("Sending payment failure email: from={}, to={}, paymentId={}, reason={}",
                fromEmail, customerEmail, payment.getId(), payment.getFailureReason());

        String subject = "Payment Failed - Order " + payment.getOrderId();
        String body = buildPaymentFailureBody(payment);

        simulateEmailSend(customerEmail, subject, body);
        paymentFailuresSent++;
    }

    @Override
    public void sendRefundConfirmation(Payment payment, String customerEmail) {
        if (!enabled || customerEmail == null || customerEmail.isBlank()) {
            log.debug("Skipping refund confirmation notification: enabled={}, email={}",
                    enabled, customerEmail);
            return;
        }

        log.info("Sending refund confirmation email: from={}, to={}, paymentId={}, refundedAmount={}",
                fromEmail, customerEmail, payment.getId(), payment.getRefundedAmount());

        String subject = "Refund Processed - Order " + payment.getOrderId();
        String body = buildRefundConfirmationBody(payment);

        simulateEmailSend(customerEmail, subject, body);
        refundConfirmationsSent++;
    }

    @Override
    public void sendRefundFailure(Payment payment, String customerEmail, String reason) {
        if (!enabled || customerEmail == null || customerEmail.isBlank()) {
            log.debug("Skipping refund failure notification: enabled={}, email={}",
                    enabled, customerEmail);
            return;
        }

        log.info("Sending refund failure email: from={}, to={}, paymentId={}, reason={}",
                fromEmail, customerEmail, payment.getId(), reason);

        String subject = "Refund Failed - Order " + payment.getOrderId();
        String body = buildRefundFailureBody(payment, reason);

        simulateEmailSend(customerEmail, subject, body);
        refundFailuresSent++;
    }

    private String buildPaymentConfirmationBody(Payment payment) {
        return String.format("""
                Dear Customer,

                Your payment has been processed successfully.

                Order ID: %s
                Payment ID: %s
                Amount: %s
                Transaction ID: %s

                Thank you for your purchase!
                """,
                payment.getOrderId(),
                payment.getId(),
                payment.getAmount(),
                payment.getGatewayTransactionId());
    }

    private String buildPaymentFailureBody(Payment payment) {
        return String.format("""
                Dear Customer,

                Unfortunately, your payment could not be processed.

                Order ID: %s
                Payment ID: %s
                Amount: %s
                Reason: %s

                Please try again or contact support.
                """,
                payment.getOrderId(),
                payment.getId(),
                payment.getAmount(),
                payment.getFailureReason());
    }

    private String buildRefundConfirmationBody(Payment payment) {
        return String.format("""
                Dear Customer,

                Your refund has been processed successfully.

                Order ID: %s
                Payment ID: %s
                Original Amount: %s
                Refunded Amount: %s

                The refund will appear in your account within 5-10 business days.
                """,
                payment.getOrderId(),
                payment.getId(),
                payment.getAmount(),
                payment.getRefundedAmount());
    }

    private String buildRefundFailureBody(Payment payment, String reason) {
        return String.format("""
                Dear Customer,

                Unfortunately, your refund could not be processed.

                Order ID: %s
                Payment ID: %s
                Reason: %s

                Please contact support for assistance.
                """,
                payment.getOrderId(),
                payment.getId(),
                reason);
    }

    private void simulateEmailSend(String to, String subject, String body) {
        // Simulate email sending delay
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("Email sent to {}: {}", to, subject);
    }

    // Methods for testing
    public int getPaymentConfirmationsSent() {
        return paymentConfirmationsSent;
    }

    public int getPaymentFailuresSent() {
        return paymentFailuresSent;
    }

    public int getRefundConfirmationsSent() {
        return refundConfirmationsSent;
    }

    public int getRefundFailuresSent() {
        return refundFailuresSent;
    }

    public void resetCounters() {
        paymentConfirmationsSent = 0;
        paymentFailuresSent = 0;
        refundConfirmationsSent = 0;
        refundFailuresSent = 0;
    }
}
