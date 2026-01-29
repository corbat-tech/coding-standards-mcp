# Scenario 04: Java Kafka Event-Driven

## Prompt

Create an event-driven order processing system with Kafka:
- Producer: OrderService publishes OrderCreated events
- Consumer: InventoryService consumes and updates stock
- Use Spring Kafka
- Implement idempotency for consumers
- Dead letter queue for failed messages
- Write integration tests with embedded Kafka
