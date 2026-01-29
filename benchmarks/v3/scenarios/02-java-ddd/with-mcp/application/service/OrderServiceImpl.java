package application.service;

import application.command.AddItemCommand;
import application.command.ConfirmOrderCommand;
import application.command.CreateOrderCommand;
import application.port.DomainEventPublisher;
import domain.entity.Order;
import domain.entity.OrderItem;
import domain.exception.OrderNotFoundException;
import domain.repository.OrderRepository;
import domain.valueobject.Money;
import domain.valueobject.OrderId;
import domain.valueobject.Quantity;

import java.util.Objects;

/**
 * Implementation of OrderService.
 * Orchestrates domain operations and publishes events.
 */
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            DomainEventPublisher eventPublisher
    ) {
        this.orderRepository = Objects.requireNonNull(
            orderRepository, "OrderRepository cannot be null"
        );
        this.eventPublisher = Objects.requireNonNull(
            eventPublisher, "DomainEventPublisher cannot be null"
        );
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        Order order = Order.create(command.getCustomerId());
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishAll(savedOrder.pullDomainEvents());
        return savedOrder;
    }

    @Override
    public Order addItem(AddItemCommand command) {
        Order order = findOrderOrThrow(command.getOrderId());

        OrderItem item = new OrderItem(
            command.getProductId(),
            command.getProductName(),
            Quantity.of(command.getQuantity()),
            Money.of(command.getUnitPrice())
        );

        order.addItem(item);
        return orderRepository.save(order);
    }

    @Override
    public Order confirmOrder(ConfirmOrderCommand command) {
        Order order = findOrderOrThrow(command.getOrderId());
        order.confirm();
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishAll(savedOrder.pullDomainEvents());
        return savedOrder;
    }

    @Override
    public Order getOrder(OrderId orderId) {
        return findOrderOrThrow(orderId);
    }

    private Order findOrderOrThrow(OrderId orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
