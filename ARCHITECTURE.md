# Corbat MCP Architecture

## Overview

Corbat MCP is a Model Context Protocol (MCP) server that provides AI coding assistants with coding standards, guardrails, and best practices. It follows a modular architecture designed for extensibility, testability, and performance.

```
┌─────────────────────────────────────────────────────────────────┐
│                         MCP Client                               │
│                    (Claude, VS Code, etc.)                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ MCP Protocol (JSON-RPC over stdio)
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Corbat MCP Server                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  Tools   │  │ Resources│  │ Prompts  │  │  Agent Module    │ │
│  │ Handler  │  │ Handler  │  │ Handler  │  │ (Task Detection) │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────┬─────────┘ │
│       │             │             │                  │           │
│       └─────────────┴─────────────┴──────────────────┘           │
│                             │                                     │
│                             ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │                    Core Services                              ││
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐             ││
│  │  │  Profiles  │  │ Guardrails │  │  Standards │             ││
│  │  │   Loader   │  │   Loader   │  │   Loader   │             ││
│  │  └────────────┘  └────────────┘  └────────────┘             ││
│  └──────────────────────────────────────────────────────────────┘│
│                             │                                     │
│                             ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │                    YAML Configuration                         ││
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐             ││
│  │  │  profiles/ │  │ guardrails/│  │ standards/ │             ││
│  │  │ templates/ │  │            │  │            │             ││
│  │  └────────────┘  └────────────┘  └────────────┘             ││
│  └──────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

## Directory Structure

```
corbat-mcp/
├── src/                      # Source code
│   ├── index.ts              # Entry point, MCP server setup
│   ├── config.ts             # Configuration management
│   ├── types.ts              # TypeScript types and Zod schemas
│   ├── tools.ts              # MCP Tools definitions and handlers
│   ├── prompts.ts            # MCP Prompts definitions and handlers
│   ├── resources.ts          # MCP Resources definitions and handlers
│   ├── profiles.ts           # Profile loading and formatting
│   ├── guardrails.ts         # Guardrails loading from YAML
│   └── agent.ts              # Task detection, stack detection, guardrails
│
├── profiles/                 # Profile definitions
│   ├── templates/            # Built-in profiles (YAML)
│   │   ├── java-spring-backend.yaml
│   │   ├── react.yaml
│   │   ├── nodejs.yaml
│   │   ├── python.yaml
│   │   └── ...
│   └── custom/               # User-defined profiles
│
├── guardrails/               # Task-type guardrails (YAML)
│   ├── feature.yaml
│   ├── bugfix.yaml
│   ├── refactor.yaml
│   ├── test.yaml
│   └── ...
│
├── standards/                # Documentation standards (Markdown)
│   ├── testing/
│   ├── architecture/
│   └── ...
│
└── tests/                    # Test suites
    ├── handlers.test.ts      # Tool handler tests
    ├── profiles.test.ts      # Profile loading tests
    ├── mcp-protocol.test.ts  # E2E MCP protocol tests
    └── ...
```

## Core Modules

### 1. Entry Point (`index.ts`)

Initializes the MCP server with three capabilities:
- **Tools**: Callable functions (get_context, validate, search, profiles, health)
- **Resources**: Readable content (profiles, standards)
- **Prompts**: Pre-built prompts (implement, review)

### 2. Tools Module (`tools.ts`)

Defines 5 simplified tools following MCP specification:

| Tool | Purpose |
|------|---------|
| `get_context` | **Primary tool** - Returns complete context for a task |
| `validate` | Validates code against standards |
| `search` | Searches standards documentation |
| `profiles` | Lists available profiles |
| `health` | Server health check |

### 3. Agent Module (`agent.ts`)

Handles intelligent task processing:

```typescript
// Task type classification
classifyTaskType(task: string) → 'feature' | 'bugfix' | 'refactor' | 'test' | ...

// Project stack auto-detection
detectProjectStack(projectDir: string) → { language, framework, suggestedProfile }

// Guardrails retrieval
getGuardrails(taskType, projectConfig) → { mandatory, avoid, workflow }
```

### 4. Profiles Module (`profiles.ts`)

Manages profile loading with:
- **Parallel loading**: All YAML files loaded concurrently
- **Caching**: 60-second TTL cache
- **Priority override**: custom/ > root/ > templates/
- **Validation**: Zod schema validation

### 5. Guardrails Module (`guardrails.ts`)

Loads task-specific guardrails from YAML:
- Mandatory rules (MUST do)
- Avoid rules (MUST NOT do)
- Workflow steps (Chain-of-thought guidance)
- Patterns and anti-patterns

## Data Flow

### Tool Call Flow

```
1. Client calls tool (e.g., get_context)
       │
       ▼
2. handleToolCall() validates input (Zod schema)
       │
       ▼
3. classifyTaskType() determines task type
       │
       ▼
4. detectProjectStack() auto-detects tech stack (if project_dir provided)
       │
       ▼
5. getProfile() loads matching profile from cache/YAML
       │
       ▼
6. getGuardrails() loads task-specific guardrails
       │
       ▼
7. Format and return markdown response
```

### Profile Loading Flow

```
1. loadProfiles() checks cache
       │
       ├── Cache valid? → Return cached profiles
       │
       └── Cache expired?
               │
               ▼
       2. Load directories in parallel:
          - profiles/templates/
          - profiles/custom/
          - profiles/ (root)
               │
               ▼
       3. For each directory, load all YAML files in parallel
               │
               ▼
       4. Parse YAML → Validate with Zod → Create Profile objects
               │
               ▼
       5. Merge with priority (custom > root > templates)
               │
               ▼
       6. Update cache, return profiles
```

## Type System

### Profile Schema (simplified)

```typescript
interface Profile {
  name: string;
  description?: string;
  architecture?: {
    type: 'hexagonal' | 'clean' | 'layered' | 'feature-based';
    layers?: Layer[];
    enforceLayerDependencies?: boolean;
  };
  codeQuality?: {
    maxMethodLines: number;
    maxClassLines: number;
    minimumTestCoverage: number;
  };
  naming?: {
    general: Record<string, string>;
    suffixes: Record<string, string>;
  };
  testing?: TestingConfig;
  ddd?: DDDConfig;
  cqrs?: CQRSConfig;
  // ... more sections
  codeExamples?: Record<string, CodeExample>;
  antiPatterns?: Record<string, AntiPattern>;
}
```

### Guardrails Schema

```typescript
interface Guardrails {
  taskType: string;
  mandatory: string[];
  recommended?: string[];
  avoid: string[];
  workflow?: {
    steps: Array<{
      name: string;
      description: string;
      actions: string[];
    }>;
  };
  patterns?: Record<string, unknown>;
  antiPatterns?: Record<string, unknown>;
}
```

## Performance Optimizations

1. **Parallel Loading**: All I/O operations use `Promise.all()`
2. **Caching**: Profiles and standards cached with 60s TTL
3. **Lazy Loading**: Resources loaded only when requested
4. **Schema Validation**: Zod schemas validate at parse time

## Testing Strategy

| Test Type | Location | Purpose |
|-----------|----------|---------|
| Unit | `tests/*.test.ts` | Individual function testing |
| Integration | `tests/handlers.test.ts` | Tool handler integration |
| E2E | `tests/mcp-protocol.test.ts` | Full MCP protocol compliance |
| Schema | `tests/profiles.test.ts` | YAML profile validation |

### Running Tests

```bash
npm test           # Run all tests
npm run test:watch # Watch mode
npm run coverage   # Coverage report
```

## Extensibility

### Adding a New Profile

1. Create `profiles/templates/my-stack.yaml`
2. Follow the schema in `src/types.ts`
3. Include `codeExamples` and `antiPatterns` sections

### Adding a New Guardrail

1. Create `guardrails/my-task-type.yaml`
2. Include mandatory, avoid, and workflow sections
3. Update `TaskType` in `src/types.ts`

### Adding a New Tool

1. Add tool definition in `tools` array (`src/tools.ts`)
2. Add handler case in `handleToolCall()`
3. Add Zod schema for input validation
4. Add tests in `tests/handlers.test.ts`

## Security Considerations

- No execution of arbitrary code
- All configuration is declarative YAML
- Input validation via Zod schemas
- No network requests (pure local operation)
- Profiles loaded from trusted directories only

## Version Compatibility

- Node.js: 20+
- TypeScript: 5.x
- MCP SDK: 1.x

## Related Documentation

- [README.md](./README.md) - Getting started guide
- [Model Context Protocol](https://modelcontextprotocol.io/) - MCP specification
- [Zod](https://zod.dev/) - Schema validation library
