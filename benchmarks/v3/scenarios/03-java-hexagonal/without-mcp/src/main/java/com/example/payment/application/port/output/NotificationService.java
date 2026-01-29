package com.example.payment.application.port.output;

import com.example.payment.domain.entity.Payment;

/**
 * Output port for notification operations.
 * Defines the contract for sending payment notifications.
 */
public interface NotificationService {

    /**
     * Send payment confirmation notification.
     *
     * @param payment the payment
     * @param customerEmail the customer email
     */
    void sendPaymentConfirmation(Payment payment, String customerEmail);

    /**
     * Send payment failure notification.
     *
     * @param payment the payment
     * @param customerEmail the customer email
     */
    void sendPaymentFailure(Payment payment, String customerEmail);

    /**
     * Send refund confirmation notification.
     *
     * @param payment the payment
     * @param customerEmail the customer email
     */
    void sendRefundConfirmation(Payment payment, String customerEmail);

    /**
     * Send refund failure notification.
     *
     * @param payment the payment
     * @param customerEmail the customer email
     * @param reason the failure reason
     */
    void sendRefundFailure(Payment payment, String customerEmail, String reason);
}
