# Scenario 02: Java Spring Kafka Consumer

## Task Description

Create a Kafka consumer that processes order events for an order processing system.

### Functional Requirements
- Consume `OrderCreatedEvent` from topic `orders.created`
- Event contains: orderId, customerId, items[], totalAmount, timestamp
- For each event:
  - Validate the order data
  - Update inventory (call InventoryService)
  - Send notification (call NotificationService)
  - Save order status to database
- Handle failures gracefully with retry logic

### Technical Requirements
- Spring Kafka
- Proper error handling and dead letter queue
- Idempotency (handle duplicate messages)
- Include unit tests

## Prompt to Use

```
Create a Kafka consumer in Java Spring Boot that processes order events.

Requirements:
- Consume OrderCreatedEvent from topic "orders.created"
- Event: orderId, customerId, items[], totalAmount, timestamp
- Processing: validate order, update inventory, send notification, save status
- Handle failures with retry and dead letter queue
- Ensure idempotency for duplicate messages
- Include unit tests

Generate the complete implementation.
```

## Expected Output Files

```
results/[with-mcp|without-mcp]/02-java-spring-kafka/
├── OrderCreatedEvent.java
├── OrderEventConsumer.java
├── OrderProcessingService.java
├── config/
│   └── KafkaConfig.java
├── exceptions/
│   └── OrderProcessingException.java
└── tests/
    └── OrderEventConsumerTest.java
```

## Evaluation Criteria

| Criteria | Weight | Description |
|----------|--------|-------------|
| Architecture | 20% | Clean separation, ports/adapters |
| Error Handling | 25% | Retry logic, DLQ, graceful failures |
| Idempotency | 15% | Duplicate handling mechanism |
| Tests | 20% | Consumer tests, mocking Kafka |
| Code Quality | 10% | Clean code, logging |
| Configuration | 10% | Proper Kafka config |
