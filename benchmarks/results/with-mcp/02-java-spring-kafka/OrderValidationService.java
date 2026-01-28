package com.orders.domain.service;

import com.orders.domain.event.OrderCreatedEvent;
import com.orders.domain.exception.OrderValidationException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class OrderValidationService {

    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("10000.00");
    private static final int MAX_ITEMS_PER_ORDER = 100;

    public void validate(OrderCreatedEvent event) {
        validateOrderId(event.orderId());
        validateCustomerId(event.customerId());
        validateItems(event);
        validateTotalAmount(event.totalAmount());
    }

    private void validateOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new OrderValidationException("Order ID is required");
        }
    }

    private void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new OrderValidationException("Customer ID is required");
        }
    }

    private void validateItems(OrderCreatedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            throw new OrderValidationException("Order must have at least one item");
        }
        if (event.items().size() > MAX_ITEMS_PER_ORDER) {
            throw new OrderValidationException("Order exceeds maximum items: " + MAX_ITEMS_PER_ORDER);
        }
        event.items().forEach(this::validateItem);
    }

    private void validateItem(OrderCreatedEvent.OrderItem item) {
        if (item.quantity() == null || item.quantity() <= 0) {
            throw new OrderValidationException("Item quantity must be positive");
        }
        if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("Item price must be positive");
        }
    }

    private void validateTotalAmount(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderValidationException("Total amount must be positive");
        }
        if (totalAmount.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new OrderValidationException("Order exceeds maximum amount: " + MAX_ORDER_AMOUNT);
        }
    }
}
