# Benchmark Test: WITH Corbat MCP

## Instructions

Execute these tests **WITH** Corbat MCP active. This measures the improvement when the LLM has access to coding standards and guardrails.

## Prerequisites

1. **Enable Corbat MCP** - Ensure it's active in your Claude/LLM configuration
2. **Start a fresh conversation** - No context from previous interactions
3. **Use the exact prompts** - The prompts are the same as without-MCP tests
4. **Let the LLM use Corbat** - It should call `get_context` automatically

## Test Execution

For each scenario below, copy the prompt exactly. The LLM should use Corbat MCP to get coding standards before generating code.

---

### Scenario 1: Java Spring CRUD Service

**Prompt:**
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

**Expected MCP Usage:** The LLM should call `get_context({ task: "Create ProductService...", project_dir: "..." })` to get Java Spring guidelines.

**Save to:** `results/with-mcp/01-java-spring-crud/`

---

### Scenario 2: Java Spring Kafka Consumer

**Prompt:**
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

**Save to:** `results/with-mcp/02-java-spring-kafka/`

---

### Scenario 3: Node.js REST API

**Prompt:**
```
Create a User management REST API in Node.js with TypeScript.

Requirements:
- POST /auth/register - register user (email, password, name)
- POST /auth/login - login, returns JWT
- GET /users/me - get profile (authenticated)
- PUT /users/me - update profile (authenticated)
- Password: min 8 chars, 1 uppercase, 1 number
- Email must be unique
- Include input validation and unit tests

Generate the complete implementation.
```

**Save to:** `results/with-mcp/03-nodejs-rest-api/`

---

### Scenario 4: React Form Component

**Prompt:**
```
Create a reusable ContactForm component in React with TypeScript.

Requirements:
- Fields: name, email, phone (optional), subject, message
- Validation: name (2-50 chars), email (valid), phone (optional, valid format), subject (max 100), message (10-1000 chars)
- Show inline validation errors
- Disabled submit until valid
- Loading state during submission
- Success/error feedback
- Include unit tests with React Testing Library

Generate the complete implementation.
```

**Save to:** `results/with-mcp/04-react-form-component/`

---

### Scenario 5: Python FastAPI Endpoint

**Prompt:**
```
Create a Task management API in Python with FastAPI.

Requirements:
- CRUD for Task (id, title, description, status, priority, due_date, created_at)
- Status: pending/in_progress/completed
- Priority: low/medium/high
- Filter by status and priority
- Mark as completed endpoint
- Rules: title required (max 200), due_date in future, no changes to completed tasks
- Use async SQLAlchemy
- Include pytest tests

Generate the complete implementation.
```

**Save to:** `results/with-mcp/05-python-fastapi-endpoint/`

---

### Scenario 6: Go HTTP Handler

**Prompt:**
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

**Save to:** `results/with-mcp/06-go-http-handler/`

---

## After Completion

1. Verify all 6 scenarios have generated code saved
2. Each folder should have the main implementation files
3. Note if the LLM called Corbat MCP tools during generation
4. Proceed to `COMPARE_RESULTS.md` for analysis

## Checklist

- [ ] Scenario 1: Java Spring CRUD saved
- [ ] Scenario 2: Java Spring Kafka saved
- [ ] Scenario 3: Node.js REST API saved
- [ ] Scenario 4: React Form Component saved
- [ ] Scenario 5: Python FastAPI saved
- [ ] Scenario 6: Go HTTP Handler saved

---

## Verification

After completing all scenarios, verify Corbat was used by checking:
- Did the LLM call `get_context` before generating code?
- Did the generated code follow the guidelines from Corbat?
- Are there visible differences in architecture/patterns compared to without-MCP?

**Note:** Do not modify the generated code. Save it exactly as the LLM produces it for fair comparison.
