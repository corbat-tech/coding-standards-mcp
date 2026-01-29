package com.example.order.application.service;

import com.example.order.application.port.in.PlaceOrderUseCase;
import com.example.order.application.port.out.OrderEventPublisher;
import com.example.order.domain.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Application service for order operations.
 * Implements the PlaceOrderUseCase input port.
 */
@Service
public class OrderService implements PlaceOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        log.info("Placing order for customer: {}", command.customerId());

        try {
            String orderId = generateOrderId();
            OrderCreatedEvent event = createOrderEvent(orderId, command);

            eventPublisher.publishOrderCreatedSync(event);

            log.info("Order placed successfully: orderId={}, eventId={}",
                orderId, event.eventId());
            return PlaceOrderResult.success(orderId);

        } catch (Exception e) {
            log.error("Failed to place order for customer: {}", command.customerId(), e);
            return PlaceOrderResult.failure("Failed to place order: " + e.getMessage());
        }
    }

    private String generateOrderId() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderCreatedEvent createOrderEvent(String orderId, PlaceOrderCommand command) {
        var eventItems = command.items().stream()
            .map(item -> new OrderCreatedEvent.OrderItem(
                item.productId(),
                item.productName(),
                item.quantity(),
                item.unitPrice()
            ))
            .toList();

        BigDecimal total = command.items().stream()
            .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderCreatedEvent.create(orderId, command.customerId(), eventItems, total);
    }
}
