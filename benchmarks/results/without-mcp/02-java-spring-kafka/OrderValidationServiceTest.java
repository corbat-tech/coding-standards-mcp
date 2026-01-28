package com.orders.service;

import com.orders.event.OrderCreatedEvent;
import com.orders.exception.OrderValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class OrderValidationServiceTest {

    private OrderValidationService validationService;
    private OrderCreatedEvent validEvent;

    @BeforeEach
    void setUp() {
        validationService = new OrderValidationService();

        OrderCreatedEvent.OrderItem item = new OrderCreatedEvent.OrderItem(
                "PROD-001", "Test Product", 2, new BigDecimal("29.99")
        );
        validEvent = new OrderCreatedEvent(
                "ORDER-123",
                "CUST-456",
                Arrays.asList(item),
                new BigDecimal("59.98"),
                Instant.now()
        );
    }

    @Test
    void validate_ValidEvent_DoesNotThrow() {
        assertDoesNotThrow(() -> validationService.validate(validEvent));
    }

    @Test
    void validate_NullOrderId_ThrowsException() {
        validEvent.setOrderId(null);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Order ID is required", exception.getMessage());
    }

    @Test
    void validate_EmptyOrderId_ThrowsException() {
        validEvent.setOrderId("   ");

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Order ID is required", exception.getMessage());
    }

    @Test
    void validate_NullCustomerId_ThrowsException() {
        validEvent.setCustomerId(null);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Customer ID is required", exception.getMessage());
    }

    @Test
    void validate_EmptyItems_ThrowsException() {
        validEvent.setItems(Collections.emptyList());

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Order must contain at least one item", exception.getMessage());
    }

    @Test
    void validate_NullItems_ThrowsException() {
        validEvent.setItems(null);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Order must contain at least one item", exception.getMessage());
    }

    @Test
    void validate_ZeroTotalAmount_ThrowsException() {
        validEvent.setTotalAmount(BigDecimal.ZERO);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Total amount must be greater than zero", exception.getMessage());
    }

    @Test
    void validate_NegativeTotalAmount_ThrowsException() {
        validEvent.setTotalAmount(new BigDecimal("-10.00"));

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Total amount must be greater than zero", exception.getMessage());
    }

    @Test
    void validate_ItemWithZeroQuantity_ThrowsException() {
        OrderCreatedEvent.OrderItem invalidItem = new OrderCreatedEvent.OrderItem(
                "PROD-001", "Test", 0, new BigDecimal("10.00")
        );
        validEvent.setItems(Arrays.asList(invalidItem));

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Item quantity must be greater than zero", exception.getMessage());
    }

    @Test
    void validate_ItemWithNegativePrice_ThrowsException() {
        OrderCreatedEvent.OrderItem invalidItem = new OrderCreatedEvent.OrderItem(
                "PROD-001", "Test", 1, new BigDecimal("-5.00")
        );
        validEvent.setItems(Arrays.asList(invalidItem));

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> validationService.validate(validEvent)
        );
        assertEquals("Item price must be greater than zero", exception.getMessage());
    }
}
