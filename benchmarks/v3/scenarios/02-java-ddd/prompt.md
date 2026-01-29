# Scenario 02: Java DDD Aggregate

## Prompt

Create an Order aggregate for an e-commerce system:
- Order contains OrderItems
- Order has states: DRAFT, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
- Enforce invariants: cannot add items to confirmed order, minimum order value $10
- Use Value Objects for OrderId, Money, Quantity
- Implement domain events: OrderCreated, OrderConfirmed
- Write tests for all business rules
