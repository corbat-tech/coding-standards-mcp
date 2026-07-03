<div align="center">

# CORBAT MCP
#### Policy and quality engine for AI coding agents

**Shared standards, context, and verification gates for agent-assisted software delivery.**

[![npm version](https://img.shields.io/npm/v/@corbat-tech/coding-standards-mcp.svg)](https://www.npmjs.com/package/@corbat-tech/coding-standards-mcp)
[![CI](https://github.com/corbat-tech/coding-standards-mcp/actions/workflows/ci.yml/badge.svg)](https://github.com/corbat-tech/coding-standards-mcp/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-82%25-brightgreen.svg)](https://github.com/corbat-tech/coding-standards-mcp)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![MCP](https://img.shields.io/badge/MCP-1.0-blue.svg)](https://modelcontextprotocol.io/)

---

[![Cursor](https://img.shields.io/badge/Cursor-✓-black?style=flat-square&logo=cursor)](docs/setup.md#cursor)
[![VS Code](https://img.shields.io/badge/VS_Code-✓-007ACC?style=flat-square&logo=visualstudiocode)](docs/setup.md#vs-code)
[![Windsurf](https://img.shields.io/badge/Windsurf-✓-00C7B7?style=flat-square)](docs/setup.md#windsurf)
[![JetBrains](https://img.shields.io/badge/JetBrains-✓-orange?style=flat-square&logo=jetbrains)](docs/setup.md#jetbrains-ides)
[![Zed](https://img.shields.io/badge/Zed-✓-084CCF?style=flat-square)](docs/setup.md#zed)
[![Claude](https://img.shields.io/badge/Claude-✓-cc785c?style=flat-square)](docs/setup.md#claude-desktop)

**Designed for MCP-compatible coding tools and multi-agent workflows. See [compatibility notes](docs/compatibility.md).**

⚡ **Try it in 30 seconds** — just add the config below and start coding.

</div>

---

## The Problem

AI-generated code often works locally but misses team-specific review expectations:

| Without Corbat | With Corbat |
|----------------|-------------|
| Methods with 50+ lines | Max 20 lines per method |
| No dependency injection | Proper DI with interfaces |
| `throw new Error('failed')` | Custom exceptions with context |
| Missing or minimal tests | Tests included, TDD approach |
| God classes, mixed concerns | SOLID principles, clean layers |
| Works locally | Reviewable implementation with explicit checks |

Corbat helps by giving agents your team's standards before implementation and by providing validation tools before review or handoff.

---

## Quick Start

**1. Add to your MCP config:**

```json
{
  "mcpServers": {
    "corbat": {
      "command": "npx",
      "args": ["-y", "@corbat-tech/coding-standards-mcp"]
    }
  }
}
```

**2. Config file location:**

| Tool | Location |
|------|----------|
| Cursor | `.cursor/mcp.json` |
| VS Code | `.vscode/mcp.json` |
| Windsurf | `~/.codeium/windsurf/mcp_config.json` |
| JetBrains | Settings → AI Assistant → MCP |
| Claude Desktop | `~/.config/Claude/claude_desktop_config.json` |
| Claude Code | `claude mcp add corbat -- npx -y @corbat-tech/coding-standards-mcp` |

> [Complete setup guide](docs/setup.md) for common MCP-compatible tools

**3. Done.** Corbat can auto-detect your stack and return matching standards.

> **Zero overhead.** Corbat runs locally and adds ~50ms to detect your stack. After that, it's just context for the AI.

> **Fully customizable.** Don't like a rule? Override it in `.corbat.json`. [Jump to customization →](#customize)

---

## See It In Action

```
You: "Create a user registration service"
```

**Without Corbat**, you get:

```typescript
class UserService {
  private users: Map<string, User> = new Map();

  register(name: string, email: string, password: string) {
    if (!name || !email || !password) throw new Error('Invalid input');
    const user = { id: crypto.randomUUID(), name, email, password };
    this.users.set(user.id, user);
    return user;
  }
}
// ✗ Hardcoded storage  ✗ Password in plain text  ✗ Generic error  ✗ No tests
```

**With Corbat**, you get:

```typescript
// Domain
interface UserRepository {
  save(user: User): Promise<void>;
  findByEmail(email: string): Promise<User | null>;
}

class EmailAlreadyExistsError extends Error {
  constructor(email: string) {
    super(`Email already registered: ${email}`);
  }
}

// Application
class RegisterUserUseCase {
  constructor(
    private readonly repository: UserRepository,
    private readonly hasher: PasswordHasher
  ) {}

  async execute(input: RegisterUserInput): Promise<User> {
    const existing = await this.repository.findByEmail(input.email);
    if (existing) throw new EmailAlreadyExistsError(input.email);

    const user = User.create({
      ...input,
      password: await this.hasher.hash(input.password)
    });
    await this.repository.save(user);
    return user;
  }
}
// ✓ Repository interface  ✓ Password hashing  ✓ Custom error  ✓ Testable
```

```typescript
// Test included
describe('RegisterUserUseCase', () => {
  const repository = { save: vi.fn(), findByEmail: vi.fn() };
  const hasher = { hash: vi.fn() };
  const useCase = new RegisterUserUseCase(repository, hasher);

  beforeEach(() => vi.clearAllMocks());

  it('should hash password before saving', async () => {
    repository.findByEmail.mockResolvedValue(null);
    hasher.hash.mockResolvedValue('hashed_password');

    await useCase.execute({ name: 'John', email: 'john@test.com', password: 'secret' });

    expect(hasher.hash).toHaveBeenCalledWith('secret');
    expect(repository.save).toHaveBeenCalledWith(
      expect.objectContaining({ password: 'hashed_password' })
    );
  });

  it('should reject duplicate emails', async () => {
    repository.findByEmail.mockResolvedValue({ id: '1', email: 'john@test.com' });

    await expect(
      useCase.execute({ name: 'John', email: 'john@test.com', password: 'secret' })
    ).rejects.toThrow(EmailAlreadyExistsError);
  });
});
```

This is the kind of structure Corbat asks agents to consider before handoff.

---

## What Corbat Provides

Corbat provides these guardrails before generation and validation checks during iteration:

### Code Quality
| Rule | Why It Matters |
|------|----------------|
| **Max 20 lines per method** | Readable, testable, single-purpose functions |
| **Max 200 lines per class** | Single Responsibility Principle |
| **Meaningful names** | No `data`, `info`, `temp`, `x` |
| **No magic numbers** | Constants with descriptive names |

### Architecture
| Rule | Why It Matters |
|------|----------------|
| **Interfaces for dependencies** | Testable code, easy mocking |
| **Layer separation** | Domain logic isolated from infrastructure |
| **Hexagonal/Clean patterns** | Framework-agnostic business rules |

### Error Handling
| Rule | Why It Matters |
|------|----------------|
| **Custom exceptions** | `UserNotFoundError` vs `Error('not found')` |
| **Error context** | Include IDs, values, state in errors |
| **No empty catches** | Every error handled or propagated |

### Security-Oriented Checks
| Rule | Why It Matters |
|------|----------------|
| **Input validation** | Reject bad data at boundaries |
| **No hardcoded secrets** | Environment variables only |
| **Parameterized queries** | Prevent SQL injection |
| **Output encoding** | Prevent XSS |

---

## Benchmark Results v3.0

We evaluated Corbat across **15 scenarios** in 6 languages. The primary benchmark report is mixed and should be read honestly: Corbat won **1/15** scenarios by the original aggregate score, while an alternative value analysis found stronger results for code compactness and maintainability.

### What The Data Supports

The strongest observed signal is that Corbat-guided outputs are often smaller and more focused:

| Scenario | With Corbat | Without Corbat | What This Means |
|----------|:-----------:|:--------------:|-----------------|
| Kotlin Coroutines | 236 lines | 1,923 lines | Same functionality, 8x less to maintain |
| Java Hexagonal | 623 lines | 2,740 lines | Clean architecture without the bloat |
| Go Clean Arch | 459 lines | 2,012 lines | Idiomatic Go, not Java-in-Go |
| TypeScript NestJS | 395 lines | 1,554 lines | Right patterns, right size |

This is not enough to claim universal quality improvement. It is evidence that standards context can reduce over-generation in some workflows.

### Value Metrics

The value report reweights the same dataset toward efficiency and maintainability:

| Metric | Result | What It Means |
|--------|:------:|---------------|
| **Code Reduction** | 67% | Less to maintain, review, and debug |
| **Security checks** | 100% | No issues detected by benchmark pattern checks |
| **Maintainability** | 93% win | Easier to understand and modify |
| **Architecture Efficiency** | 87% win | Better patterns per line of code |
| **Cognitive Load** | -59% | Faster onboarding for new developers |

📊 [Detailed value analysis](benchmarks/v3/CORBAT_VALUE_REPORT.md)

### Security: No Benchmark Pattern Findings

Every scenario was analyzed using pattern checks inspired by OWASP Top 10 categories. This is not a replacement for SAST, DAST, dependency scanning, manual review, or threat modeling.

- No SQL/NoSQL injection patterns detected
- No XSS patterns detected
- No hardcoded credentials detected
- Input validation patterns present at boundaries
- Error messages did not expose stack traces in the benchmark samples

### Languages & Patterns Tested

| Language | Scenarios | Patterns |
|:--------:|:---------:|:---------|
| ☕ Java | 5 | Spring Boot, DDD Aggregates, Hexagonal, Kafka Events, Saga |
| 📘 TypeScript | 4 | Express REST, NestJS Clean, React Components, Next.js Full-Stack |
| 🐍 Python | 2 | FastAPI CRUD, Repository Pattern |
| 🐹 Go | 2 | HTTP Handlers, Clean Architecture |
| 🦀 Rust | 1 | Axum with Repository Trait |
| 🟣 Kotlin | 1 | Coroutines + Strategy Pattern |

📖 [Full benchmark methodology](benchmarks/v3/BENCHMARK_REPORT_V3.md) · [Value analysis](benchmarks/v3/CORBAT_VALUE_REPORT.md)

---

## Built-in Profiles

Corbat auto-detects your stack and applies the right standards:

| Profile | Stack | What You Get |
|---------|-------|--------------|
| `java-spring-backend` | Java 21 + Spring Boot 3 | Hexagonal + DDD, TDD with 80%+ coverage |
| `kotlin-spring` | Kotlin + Spring Boot 3 | Coroutines, Kotest + MockK |
| `nodejs` | Node.js + TypeScript | Clean Architecture, Vitest |
| `nextjs` | Next.js 14+ | App Router patterns, Server Components |
| `react` | React 18+ | Hooks, Testing Library, accessible components |
| `vue` | Vue 3.5+ | Composition API, Vitest |
| `angular` | Angular 19+ | Standalone components, Jest |
| `python` | Python + FastAPI | Async patterns, pytest |
| `go` | Go 1.22+ | Idiomatic Go, table-driven tests |
| `rust` | Rust + Axum | Ownership patterns, proptest |
| `csharp-dotnet` | C# 12 + ASP.NET Core 8 | Clean + CQRS, xUnit |
| `flutter` | Dart 3 + Flutter | BLoC/Riverpod, widget tests |

**Auto-detection:** Corbat reads `pom.xml`, `package.json`, `go.mod`, `Cargo.toml`, etc.

---

## When to Use Corbat

| Use Case | Why Corbat Helps |
|----------|------------------|
| **Starting a new project** | Correct architecture from day one |
| **Multi-agent delivery** | Planner, implementation, review, and security agents share the same policy context |
| **Teams with mixed experience** | Standards become explicit and repeatable |
| **Strict code review standards** | Agents can validate against the review bar before handoff |
| **Regulated industries** | Consistent security and documentation |
| **Legacy modernization** | New code follows modern patterns |

### When Corbat Might Not Be Needed

- Quick prototypes where quality doesn't matter
- One-off scripts you'll throw away
- Learning projects where you want to make mistakes

---

## Customize

### Option 1: Interactive Setup

```bash
npx corbat-init
```

Detects your stack and generates a `.corbat.json` with sensible defaults.

### Option 2: Manual Configuration

Create `.corbat.json` in your project root:

```json
{
  "profile": "java-spring-backend",
  "architecture": {
    "pattern": "hexagonal",
    "layers": ["domain", "application", "infrastructure", "api"]
  },
  "quality": {
    "maxMethodLines": 20,
    "maxClassLines": 200,
    "minCoverage": 80
  },
  "rules": {
    "always": [
      "Use records for DTOs",
      "Prefer Optional over null"
    ],
    "never": [
      "Use field injection",
      "Catch generic Exception"
    ]
  }
}
```

### Option 3: Use a Template

**[Browse 14 ready-to-use templates](docs/templates.md)** for Java, Python, Node.js, React, Go, Rust, and more.

---

## How It Works

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Your Prompt │────▶│ Corbat MCP  │────▶│ AI + Rules  │
└─────────────┘     └──────┬──────┘     └─────────────┘
                          │
           ┌──────────────┼──────────────┐
           ▼              ▼              ▼
    ┌────────────┐ ┌────────────┐ ┌────────────┐
    │ 1. Detect  │ │ 2. Load    │ │ 3. Inject  │
    │   Stack    │ │  Profile   │ │ Guardrails │
    └────────────┘ └────────────┘ └────────────┘
     pom.xml        hexagonal      max 20 lines
     package.json   + DDD          + interfaces
     go.mod         + SOLID        + custom errors
```

Corbat does not modify AI output. It supplies context, standards, profiles, and validation feedback so agents can align with your review expectations.

**Important:** Actual code quality depends on the model, host tool, prompt, repository context, tests, and human review. Treat Corbat as a policy and verification layer, not as a guarantee of production readiness.

---

## Documentation

| Resource | Description |
|----------|-------------|
| [Setup Guide](docs/setup.md) | Installation notes for common MCP-compatible tools |
| [Templates](docs/templates.md) | Ready-to-use `.corbat.json` configurations |
| [Compatibility](docs/compatibility.md) | Full list of supported AI tools |
| [Benchmark Analysis](benchmarks/v3/BENCHMARK_REPORT_V3.md) | Detailed results from 15 scenarios |
| [Multi-Agent Architecture](docs/multi-agent-architecture.md) | How Corbat fits planner/reviewer/security/release agent workflows |
| [Security Model](docs/security.md) | Local execution model, threat model, and reporting |
| [API Reference](docs/full-documentation.md) | Tools, prompts, and configuration options |

---

<div align="center">

### Make agent output easier to review.

Add to your MCP config and you're done:

```json
{ "mcpServers": { "corbat": { "command": "npx", "args": ["-y", "@corbat-tech/coding-standards-mcp"] }}}
```

Use Corbat as shared standards context plus a local quality gate.

---

*Developed by [corbat-tech](https://corbat.tech)*

</div>
