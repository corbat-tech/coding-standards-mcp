# Contributing to Corbat MCP

Thank you for your interest in contributing to Corbat MCP! This document provides guidelines and instructions for contributing.

## Development Setup

### Prerequisites

- Node.js 18+
- npm or pnpm

### Getting Started

```bash
# Clone the repository
git clone https://github.com/corbat-tech/coding-standards-mcp.git
cd corbat-mcp

# Install dependencies
npm install

# Run tests
npm test

# Build
npm run build

# Run in development mode
npm run dev
```

## Project Structure

```
corbat-mcp/
├── src/
│   ├── index.ts          # MCP server entry point
│   ├── config.ts         # Configuration management
│   ├── agent.ts          # Stack detection, task classification
│   ├── profiles.ts       # Profile loading and caching
│   ├── guardrails.ts     # Guardrails loading
│   ├── prompts.ts        # MCP prompts
│   ├── resources.ts      # MCP resources
│   ├── types.ts          # Zod schemas and TypeScript types
│   ├── logger.ts         # Structured logging
│   ├── errors.ts         # Custom error classes
│   ├── metrics.ts        # Usage metrics
│   └── tools/
│       ├── definitions.ts    # Tool definitions
│       ├── schemas.ts        # Input validation schemas
│       ├── index.ts          # Tool dispatcher
│       └── handlers/         # Individual tool handlers
├── profiles/
│   ├── templates/        # Built-in profiles
│   ├── examples/         # Example custom profiles
│   └── custom/           # User custom profiles (gitignored)
├── guardrails/           # Task-type guardrails
├── standards/            # Documentation standards
├── tests/                # Test suites
└── docs/                 # Documentation
```

## Adding a New Profile

1. Create a YAML file in `profiles/templates/`:

```yaml
# profiles/templates/my-stack.yaml
name: "My Stack Profile"
description: "Standards for My Stack"

# Optionally extend an existing profile
extends: "nodejs"

architecture:
  type: "clean"
  enforceLayerDependencies: true

codeQuality:
  maxMethodLines: 20
  maxClassLines: 200
  minimumTestCoverage: 80

# Add other sections as needed...
```

2. The profile schema is defined in `src/types.ts` - refer to existing profiles for examples.

3. Add tests in `tests/profiles.test.ts` if adding new behavior.

## Adding a New Tool

1. **Add the handler** in `src/tools/handlers/`:

```typescript
// src/tools/handlers/my-tool.ts
import { MyToolSchema } from '../schemas.js';

export async function handleMyTool(args: Record<string, unknown>) {
  const { param1 } = MyToolSchema.parse(args);

  // Tool logic here

  return {
    content: [{ type: 'text', text: 'Result' }]
  };
}
```

2. **Add the schema** in `src/tools/schemas.ts`:

```typescript
export const MyToolSchema = z.object({
  param1: z.string(),
});
```

3. **Add the definition** in `src/tools/definitions.ts`:

```typescript
{
  name: 'my_tool',
  description: `Description for LLMs...`,
  inputSchema: {
    type: 'object',
    properties: {
      param1: { type: 'string', description: '...' }
    },
    required: ['param1']
  }
}
```

4. **Export and register** in `src/tools/handlers/index.ts` and `src/tools/index.ts`.

5. **Add tests** in `tests/handlers.test.ts`.

## Code Style

We use Biome for formatting and linting:

```bash
# Format code
npm run format

# Lint code
npm run lint

# Check both
npm run check
```

### Guidelines

- Use TypeScript strict mode
- Use Zod for runtime validation
- Keep functions small and focused
- Add JSDoc comments for public APIs
- Follow existing patterns in the codebase

## Testing

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Run with coverage
npm run test:coverage
```

### Test Structure

- `tests/unit/` - Unit tests for isolated functions
- `tests/` - Integration tests for handlers and workflows
- Use Vitest for all tests

### Writing Tests

```typescript
import { describe, expect, it } from 'vitest';

describe('MyFeature', () => {
  it('should do something', async () => {
    const result = await myFunction();
    expect(result).toBe('expected');
  });
});
```

## Pull Request Process

1. **Create a feature branch** from `main`
2. **Make your changes** following the guidelines above
3. **Ensure tests pass**: `npm test`
4. **Ensure linting passes**: `npm run check`
5. **Update documentation** if needed
6. **Submit a PR** with a clear description

### PR Title Format

- `feat: Add new feature`
- `fix: Fix bug description`
- `docs: Update documentation`
- `refactor: Refactor component`
- `test: Add tests for feature`

## Releasing

Releases are managed by maintainers. Version bumps follow semver:

- **Patch**: Bug fixes, documentation
- **Minor**: New features, non-breaking changes
- **Major**: Breaking changes

## Questions?

- Open an issue for bugs or feature requests
- Start a discussion for questions
- Check existing issues before creating new ones

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
