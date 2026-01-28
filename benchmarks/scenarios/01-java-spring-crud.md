# Scenario 01: Java Spring CRUD Service

## Task Description

Create a `ProductService` for an e-commerce application that manages products with the following requirements:

### Functional Requirements
- CRUD operations for Product entity
- Product has: id, name, description, price, stock, category, createdAt, updatedAt
- Business rules:
  - Price must be positive
  - Stock cannot be negative
  - Name is required and max 100 characters
- Search products by category
- Update stock (increment/decrement)

### Technical Requirements
- Spring Boot 3.x
- Use appropriate architecture patterns
- Include proper error handling
- Include unit tests

## Prompt to Use

```
Create a ProductService for an e-commerce application in Java Spring Boot.

Requirements:
- CRUD operations for Product (id, name, description, price, stock, category, createdAt, updatedAt)
- Business rules: price > 0, stock >= 0, name required (max 100 chars)
- Search by category
- Update stock method (increment/decrement)
- Include unit tests

Generate the complete implementation with all necessary classes.
```

## Expected Output Files

Save the generated code in the results folder with this structure:
```
results/[with-mcp|without-mcp]/01-java-spring-crud/
├── Product.java
├── ProductRepository.java
├── ProductService.java (or interface + impl)
├── ProductController.java (if generated)
├── exceptions/ (if any)
└── tests/
    └── ProductServiceTest.java
```

## Evaluation Criteria

| Criteria | Weight | Description |
|----------|--------|-------------|
| Architecture | 25% | Hexagonal/Clean architecture, layer separation |
| Tests | 25% | Unit tests present, AAA pattern, coverage |
| Error Handling | 15% | Custom exceptions, proper validation |
| Naming | 10% | Follows Java conventions |
| Code Quality | 15% | Method size, complexity, SOLID |
| Documentation | 10% | Javadoc, clear code |
