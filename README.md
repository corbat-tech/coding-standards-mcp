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

## Benchmark Results

Tested across 20 real-world scenarios:

| Metric | Without | With | Impact |
|--------|:-------:|:----:|:------:|
| **Quality Score** | 63/100 | 93/100 | +48% |
| **Code Smells** | 43 | 0 | -100% |
| **SOLID Compliance** | 50% | 89% | +78% |
| **Tests Generated** | 219 | 558 | +155% |
| **SonarQube** | FAIL | PASS | Fixed |

[View detailed benchmark report with code samples](docs/comparison-tests/RESULTS-REPORT.md)

---

## Code Comparison

### Before: Without Corbat MCP

```typescript
class UserService {
  private users: User[] = [];

  getUser(id: string) {
    return this.users.find(u => u.id === id);
  }

  createUser(name: string, email: string) {
    const user = { id: Date.now(), name, email };
    this.users.push(user);
    return user;
  }
}
// Problems: returns undefined, no validation, no DI, no tests
```

### After: With Corbat MCP

```typescript
interface UserRepository {
  findById(id: UserId): User | null;
  save(user: User): void;
}

class UserService {
  constructor(
    private readonly repository: UserRepository,
    private readonly idGenerator: IdGenerator
  ) {}

  getUser(id: UserId): User {
    const user = this.repository.findById(id);
    if (!user) throw new UserNotFoundError(id);
    return user;
  }

  createUser(input: CreateUserInput): User {
    this.validateInput(input);
    const user = User.create(
      this.idGenerator.generate(),
      input.name.trim(),
      input.email.toLowerCase()
    );
    this.repository.save(user);
    return user;
  }
}
// ✓ Dependency injection ✓ Custom errors ✓ Validation ✓ 15 tests
```

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
| [Benchmark Report](docs/comparison-tests/RESULTS-REPORT.md) | 20 real-world tests with code samples |
| [API Reference](docs/full-documentation.md) | Tools, prompts, and configuration |

---

<div align="center">

**Stop fixing AI code. Start shipping it.**

*Recommended by [corbat-tech](https://corbat.tech) — We use Claude Code internally, but Corbat MCP works with any MCP-compatible tool.*

</div>
