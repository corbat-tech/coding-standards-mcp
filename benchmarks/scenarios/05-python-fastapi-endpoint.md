# Scenario 05: Python FastAPI Endpoint

## Task Description

Create a Task management API endpoint with async database operations.

### Functional Requirements
- CRUD for Task entity
- Task has: id, title, description, status (pending/in_progress/completed), priority (low/medium/high), due_date, created_at
- Filter tasks by status and priority
- Mark task as completed
- Business rules:
  - Title required, max 200 chars
  - Due date must be in the future when creating
  - Cannot change status of completed tasks

### Technical Requirements
- FastAPI
- Async/await with SQLAlchemy async
- Pydantic models for validation
- Include unit tests with pytest

## Prompt to Use

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

## Expected Output Files

```
results/[with-mcp|without-mcp]/05-python-fastapi-endpoint/
├── models/
│   └── task.py
├── schemas/
│   └── task.py
├── routers/
│   └── tasks.py
├── services/
│   └── task_service.py
├── database.py
└── tests/
    └── test_tasks.py
```

## Evaluation Criteria

| Criteria | Weight | Description |
|----------|--------|-------------|
| Architecture | 20% | Clean structure, separation |
| Async | 20% | Proper async/await usage |
| Validation | 20% | Pydantic models, business rules |
| Tests | 20% | pytest, async tests |
| Type Hints | 10% | Complete type annotations |
| Code Quality | 10% | PEP8, clean code |
