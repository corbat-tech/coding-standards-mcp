# Scenario 04: React Form Component

## Task Description

Create a reusable Contact Form component with validation and submission handling.

### Functional Requirements
- Fields: name, email, phone (optional), subject, message
- Validation:
  - Name: required, 2-50 characters
  - Email: required, valid format
  - Phone: optional, valid format if provided
  - Subject: required, max 100 characters
  - Message: required, 10-1000 characters
- Show validation errors inline
- Submit button disabled until form valid
- Loading state during submission
- Success/error feedback after submit

### Technical Requirements
- React 18+ with hooks
- TypeScript
- Form state management (React Hook Form or custom)
- Include unit tests with React Testing Library

## Prompt to Use

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

## Expected Output Files

```
results/[with-mcp|without-mcp]/04-react-form-component/
├── ContactForm.tsx
├── ContactForm.types.ts
├── useContactForm.ts (or validation logic)
├── ContactForm.module.css (or styled)
└── ContactForm.test.tsx
```

## Evaluation Criteria

| Criteria | Weight | Description |
|----------|--------|-------------|
| Component Design | 20% | Reusability, props API |
| Validation | 20% | Complete validation, good UX |
| Tests | 25% | RTL tests, user interactions |
| TypeScript | 15% | Proper types, interfaces |
| Accessibility | 10% | Labels, ARIA, keyboard |
| Code Quality | 10% | Hooks usage, clean code |
