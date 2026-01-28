<div align="center">

# CORBAT MCP
#### AI Coding Standards Server

**AI-generated code that passes code review on the first try.**

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

**Works with GitHub Copilot, Continue, Cline, Tabnine, Amazon Q, and [25+ more tools](docs/compatibility.md)**

</div>

---

## The Problem

AI-generated code works, but rarely passes code review:

| Without Corbat | With Corbat |
|----------------|-------------|
| No dependency injection | Proper DI with interfaces |
| Missing error handling | Custom error types with context |
| Basic tests (if any) | 80%+ coverage with TDD |
| God classes, long methods | SOLID, max 20 lines/method |
| Fails SonarQube | Passes quality gates |

**Result:** Production-ready code that passes code review.

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

> [Complete setup guide](docs/setup.md) for all 25+ tools

**3. Done!** Corbat auto-detects your stack.

```
You: "Create a payment service"

Corbat: ✓ Detected: Java 21, Spring Boot 3, Maven
        ✓ Profile: java-spring-backend
        ✓ Architecture: Hexagonal + DDD
        ✓ Testing: TDD, 80%+ coverage
```

---

## Benchmark Results v2.0

### Overall Impact

<div align="center">

| Metric | Without Corbat | With Corbat | **Improvement** |
|--------|:--------------:|:-----------:|:---------------:|
| **Quality Score** | 4.6/10 | 7.7/10 | **+67%** |
| **Custom Errors** | 3 | 18 | **+500%** |
| **Interfaces/Ports** | 19 | 41 | **+116%** |
| **Files (modularity)** | 55 | 95 | **+73%** |

</div>

### By Complexity Level

| Category | Scenarios | Without | With | **Improvement** |
|----------|-----------|:-------:|:----:|:---------------:|
| **Basic** | UserService, REST API, React Form | 4.0 | 7.6 | **+90%** |
| **Intermediate** | Kafka Consumer, FastAPI, Go HTTP | 4.3 | 7.2 | **+67%** |
| **Advanced** | Saga, Circuit Breaker, Event Sourcing | 5.6 | 8.2 | **+46%** |

### Pattern Detection

| Pattern | Without Corbat | With Corbat |
|---------|:--------------:|:-----------:|
| Hexagonal Architecture | 0/10 scenarios | **10/10** |
| Repository Pattern | 2/10 | **7/10** |
| Custom Error Types | 1/10 | **8/10** |
| Dependency Injection | 2/10 | **10/10** |
| Saga Pattern | 0/10 | **1/1** (when needed) |

### Real Example: Saga Pattern (Scenario 07)

<table>
<tr>
<th>Without Corbat</th>
<th>With Corbat</th>
</tr>
<tr>
<td>

```java
// Hardcoded rollback, not extensible
try {
  targetAccount.credit(amount);
} catch (Exception e) {
  rollbackDebit(sourceAccount, amount);
  throw new TransferException(...);
}
```

</td>
<td>

```java
// Reusable Saga Pattern
public interface SagaStep<T> {
  void execute(T context);
  void compensate(T context);
}

public class SagaOrchestrator<T> {
  public void execute(T context) {
    for (SagaStep<T> step : steps) {
      step.execute(context);
      executedSteps.add(step);
    }
  }
  // Auto-rollback on failure
}
```

</td>
</tr>
<tr>
<td>9 files, 292 LOC, manual rollback</td>
<td><b>17 files, 707 LOC, orchestrated compensation</b></td>
</tr>
</table>

[View full benchmark analysis with 10 scenarios](benchmarks/v2/ANALYSIS.md)

---

## Code Comparison

### Before: Without Corbat MCP

```typescript
class UserService {
  private users: Map<string, User> = new Map();

  getById(id: string): User | undefined {
    return this.users.get(id);
  }

  createUser(input: CreateUserInput): User {
    if (!input.name) throw new Error('Name is required');
    const user = { id: uuidv4(), ...input };
    this.users.set(user.id, user);
    return user;
  }
}
// ✗ Returns undefined  ✗ Generic errors  ✗ No DI  ✗ Hardcoded storage
```

### After: With Corbat MCP

```typescript
// Port (interface)
interface UserRepository {
  findById(id: string): User | null;
  save(user: User): void;
  existsByEmail(email: string): boolean;
}

// Custom errors
class UserNotFoundError extends Error { /*...*/ }
class UserAlreadyExistsError extends Error { /*...*/ }
class InvalidUserInputError extends Error { /*...*/ }

// Service with DI
class UserService {
  constructor(
    private readonly repository: UserRepository,
    private readonly idGenerator: IdGenerator
  ) {}

  getUserById(id: string): User {
    const user = this.repository.findById(id);
    if (!user) throw new UserNotFoundError(id);
    return user;
  }

  createUser(input: CreateUserInput): User {
    this.validateInput(input);
    this.ensureEmailNotTaken(input.email);
    const user = createUser(this.idGenerator.generate(), input);
    this.repository.save(user);
    return user;
  }
}
// ✓ Repository interface  ✓ 3 custom errors  ✓ DI  ✓ 11 tests  ✓ Testable
```

**Result:** 3 files → 7 files | 129 LOC → 308 LOC | 0 interfaces → 4 interfaces | 0 custom errors → 3

---

## Built-in Profiles

| Profile | Stack | Architecture | Testing |
|---------|-------|--------------|---------|
| `java-spring-backend` | Java 21 + Spring Boot 3 | Hexagonal + DDD + CQRS | TDD, 80%+ coverage |
| `kotlin-spring` | Kotlin + Spring Boot 3 | Hexagonal + Coroutines | Kotest, MockK |
| `nodejs` | Node.js + TypeScript | Clean Architecture | Vitest |
| `nextjs` | Next.js 14+ | Feature-based + RSC | Vitest, Playwright |
| `react` | React 18+ | Feature-based | Testing Library |
| `vue` | Vue 3.5+ | Feature-based | Vitest |
| `angular` | Angular 19+ | Feature modules | Jest |
| `python` | Python + FastAPI | Hexagonal + async | pytest |
| `go` | Go 1.22+ | Clean + idiomatic | Table-driven tests |
| `rust` | Rust + Axum | Clean + ownership | Built-in + proptest |
| `csharp-dotnet` | C# 12 + ASP.NET Core 8 | Clean + CQRS | xUnit, FluentAssertions |
| `flutter` | Dart 3 + Flutter | Clean + BLoC/Riverpod | flutter_test |
| `minimal` | Any | Basic quality rules | Optional |

**Auto-detection:** Corbat reads `pom.xml`, `package.json`, `go.mod`, `Cargo.toml`, `pubspec.yaml`, `*.csproj` to select the right profile.

### Architecture Patterns Enforced

- **Hexagonal Architecture** — Ports & Adapters, infrastructure isolation
- **Domain-Driven Design** — Aggregates, Value Objects, Domain Events
- **SOLID Principles** — Single responsibility, dependency inversion
- **Clean Code** — Max 20 lines/method, meaningful names, no magic numbers
- **Error Handling** — Custom exceptions with context, no generic catches
- **Testing** — TDD workflow, unit + integration, mocking strategies

---

## Customize

### Ready-to-use templates

Copy a production-ready configuration for your stack:

**[Browse 14 templates](docs/templates.md)** — Java, Python, Node.js, React, Vue, Angular, Go, Kotlin, Rust, Flutter, and more.

### Generate a custom profile

```bash
npx corbat-init
```

Interactive wizard that auto-detects your stack and lets you configure architecture, DDD patterns, and quality metrics.

### Manual config

Create `.corbat.json` in your project root:

```json
{
  "profile": "java-spring-backend",
  "architecture": {
    "pattern": "hexagonal",
    "layers": ["domain", "application", "infrastructure", "api"]
  },
  "ddd": {
    "aggregates": true,
    "valueObjects": true,
    "domainEvents": true
  },
  "quality": {
    "maxMethodLines": 20,
    "maxClassLines": 200,
    "minCoverage": 80
  },
  "rules": {
    "always": ["Use records for DTOs", "Prefer Optional over null"],
    "never": ["Use field injection", "Catch generic Exception"]
  }
}
```

---

## How It Works

```
Your Prompt ──▶ Corbat MCP ──▶ AI + Standards
                    │
                    ├─ 1. Detect stack (pom.xml, package.json...)
                    ├─ 2. Classify task (feature, bugfix, refactor)
                    ├─ 3. Load profile with architecture rules
                    └─ 4. Inject guardrails before code generation
```

---

## Documentation

| Resource | Description |
|----------|-------------|
| [Setup Guide](docs/setup.md) | Installation for all 25+ tools |
| [Templates](docs/templates.md) | Ready-to-use `.corbat.json` configurations |
| [Compatibility](docs/compatibility.md) | Full list of supported tools |
| [Benchmark v2 Analysis](benchmarks/v2/ANALYSIS.md) | 10 scenarios with detailed comparison |
| [API Reference](docs/full-documentation.md) | Tools, prompts, and configuration |

---

<div align="center">

**Stop fixing AI code. Start shipping it.**

| Without Corbat | With Corbat |
|:--------------:|:-----------:|
| 4.6/10 quality | **7.7/10 quality** |
| 3 custom errors | **18 custom errors** |
| 0% hexagonal | **100% hexagonal** |

*Recommended by [corbat-tech](https://corbat.tech) — We use Claude Code internally, but Corbat MCP works with any MCP-compatible tool.*

</div>
