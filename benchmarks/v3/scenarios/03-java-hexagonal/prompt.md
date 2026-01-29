# Scenario 03: Java Hexagonal Architecture

## Prompt

Implement a Payment processing service with hexagonal architecture:
- Input ports: ProcessPayment, RefundPayment, GetPaymentStatus
- Output ports: PaymentGateway, PaymentRepository, NotificationService
- Domain: Payment entity, PaymentStatus, Money value object
- Adapters: REST controller, JPA repository, Stripe gateway mock
- Write tests at each layer
