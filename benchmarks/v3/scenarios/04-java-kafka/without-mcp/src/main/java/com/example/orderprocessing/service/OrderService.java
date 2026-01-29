package com.example.orderprocessing.service;

import com.example.orderprocessing.domain.entity.Order;
import com.example.orderprocessing.domain.entity.OrderLineItem;
import com.example.orderprocessing.domain.event.OrderCreatedEvent;
import com.example.orderprocessing.domain.repository.OrderRepository;
import com.example.orderprocessing.producer.OrderEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for order management and publishing order events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    /**
     * Create a new order and publish OrderCreatedEvent
     */
    @Transactional
    public Order createOrder(String customerId, List<CreateOrderItemRequest> items) {
        log.info("Creating order for customer: {}", customerId);

        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .status(Order.OrderStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemRequest : items) {
            OrderLineItem lineItem = OrderLineItem.builder()
                    .productId(itemRequest.productId())
                    .productName(itemRequest.productName())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .build();

            order.addLineItem(lineItem);
            totalAmount = totalAmount.add(lineItem.getSubtotal());
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with ID: {}", savedOrder.getId());

        // Publish event
        OrderCreatedEvent event = createOrderEvent(savedOrder);
        orderEventProducer.publishOrderCreatedEvent(event);

        return savedOrder;
    }

    /**
     * Get order by ID
     */
    public Optional<Order> getOrder(String orderId) {
        return orderRepository.findById(orderId);
    }

    /**
     * Get all orders for a customer
     */
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Update order status
     */
    @Transactional
    public Order updateOrderStatus(String orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    /**
     * Cancel an order
     */
    @Transactional
    public Order cancelOrder(String orderId) {
        return updateOrderStatus(orderId, Order.OrderStatus.CANCELLED);
    }

    private OrderCreatedEvent createOrderEvent(Order order) {
        List<OrderCreatedEvent.OrderItem> eventItems = order.getLineItems().stream()
                .map(item -> OrderCreatedEvent.OrderItem.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderCreatedEvent.create(
                order.getId(),
                order.getCustomerId(),
                eventItems,
                order.getTotalAmount()
        );
    }

    /**
     * Request object for creating order items
     */
    public record CreateOrderItemRequest(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
