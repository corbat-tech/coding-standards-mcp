# Scenario 05: Java Saga Pattern

## Prompt

Implement a Saga for order fulfillment:
- Steps: CreateOrder -> ReserveInventory -> ProcessPayment -> ShipOrder
- Compensating actions for rollback
- Orchestrator pattern
- Handle partial failures
- Write tests for happy path and all failure scenarios
