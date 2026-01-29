package com.payment.domain.port.output;

import com.payment.domain.entity.Payment;

/**
 * Output port for sending notifications.
 * Defines the contract for notification delivery.
 */
public interface NotificationService {

    /**
     * Send notification when payment is successful.
     *
     * @param payment the completed payment
     */
    void notifyPaymentSuccess(Payment payment);

    /**
     * Send notification when payment fails.
     *
     * @param payment the failed payment
     * @param reason the failure reason
     */
    void notifyPaymentFailure(Payment payment, String reason);

    /**
     * Send notification when refund is processed.
     *
     * @param payment the refunded payment
     */
    void notifyRefundProcessed(Payment payment);
}
