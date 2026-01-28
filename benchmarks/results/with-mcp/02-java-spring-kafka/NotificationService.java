package com.orders.domain.service;

import com.orders.domain.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        log.info("Sending order confirmation to customer: {}", event.customerId());

        String message = buildConfirmationMessage(event);
        sendNotification(event.customerId(), message);

        log.info("Order confirmation sent for order: {}", event.orderId());
    }

    private String buildConfirmationMessage(OrderCreatedEvent event) {
        return String.format(
            "Your order %s has been received. Total: $%s",
            event.orderId(),
            event.totalAmount()
        );
    }

    private void sendNotification(String customerId, String message) {
        // In production, would integrate with notification system (email, SMS, push)
        log.debug("Notification to {}: {}", customerId, message);
    }

    public void sendOrderFailureNotification(String orderId, String customerId, String reason) {
        log.info("Sending failure notification to customer: {} for order: {}",
            customerId, orderId);
        // Send failure notification
    }
}
