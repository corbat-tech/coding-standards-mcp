package com.orders.service;

import com.orders.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        logger.info("Sending order confirmation notification for order: {} to customer: {}",
                    event.getOrderId(), event.getCustomerId());

        // In a real implementation, this would send email/SMS/push notification
        // For now, just logging the action
        logger.info("Order confirmation sent successfully for order: {}", event.getOrderId());
    }

    public void sendOrderFailureNotification(String orderId, String customerId, String reason) {
        logger.info("Sending order failure notification for order: {} to customer: {}",
                    orderId, customerId);
        logger.info("Failure reason: {}", reason);
    }
}
