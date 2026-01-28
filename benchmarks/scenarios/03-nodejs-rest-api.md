# Scenario 03: Node.js REST API

## Task Description

Create a User management REST API with authentication endpoints.

### Functional Requirements
- User registration (email, password, name)
- User login (returns JWT token)
- Get user profile (authenticated)
- Update user profile (authenticated)
- Password validation: min 8 chars, 1 uppercase, 1 number
- Email must be unique

### Technical Requirements
- Express.js or Fastify
- TypeScript
- Proper input validation
- JWT authentication
- Include unit tests

## Prompt to Use

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

## Expected Output Files

```
results/[with-mcp|without-mcp]/03-nodejs-rest-api/
├── src/
│   ├── controllers/
│   │   └── userController.ts
│   ├── services/
│   │   └── userService.ts
│   ├── middleware/
│   │   └── authMiddleware.ts
│   ├── validators/
│   │   └── userValidator.ts
│   ├── models/
│   │   └── user.ts
│   └── routes/
│       └── userRoutes.ts
└── tests/
    └── userService.test.ts
```

## Evaluation Criteria

| Criteria | Weight | Description |
|----------|--------|-------------|
| Architecture | 25% | Clean architecture, layer separation |
| Security | 20% | Password hashing, JWT handling, validation |
| Tests | 20% | Unit tests, mocking |
| TypeScript | 15% | Proper types, no any |
| Error Handling | 10% | Proper HTTP codes, error messages |
| Code Quality | 10% | Clean code, async/await |
