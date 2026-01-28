package com.orders.service;

import com.orders.event.OrderCreatedEvent;
import com.orders.exception.OrderValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderValidationService {

    public void validate(OrderCreatedEvent event) {
        if (event.getOrderId() == null || event.getOrderId().trim().isEmpty()) {
            throw new OrderValidationException("Order ID is required");
        }

        if (event.getCustomerId() == null || event.getCustomerId().trim().isEmpty()) {
            throw new OrderValidationException("Customer ID is required");
        }

        if (event.getItems() == null || event.getItems().isEmpty()) {
            throw new OrderValidationException("Order must contain at least one item");
        }

        if (event.getTotalAmount() == null || event.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("Total amount must be greater than zero");
        }

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new OrderValidationException("Item quantity must be greater than zero");
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException("Item price must be greater than zero");
            }
        }
    }
}
