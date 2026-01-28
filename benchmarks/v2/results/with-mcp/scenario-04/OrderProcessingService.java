package com.example.order.application;

import com.example.order.domain.event.OrderCreatedEvent;
import com.example.order.domain.exception.DuplicateOrderException;
import com.example.order.domain.exception.OrderProcessingException;
import com.example.order.domain.model.ProcessedOrder;
import com.example.order.domain.port.Clock;
import com.example.order.domain.port.IdGenerator;
import com.example.order.domain.port.ProcessedOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderProcessingService {
    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    private final ProcessedOrderRepository repository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public OrderProcessingService(
        ProcessedOrderRepository repository,
        IdGenerator idGenerator,
        Clock clock
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public ProcessedOrder process(OrderCreatedEvent event) {
        log.info("Processing order: {}", event.orderId());

        validateNotDuplicate(event.orderId());
        validateEvent(event);

        ProcessedOrder processedOrder = ProcessedOrder.success(
            idGenerator.generate(),
            event.orderId(),
            event.customerId(),
            event.totalAmount(),
            clock.now()
        );

        repository.save(processedOrder);
        log.info("Order processed successfully: {}", event.orderId());

        return processedOrder;
    }

    public ProcessedOrder processWithFailure(OrderCreatedEvent event, String reason) {
        log.warn("Recording failed order: {} - {}", event.orderId(), reason);

        ProcessedOrder failedOrder = ProcessedOrder.failure(
            idGenerator.generate(),
            event.orderId(),
            event.customerId(),
            event.totalAmount(),
            reason,
            clock.now()
        );

        repository.save(failedOrder);
        return failedOrder;
    }

    private void validateNotDuplicate(String orderId) {
        if (repository.existsByOrderId(orderId)) {
            throw new DuplicateOrderException(orderId);
        }
    }

    private void validateEvent(OrderCreatedEvent event) {
        if (event.orderId() == null || event.orderId().isBlank()) {
            throw new OrderProcessingException(
                event.orderId(),
                "Order ID is required",
                false
            );
        }
        if (event.items() == null || event.items().isEmpty()) {
            throw new OrderProcessingException(
                event.orderId(),
                "Order must have at least one item",
                false
            );
        }
    }
}
