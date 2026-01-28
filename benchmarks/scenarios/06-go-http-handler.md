# Scenario 06: Go HTTP Handler

## Task Description

Create an HTTP handler for a Book inventory system with proper Go idioms.

### Functional Requirements
- CRUD for Book entity
- Book has: ID, Title, Author, ISBN, PublishedYear, Genre, Available (bool)
- Borrow book (set Available=false)
- Return book (set Available=true)
- Search by author or genre
- Business rules:
  - ISBN must be valid format (10 or 13 digits)
  - Cannot borrow unavailable book
  - PublishedYear must be reasonable (1450-current year)

### Technical Requirements
- Standard library (net/http) or Chi router
- Proper error handling with custom errors
- JSON request/response
- Include unit tests with table-driven tests

## Prompt to Use

```
Create an HTTP handler for a Book inventory system in Go.

Requirements:
- CRUD for Book (ID, Title, Author, ISBN, PublishedYear, Genre, Available)
- POST /books/{id}/borrow - borrow book
- POST /books/{id}/return - return book
- GET /books?author=X or GET /books?genre=X - search
- Rules: valid ISBN (10 or 13 digits), can't borrow unavailable, valid year (1450-now)
- Use standard library or Chi
- Include table-driven unit tests

Generate the complete implementation.
```

## Expected Output Files

```
results/[with-mcp|without-mcp]/06-go-http-handler/
├── main.go
├── handlers/
│   └── book_handler.go
├── models/
│   └── book.go
├── services/
│   └── book_service.go
├── errors/
│   └── errors.go
└── handlers/
    └── book_handler_test.go
```

## Evaluation Criteria

| Criteria | Weight | Description |
|----------|--------|-------------|
| Go Idioms | 25% | Error handling, interfaces, naming |
| Architecture | 20% | Clean structure, dependency injection |
| Tests | 25% | Table-driven tests, coverage |
| Error Handling | 15% | Custom errors, proper HTTP codes |
| Validation | 10% | Input validation, business rules |
| Code Quality | 5% | gofmt, clean code |
