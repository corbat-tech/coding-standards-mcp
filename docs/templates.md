# Configuration Templates

Production-ready `.corbat.json` templates for your stack.

---

## How to Use

1. Find the template for your stack below
2. Copy the JSON configuration
3. Save as `.corbat.json` in your project root
4. Customize the rules as needed

> **Tip:** Templates are starting points. Modify `rules.always` and `rules.never` to match your team's conventions.

---

## Quick Navigation

### Backend
- [Java + Spring Boot (Hexagonal)](#java--spring-boot-hexagonal)
- [Python + FastAPI](#python--fastapi)
- [Node.js + TypeScript](#nodejs--typescript)
- [Go Microservices](#go-microservices)
- [Kotlin + Spring](#kotlin--spring)
- [C# + ASP.NET Core](#c--aspnet-core)
- [Rust Backend](#rust-backend)
- [Python + Django](#python--django)

### Frontend
- [React + TypeScript](#react--typescript)
- [Vue + TypeScript](#vue--typescript)
- [Angular Enterprise](#angular-enterprise)

### Full-Stack
- [Next.js Full-Stack](#nextjs-full-stack)

### Mobile
- [Flutter](#flutter)
- [React Native](#react-native)

---

## Backend Templates

---

### Java + Spring Boot (Hexagonal)

**Best for:** Enterprise applications, financial systems, banking, healthcare

**Stack:** Java 21+ | Spring Boot 3.x | Maven/Gradle

**Architecture:** Hexagonal (Ports & Adapters) + DDD + CQRS

```json
{
  "profile": "java-spring-backend",
  "architecture": {
    "pattern": "hexagonal",
    "layers": ["domain", "application", "infrastructure", "api"],
    "enforceLayerDependencies": true
  },
  "ddd": {
    "enabled": true,
    "ubiquitousLanguageEnforced": true,
    "patterns": {
      "aggregates": true,
      "entities": true,
      "valueObjects": true,
      "domainEvents": true,
      "repositories": true,
      "domainServices": true,
      "factories": true,
      "specifications": false
    },
    "valueObjectGuidelines": {
      "useRecords": true,
      "immutable": true,
      "selfValidating": true
    },
    "aggregateGuidelines": {
      "singleEntryPoint": true,
      "protectInvariants": true,
      "smallAggregates": true,
      "referenceByIdentity": true
    }
  },
  "cqrs": {
    "enabled": true,
    "separation": "logical",
    "patterns": {
      "commands": {
        "suffix": "Command",
        "handler": "CommandHandler"
      },
      "queries": {
        "suffix": "Query",
        "handler": "QueryHandler"
      }
    }
  },
  "eventDriven": {
    "enabled": true,
    "approach": "domain-events",
    "patterns": {
      "domainEvents": {
        "suffix": "Event",
        "pastTense": true
      },
      "eventPublishing": {
        "interface": "DomainEventPublisher",
        "async": true
      }
    }
  },
  "quality": {
    "maxMethodLines": 20,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 80,
    "requireDocumentation": true
  },
  "testing": {
    "framework": "JUnit5",
    "assertionLibrary": "AssertJ",
    "mockingLibrary": "Mockito",
    "types": {
      "unit": {
        "suffix": "Test",
        "location": "src/test/java",
        "coverage": 80
      },
      "integration": {
        "suffix": "IT",
        "useTestcontainers": true
      },
      "architecture": {
        "tool": "ArchUnit",
        "recommended": true
      }
    },
    "testcontainers": {
      "enabled": true,
      "containers": ["PostgreSQL", "Redis", "Kafka"]
    }
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "SLF4J + Logback",
      "format": "JSON",
      "structuredLogging": true,
      "correlationId": true
    },
    "metrics": {
      "framework": "Micrometer",
      "registry": "Prometheus"
    },
    "tracing": {
      "framework": "OpenTelemetry",
      "propagation": "W3C Trace Context"
    }
  },
  "errorHandling": {
    "format": "RFC 7807 Problem Details",
    "globalHandler": "@ControllerAdvice",
    "customExceptions": {
      "domain": ["DomainException", "ValidationException", "BusinessRuleViolationException"],
      "application": ["ApplicationException", "NotFoundException", "ConflictException"],
      "infrastructure": ["InfrastructureException", "ExternalServiceException"]
    }
  },
  "database": {
    "migrations": {
      "tool": "Flyway",
      "location": "db/migration",
      "naming": "V{version}__{description}.sql"
    },
    "auditing": {
      "enabled": true,
      "fields": ["createdAt", "createdBy", "updatedAt", "updatedBy"]
    },
    "softDelete": {
      "recommended": true,
      "field": "deletedAt"
    }
  },
  "mapping": {
    "tool": "MapStruct",
    "componentModel": "spring",
    "nullValueHandling": "RETURN_NULL"
  },
  "apiDocumentation": {
    "enabled": true,
    "tool": "SpringDoc OpenAPI",
    "version": "3.0"
  },
  "security": {
    "authentication": {
      "method": "JWT",
      "storage": "HTTP-only cookies"
    },
    "authorization": {
      "framework": "Spring Security",
      "method": "RBAC"
    },
    "practices": [
      "Never log sensitive data",
      "Validate all inputs",
      "Use parameterized queries",
      "Implement rate limiting"
    ]
  },
  "rules": {
    "always": [
      "Use constructor injection (never field injection)",
      "Use Java records for DTOs and Value Objects",
      "Prefer Optional<T> over null returns",
      "Use @Transactional at application service layer only",
      "Document public APIs with Javadoc",
      "Use sealed classes for domain exceptions",
      "Implement equals/hashCode for entities using ID only",
      "Use UUID for entity identifiers",
      "Apply validation annotations on DTOs",
      "Return domain objects from repositories, not entities"
    ],
    "never": [
      "Catch generic Exception",
      "Use @Autowired on fields",
      "Put business logic in controllers",
      "Return JPA entities from controllers",
      "Use primitive obsession (wrap primitives in Value Objects)",
      "Expose internal domain state",
      "Use static methods for business logic",
      "Skip null checks on external inputs",
      "Use System.out for logging",
      "Commit code with TODO comments in production"
    ]
  }
}
```

**Customization tips:**
- Adjust `minimumTestCoverage` based on project maturity (start with 60%, increase to 80%)
- Enable `specifications` pattern for complex query logic
- Set `cqrs.separation` to `"physical"` for high-scale read/write separation

---

### Python + FastAPI

**Best for:** Modern APIs, AI/ML services, async-first backends

**Stack:** Python 3.11+ | FastAPI 0.100+ | SQLAlchemy 2.0

**Architecture:** Hexagonal + Async-first

```json
{
  "profile": "python",
  "architecture": {
    "pattern": "hexagonal",
    "layers": ["domain", "application", "infrastructure", "api"],
    "enforceLayerDependencies": true
  },
  "ddd": {
    "enabled": true,
    "patterns": {
      "aggregates": true,
      "valueObjects": true,
      "domainEvents": true,
      "repositories": true,
      "domainServices": true
    }
  },
  "quality": {
    "maxMethodLines": 25,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 5,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 80,
    "requireDocumentation": true,
    "principles": [
      "Zen of Python",
      "Explicit is better than implicit",
      "Simple is better than complex"
    ]
  },
  "typeHints": {
    "required": true,
    "validation": "Pydantic v2",
    "strictMode": true,
    "runtimeValidation": true
  },
  "asyncPatterns": {
    "preferred": true,
    "libraries": ["asyncpg", "httpx", "aioredis", "aiokafka"],
    "avoidBlocking": true
  },
  "testing": {
    "framework": "pytest",
    "plugins": ["pytest-asyncio", "pytest-cov", "pytest-mock"],
    "httpClient": "httpx.AsyncClient",
    "patterns": {
      "arrange_act_assert": true,
      "given_when_then": true
    },
    "containers": ["PostgreSQL", "Redis", "MongoDB"]
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "structlog",
      "format": "JSON",
      "structuredLogging": true,
      "correlationId": true
    },
    "metrics": {
      "framework": "prometheus-client"
    },
    "tracing": {
      "framework": "OpenTelemetry"
    }
  },
  "errorHandling": {
    "format": "RFC 7807 Problem Details",
    "customExceptions": {
      "domain": ["DomainError", "ValidationError", "BusinessRuleError"],
      "application": ["ApplicationError", "NotFoundError", "ConflictError"],
      "infrastructure": ["InfrastructureError", "ExternalServiceError"]
    }
  },
  "database": {
    "orm": {
      "tool": "SQLAlchemy 2.0",
      "patterns": ["Repository", "Unit of Work"]
    },
    "migrations": {
      "tool": "Alembic",
      "naming": "{revision}_{slug}.py"
    }
  },
  "security": {
    "authentication": {
      "method": "JWT + OAuth2",
      "library": "python-jose"
    },
    "passwords": {
      "library": "passlib[bcrypt]"
    },
    "practices": [
      "Validate all inputs with Pydantic",
      "Use parameterized queries",
      "Never log sensitive data",
      "Implement rate limiting"
    ]
  },
  "rules": {
    "always": [
      "Use type hints for all function parameters and returns",
      "Use Pydantic models for request/response validation",
      "Use async/await for all I/O operations",
      "Use dependency injection with FastAPI Depends",
      "Follow PEP 8 and PEP 257 (docstrings)",
      "Use structlog for structured logging",
      "Use context managers for resource management",
      "Define __all__ in module __init__.py",
      "Use dataclasses or Pydantic for DTOs",
      "Implement proper exception handling per layer"
    ],
    "never": [
      "Use bare except clauses",
      "Mix sync and async code in same function",
      "Skip type annotations",
      "Use global mutable state",
      "Hardcode configuration values",
      "Use time.sleep() in async code",
      "Ignore type checker warnings",
      "Use print() for logging",
      "Store secrets in code",
      "Use * imports"
    ]
  }
}
```

**Customization tips:**
- For Django projects, use the [Python + Django](#python--django) template instead
- Disable `asyncPatterns.preferred` for sync-only projects
- Add AI/ML specific rules if using PyTorch/TensorFlow

---

### Node.js + TypeScript

**Best for:** REST APIs, microservices, real-time applications

**Stack:** Node.js 20+ | TypeScript 5.x | Express/Fastify

**Architecture:** Clean Architecture

```json
{
  "profile": "nodejs",
  "architecture": {
    "pattern": "clean",
    "layers": ["domain", "application", "infrastructure", "api"],
    "enforceLayerDependencies": true
  },
  "ddd": {
    "enabled": true,
    "patterns": {
      "aggregates": true,
      "valueObjects": true,
      "domainEvents": true,
      "repositories": true
    }
  },
  "quality": {
    "maxMethodLines": 25,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 80,
    "requireDocumentation": true
  },
  "typescript": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUncheckedIndexedAccess": true,
    "moduleResolution": "NodeNext"
  },
  "testing": {
    "framework": "Vitest",
    "mockingLibrary": "vitest mocks",
    "patterns": {
      "arrange_act_assert": true,
      "isolatedTests": true
    },
    "containers": ["PostgreSQL", "Redis", "Kafka", "MongoDB"]
  },
  "errorHandling": {
    "customErrorClasses": true,
    "errorProperties": ["code", "statusCode", "isOperational", "context"],
    "asyncErrorHandling": "try-catch with async/await",
    "globalErrorHandler": true
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "Pino",
      "format": "JSON",
      "structuredLogging": true,
      "correlationId": true
    },
    "metrics": {
      "framework": "prom-client"
    },
    "tracing": {
      "framework": "OpenTelemetry"
    }
  },
  "database": {
    "orm": {
      "tool": "Prisma",
      "patterns": ["Repository"]
    },
    "migrations": {
      "tool": "Prisma Migrate"
    }
  },
  "validation": {
    "library": "Zod",
    "location": "api layer",
    "runtime": true
  },
  "security": {
    "authentication": {
      "method": "JWT",
      "library": "jose"
    },
    "practices": [
      "Validate all inputs with Zod",
      "Use parameterized queries",
      "Implement rate limiting",
      "Set security headers (helmet)"
    ]
  },
  "rules": {
    "always": [
      "Use TypeScript strict mode",
      "Use native ESM modules (type: module)",
      "Use async/await over callbacks/promises",
      "Validate all inputs with Zod schemas",
      "Use dependency injection (tsyringe/inversify)",
      "Log with structured JSON (Pino)",
      "Use Result types for error handling",
      "Define explicit return types",
      "Use readonly for immutable properties",
      "Implement graceful shutdown"
    ],
    "never": [
      "Use any type (use unknown instead)",
      "Use CommonJS require()",
      "Swallow errors silently",
      "Use synchronous I/O (fs.readFileSync)",
      "Skip input validation",
      "Use var (use const/let)",
      "Mutate function parameters",
      "Use console.log for production logging",
      "Store secrets in code",
      "Use non-null assertion (!) without validation"
    ]
  }
}
```

**Customization tips:**
- For NestJS, add framework-specific decorators to rules
- Use `"pattern": "hexagonal"` for larger enterprise projects
- Add GraphQL rules if using Apollo/GraphQL

---

### Go Microservices

**Best for:** High-performance microservices, cloud-native apps, CLI tools

**Stack:** Go 1.22+ | Standard library + minimal dependencies

**Architecture:** Clean Architecture + Idiomatic Go

```json
{
  "profile": "go",
  "architecture": {
    "pattern": "clean",
    "layers": ["domain", "usecase", "interface", "infrastructure"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 300,
    "maxFileLines": 500,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70,
    "requireDocumentation": true
  },
  "idiomaticGo": {
    "errorHandling": {
      "returnErrorsLast": true,
      "wrapWithContext": true,
      "useErrorsIs": true,
      "useErrorsAs": true,
      "noPanicsInLibraries": true
    },
    "naming": {
      "shortVariables": true,
      "acronymsUppercase": true,
      "interfaceSuffix": "er"
    },
    "patterns": {
      "functionalOptions": true,
      "tableDrivenTests": true,
      "deferForCleanup": true
    }
  },
  "testing": {
    "framework": "testing (stdlib)",
    "patterns": {
      "tableDrivenTests": true,
      "subtests": true,
      "parallelTests": true
    },
    "tools": ["go test", "testify", "gomock"],
    "coverage": {
      "tool": "go test -cover",
      "minimum": 70
    }
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "slog (stdlib)",
      "format": "JSON",
      "structuredLogging": true
    },
    "metrics": {
      "framework": "prometheus/client_golang"
    },
    "tracing": {
      "framework": "OpenTelemetry"
    }
  },
  "errorHandling": {
    "pattern": "errors as values",
    "wrapping": "fmt.Errorf with %w",
    "sentinelErrors": true,
    "customErrorTypes": true
  },
  "concurrency": {
    "patterns": ["goroutines", "channels", "context"],
    "avoidRaceConditions": true,
    "useContext": true,
    "gracefulShutdown": true
  },
  "rules": {
    "always": [
      "Return error as last return value",
      "Check all errors explicitly",
      "Wrap errors with context using fmt.Errorf(%w)",
      "Use context.Context for cancellation",
      "Use table-driven tests",
      "Use defer for cleanup",
      "Keep functions short and focused",
      "Use interfaces for dependencies",
      "Document exported functions",
      "Use go fmt and go vet"
    ],
    "never": [
      "Use panic for regular errors",
      "Ignore returned errors (use _ only intentionally)",
      "Use global mutable state",
      "Use init() for complex logic",
      "Create goroutines without ownership",
      "Use naked returns in long functions",
      "Skip error wrapping",
      "Use fmt.Print for logging",
      "Embed mutexes in public structs",
      "Use reflect without necessity"
    ]
  }
}
```

**Customization tips:**
- Increase `maxMethodLines` to 40 for generated code
- Add gRPC-specific rules for RPC services
- Enable `functionalOptions` pattern for configurable structs

---

### Kotlin + Spring

**Best for:** Modern JVM backends, Android backends, null-safe enterprise apps

**Stack:** Kotlin 1.9+ | Spring Boot 3.x | Coroutines

**Architecture:** Hexagonal + Null-safety

```json
{
  "profile": "kotlin-spring",
  "architecture": {
    "pattern": "hexagonal",
    "layers": ["domain", "application", "infrastructure", "api"],
    "enforceLayerDependencies": true
  },
  "ddd": {
    "enabled": true,
    "patterns": {
      "aggregates": true,
      "valueObjects": true,
      "domainEvents": true,
      "repositories": true
    }
  },
  "quality": {
    "maxMethodLines": 20,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 80,
    "requireDocumentation": true
  },
  "kotlinFeatures": {
    "nullSafety": {
      "enforced": true,
      "avoidDoubleBang": true,
      "preferSafeCall": true,
      "useElvisOperator": true
    },
    "coroutines": {
      "enabled": true,
      "structuredConcurrency": true,
      "useFlow": true,
      "suspendFunctions": true
    },
    "functional": {
      "sealedClasses": true,
      "dataClasses": true,
      "extensionFunctions": true,
      "scopeFunctions": true
    }
  },
  "testing": {
    "framework": "JUnit5",
    "assertionLibrary": "Kotest assertions",
    "mockingLibrary": "MockK",
    "coroutineTesting": "kotlinx-coroutines-test",
    "containers": ["PostgreSQL", "Redis"]
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "kotlin-logging + SLF4J",
      "format": "JSON"
    },
    "metrics": {
      "framework": "Micrometer"
    }
  },
  "rules": {
    "always": [
      "Use data classes for DTOs",
      "Use sealed classes for domain states",
      "Prefer val over var",
      "Use safe call operator (?.) over !!",
      "Use Elvis operator (?:) for defaults",
      "Use extension functions for utilities",
      "Use coroutines for async operations",
      "Use Flow for reactive streams",
      "Leverage Kotlin DSLs (bean, router)",
      "Use scope functions appropriately (let, run, apply)"
    ],
    "never": [
      "Use !! (double-bang) in production code",
      "Use Java-style null checks",
      "Create mutable data classes",
      "Use lateinit for nullable types",
      "Block coroutine threads",
      "Use runBlocking in production",
      "Ignore compiler warnings",
      "Use platform types from Java carelessly",
      "Create classes when objects suffice",
      "Use var when val works"
    ]
  }
}
```

**Customization tips:**
- Disable coroutines for blocking-only projects
- Add Android-specific rules for mobile backends
- Use Ktor instead of Spring for lightweight services

---

### C# + ASP.NET Core

**Best for:** Microsoft ecosystem, enterprise apps, Azure-native services

**Stack:** C# 12+ | .NET 8+ | ASP.NET Core

**Architecture:** Clean Architecture + CQRS

```json
{
  "profile": "csharp-dotnet",
  "architecture": {
    "pattern": "clean",
    "layers": ["Domain", "Application", "Infrastructure", "WebApi"],
    "enforceLayerDependencies": true
  },
  "ddd": {
    "enabled": true,
    "patterns": {
      "aggregates": true,
      "valueObjects": true,
      "domainEvents": true,
      "repositories": true
    }
  },
  "cqrs": {
    "enabled": true,
    "library": "MediatR",
    "patterns": {
      "commands": {
        "suffix": "Command",
        "handler": "CommandHandler"
      },
      "queries": {
        "suffix": "Query",
        "handler": "QueryHandler"
      }
    }
  },
  "quality": {
    "maxMethodLines": 25,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 80,
    "requireDocumentation": true
  },
  "csharpFeatures": {
    "nullableReferenceTypes": true,
    "records": true,
    "patternMatching": true,
    "primaryConstructors": true,
    "fileScoped": true
  },
  "testing": {
    "framework": "xUnit",
    "assertionLibrary": "FluentAssertions",
    "mockingLibrary": "NSubstitute",
    "architectureTests": "NetArchTest",
    "containers": ["PostgreSQL", "Redis", "SqlServer"]
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "Serilog",
      "format": "JSON",
      "sinks": ["Console", "Seq", "ApplicationInsights"]
    },
    "metrics": {
      "framework": "OpenTelemetry"
    }
  },
  "errorHandling": {
    "format": "RFC 7807 Problem Details",
    "globalHandler": "ExceptionMiddleware",
    "resultPattern": "FluentResults or OneOf"
  },
  "database": {
    "orm": {
      "tool": "Entity Framework Core",
      "patterns": ["Repository", "Unit of Work"]
    },
    "migrations": {
      "tool": "EF Core Migrations"
    }
  },
  "validation": {
    "library": "FluentValidation",
    "location": "Application layer"
  },
  "rules": {
    "always": [
      "Enable nullable reference types",
      "Use records for DTOs and Value Objects",
      "Use primary constructors for DI",
      "Use async/await for I/O operations",
      "Use MediatR for CQRS",
      "Use FluentValidation for input validation",
      "Use Result pattern for error handling",
      "Document public APIs with XML comments",
      "Use file-scoped namespaces",
      "Follow Microsoft naming conventions"
    ],
    "never": [
      "Use null without nullable annotation",
      "Catch generic Exception",
      "Use async void (except event handlers)",
      "Skip input validation",
      "Return Entity Framework entities from API",
      "Use static classes for business logic",
      "Ignore compiler warnings",
      "Use #pragma to suppress warnings without justification",
      "Use dynamic type in business logic",
      "Expose internal implementation details"
    ]
  }
}
```

**Customization tips:**
- Use `Ardalis.Result` for Result pattern
- Add Minimal API rules for lightweight services
- Enable Blazor rules for full-stack .NET

---

### Rust Backend

**Best for:** High-performance systems, WebAssembly, safety-critical applications

**Stack:** Rust 1.75+ | Axum/Actix-web | Tokio

**Architecture:** Clean + Ownership-driven

```json
{
  "profile": "rust",
  "architecture": {
    "pattern": "clean",
    "layers": ["domain", "application", "infrastructure", "api"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 300,
    "maxFileLines": 500,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70,
    "requireDocumentation": true
  },
  "rustFeatures": {
    "ownership": {
      "borrowCheckerCompliance": true,
      "preferSlices": true,
      "avoidClone": true,
      "useLifetimes": true
    },
    "errorHandling": {
      "useResult": true,
      "useOption": true,
      "customErrorTypes": true,
      "useThiserror": true,
      "useAnyhow": true
    },
    "async": {
      "runtime": "Tokio",
      "useFutures": true
    }
  },
  "testing": {
    "framework": "built-in (#[test])",
    "patterns": {
      "unitTests": true,
      "integrationTests": true,
      "docTests": true
    },
    "tools": ["cargo test", "proptest"]
  },
  "observability": {
    "enabled": true,
    "logging": {
      "framework": "tracing",
      "format": "JSON"
    },
    "metrics": {
      "framework": "metrics-rs"
    }
  },
  "rules": {
    "always": [
      "Use Result<T, E> for fallible operations",
      "Use Option<T> for nullable values",
      "Use &[T] slices over Vec<T> in function params",
      "Propagate errors with ? operator",
      "Wrap errors with context (thiserror/anyhow)",
      "Use #[derive] for common traits",
      "Document public items with /// comments",
      "Use clippy and rustfmt",
      "Prefer iterators over loops",
      "Use const for compile-time values"
    ],
    "never": [
      "Use panic! for regular errors",
      "Use unwrap() in production code",
      "Use unsafe without documentation",
      "Ignore clippy warnings",
      "Use .clone() without necessity",
      "Use mut when immutable works",
      "Skip error handling with _",
      "Use global mutable state",
      "Ignore borrow checker errors",
      "Use expect() without meaningful message"
    ]
  }
}
```

**Customization tips:**
- Use `anyhow` for application code, `thiserror` for libraries
- Add WebAssembly-specific rules for WASM targets
- Enable `unsafe` rules for FFI/low-level code

---

### Python + Django

**Best for:** Full-featured web apps, admin systems, legacy Python projects

**Stack:** Python 3.11+ | Django 5.x | Django REST Framework

**Architecture:** MTV (Model-Template-View) + Service Layer

```json
{
  "profile": "python-django",
  "architecture": {
    "pattern": "layered",
    "layers": ["models", "services", "views", "serializers", "urls"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 25,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 5,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 75
  },
  "django": {
    "apps": {
      "singleResponsibility": true,
      "maxModelsPerApp": 10
    },
    "models": {
      "useManagers": true,
      "useQuerySets": true,
      "softDelete": true
    },
    "views": {
      "preferClassBased": true,
      "useSerializers": true
    }
  },
  "typeHints": {
    "required": true,
    "validation": "Pydantic or Django serializers",
    "useTypeStubs": true
  },
  "testing": {
    "framework": "pytest-django",
    "plugins": ["pytest-cov", "pytest-factoryboy"],
    "patterns": {
      "factories": true,
      "fixtures": true
    },
    "coverage": 75
  },
  "database": {
    "migrations": {
      "tool": "Django Migrations",
      "squashing": true
    },
    "optimization": {
      "selectRelated": true,
      "prefetchRelated": true,
      "deferFields": true
    }
  },
  "security": {
    "middleware": ["CSRFViewMiddleware", "SecurityMiddleware"],
    "practices": [
      "Use Django ORM (no raw SQL)",
      "Enable CSRF protection",
      "Use django-environ for secrets"
    ]
  },
  "rules": {
    "always": [
      "Use type hints for all functions",
      "Use Django ORM instead of raw SQL",
      "Use select_related/prefetch_related for queries",
      "Use Django signals sparingly",
      "Create fat models, thin views",
      "Use Django REST Framework serializers",
      "Use factory_boy for test data",
      "Use django-environ for configuration",
      "Follow Django coding style",
      "Use custom managers for complex queries"
    ],
    "never": [
      "Use raw SQL without justification",
      "Put business logic in views",
      "Use signals for business logic",
      "Skip migrations",
      "Use global state",
      "Hardcode configuration",
      "Use generic Exception handlers",
      "Skip authentication on endpoints",
      "Use N+1 queries",
      "Ignore Django security warnings"
    ]
  }
}
```

**Customization tips:**
- For async Django, add `async/await` rules
- Use Django Ninja for modern API-first projects
- Add Celery rules for background tasks

---

## Frontend Templates

---

### React + TypeScript

**Best for:** Single-page applications, component libraries, modern UIs

**Stack:** React 18+ | TypeScript 5.x | Vite

**Architecture:** Feature-based

```json
{
  "profile": "react",
  "architecture": {
    "pattern": "feature-based",
    "layers": ["features", "shared", "core"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 200,
    "maxFileLines": 300,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70
  },
  "components": {
    "type": "functional",
    "onePerFile": true,
    "maxLines": 200,
    "naming": "PascalCase"
  },
  "stateManagement": {
    "local": ["useState", "useReducer"],
    "shared": ["Zustand", "Context"],
    "server": ["TanStack Query", "SWR"],
    "avoid": ["Redux for simple apps", "prop drilling"]
  },
  "testing": {
    "framework": "Vitest",
    "componentTesting": "React Testing Library",
    "e2e": "Playwright",
    "patterns": {
      "behaviorTesting": true,
      "userEventSimulation": true,
      "accessibilityTesting": true,
      "avoidImplementationDetails": true
    }
  },
  "accessibility": {
    "standard": "WCAG 2.1 AA",
    "tools": ["eslint-plugin-jsx-a11y", "axe-core"],
    "required": true
  },
  "performance": {
    "metrics": {
      "LCP": "< 2.5s",
      "FID": "< 100ms",
      "CLS": "< 0.1"
    },
    "patterns": ["lazy loading", "memoization", "code splitting"]
  },
  "styling": {
    "preferred": ["Tailwind CSS", "CSS Modules"],
    "avoid": ["inline styles", "CSS-in-JS runtime"]
  },
  "typescript": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true
  },
  "rules": {
    "always": [
      "Use functional components with hooks",
      "One component per file",
      "Extract custom hooks for reusable logic",
      "Use TypeScript strict mode",
      "Implement error boundaries",
      "Follow WCAG 2.1 AA accessibility",
      "Use semantic HTML elements",
      "Memoize expensive computations",
      "Use React.lazy for code splitting",
      "Test behavior, not implementation"
    ],
    "never": [
      "Use class components (except error boundaries)",
      "Mutate state directly",
      "Use any type",
      "Skip prop validation",
      "Use inline styles for theming",
      "Use index as key (with reordering)",
      "Fetch data in useEffect (use React Query)",
      "Use useEffect for derived state",
      "Create components inside components",
      "Ignore accessibility warnings"
    ]
  }
}
```

**Customization tips:**
- Add `"server": ["TanStack Query"]` for data fetching patterns
- Use `"styling": "styled-components"` if using CSS-in-JS
- Add Storybook rules for component libraries

---

### Vue + TypeScript

**Best for:** Progressive web apps, dashboards, gradual adoption projects

**Stack:** Vue 3.5+ | TypeScript 5.x | Vite

**Architecture:** Feature-based + Composition API

```json
{
  "profile": "vue",
  "architecture": {
    "pattern": "feature-based",
    "layers": ["features", "shared", "core"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 200,
    "maxFileLines": 300,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70
  },
  "vueFeatures": {
    "compositionAPI": {
      "required": true,
      "scriptSetup": true
    },
    "components": {
      "singleFileComponents": true,
      "maxLines": 200
    },
    "composables": {
      "extractLogic": true,
      "prefix": "use"
    }
  },
  "stateManagement": {
    "local": ["ref", "reactive", "computed"],
    "shared": ["Pinia"],
    "server": ["TanStack Query", "VueQuery"]
  },
  "testing": {
    "framework": "Vitest",
    "componentTesting": "Vue Test Utils",
    "e2e": "Playwright",
    "patterns": {
      "mountComponents": true,
      "testComposables": true,
      "stubChildComponents": true
    }
  },
  "typescript": {
    "strict": true,
    "volar": true,
    "propTypes": true
  },
  "rules": {
    "always": [
      "Use Composition API with <script setup>",
      "Extract logic into composables",
      "Use TypeScript for all components",
      "Define props with withDefaults(defineProps<T>())",
      "Define emits with defineEmits<T>()",
      "Use Pinia for global state",
      "Use computed for derived state",
      "Use v-model for two-way binding",
      "Follow Vue style guide (Priority A & B)",
      "Test composables in isolation"
    ],
    "never": [
      "Use Options API in new code",
      "Mutate props directly",
      "Use mixins (use composables instead)",
      "Skip TypeScript in components",
      "Use this in <script setup>",
      "Create watchers for derived state",
      "Use v-html without sanitization",
      "Skip key in v-for",
      "Use $refs for data flow",
      "Use global event bus"
    ]
  }
}
```

**Customization tips:**
- Use Nuxt template for full-stack Vue
- Add VueUse composables to recommended libraries
- Enable SSR rules for server-rendered apps

---

### Angular Enterprise

**Best for:** Large enterprise apps, complex forms, strict typing

**Stack:** Angular 19+ | TypeScript 5.x | RxJS

**Architecture:** Feature modules + Standalone components

```json
{
  "profile": "angular",
  "architecture": {
    "pattern": "feature-based",
    "layers": ["features", "shared", "core"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 25,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70
  },
  "angularFeatures": {
    "standalone": {
      "required": true,
      "lazyLoading": true
    },
    "signals": {
      "preferred": true,
      "replaceObservables": true
    },
    "changeDetection": {
      "strategy": "OnPush",
      "zoneless": true
    }
  },
  "stateManagement": {
    "local": ["signals", "signal stores"],
    "shared": ["NgRx SignalStore", "Akita"],
    "server": ["NgRx ComponentStore"]
  },
  "testing": {
    "framework": "Jest",
    "componentTesting": "Angular Testing Library",
    "e2e": "Playwright",
    "patterns": {
      "shallowRendering": true,
      "mockServices": true,
      "harnesses": true
    }
  },
  "rxjs": {
    "operators": ["map", "filter", "switchMap", "catchError"],
    "patterns": {
      "asyncPipe": true,
      "takeUntilDestroy": true,
      "avoidSubscribe": true
    }
  },
  "forms": {
    "preferred": "Reactive Forms",
    "validation": "Custom validators",
    "typedForms": true
  },
  "rules": {
    "always": [
      "Use standalone components",
      "Use signals for state",
      "Use OnPush change detection",
      "Use async pipe for observables",
      "Use typed reactive forms",
      "Use Angular CDK for UI patterns",
      "Implement lazy loading per feature",
      "Use takeUntilDestroyed for subscriptions",
      "Follow Angular style guide",
      "Use dependency injection properly"
    ],
    "never": [
      "Use NgModules for new code",
      "Subscribe in components (use async pipe)",
      "Use ChangeDetection.Default",
      "Use template-driven forms for complex forms",
      "Skip unsubscribing from observables",
      "Use any type",
      "Put logic in templates",
      "Use setTimeout for change detection",
      "Create circular dependencies",
      "Skip lazy loading for features"
    ]
  }
}
```

**Customization tips:**
- Use `"signals": false` for Angular 16 or earlier
- Add NgRx effects for complex async flows
- Enable SSR rules for Angular Universal

---

## Full-Stack Templates

---

### Next.js Full-Stack

**Best for:** SaaS applications, marketing sites, e-commerce

**Stack:** Next.js 14+ | TypeScript 5.x | App Router

**Architecture:** Feature-based + Server Components

```json
{
  "profile": "nextjs",
  "architecture": {
    "pattern": "feature-based",
    "layers": ["app", "features", "shared", "lib"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 200,
    "maxFileLines": 300,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70
  },
  "nextjsFeatures": {
    "appRouter": {
      "required": true,
      "serverComponents": true,
      "serverActions": true
    },
    "rendering": {
      "defaultSSR": true,
      "clientBoundaries": "minimal",
      "streaming": true
    },
    "dataFetching": {
      "serverComponents": true,
      "caching": "automatic",
      "revalidation": "on-demand"
    }
  },
  "serverComponents": {
    "default": true,
    "clientOnly": ["forms", "interactivity", "browser APIs"],
    "serverOnly": ["data fetching", "secrets", "database"]
  },
  "testing": {
    "framework": "Vitest",
    "componentTesting": "React Testing Library",
    "e2e": "Playwright",
    "patterns": {
      "testServerComponents": true,
      "mockServerActions": true
    }
  },
  "performance": {
    "coreWebVitals": {
      "LCP": "< 2.5s",
      "FID": "< 100ms",
      "CLS": "< 0.1"
    },
    "optimizations": [
      "next/image",
      "next/font",
      "dynamic imports",
      "route prefetching"
    ]
  },
  "typescript": {
    "strict": true,
    "noImplicitAny": true
  },
  "rules": {
    "always": [
      "Use App Router (not Pages Router)",
      "Use Server Components by default",
      "Use Server Actions for mutations",
      "Implement loading.tsx for suspense",
      "Implement error.tsx for error boundaries",
      "Use TypeScript strict mode",
      "Optimize images with next/image",
      "Use next/font for fonts",
      "Implement metadata for SEO",
      "Use route handlers for APIs"
    ],
    "never": [
      "Use 'use client' unnecessarily",
      "Fetch data in client components",
      "Skip error boundaries",
      "Use inline styles for theming",
      "Ignore Core Web Vitals",
      "Use Pages Router in new projects",
      "Skip loading states",
      "Use getServerSideProps (App Router)",
      "Expose secrets in client code",
      "Skip image optimization"
    ]
  }
}
```

**Customization tips:**
- Add Prisma rules for database layer
- Use `"caching": "manual"` for dynamic apps
- Add NextAuth rules for authentication

---

## Mobile Templates

---

### Flutter

**Best for:** Cross-platform mobile apps (iOS, Android, Web, Desktop)

**Stack:** Dart 3.x | Flutter 3.x | Material/Cupertino

**Architecture:** Clean Architecture + BLoC/Riverpod

```json
{
  "profile": "flutter",
  "architecture": {
    "pattern": "clean",
    "layers": ["domain", "data", "presentation"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 200,
    "maxFileLines": 400,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 70
  },
  "flutter": {
    "widgets": {
      "composition": true,
      "maxBuildMethodLines": 50,
      "extractWidgets": true
    },
    "stateManagement": {
      "preferred": ["Riverpod", "BLoC"],
      "patterns": ["Provider", "StateNotifier"]
    },
    "navigation": {
      "router": "go_router",
      "deepLinking": true
    }
  },
  "dartFeatures": {
    "nullSafety": true,
    "soundNullSafety": true,
    "records": true,
    "patterns": true
  },
  "testing": {
    "framework": "flutter_test",
    "patterns": {
      "unitTests": true,
      "widgetTests": true,
      "integrationTests": true,
      "goldenTests": true
    },
    "mocking": "mocktail"
  },
  "rules": {
    "always": [
      "Use null safety",
      "Extract widgets for readability",
      "Use const constructors",
      "Use Riverpod or BLoC for state",
      "Separate UI from business logic",
      "Use go_router for navigation",
      "Implement proper error handling",
      "Use freezed for immutable models",
      "Follow Flutter style guide",
      "Write widget tests for all screens"
    ],
    "never": [
      "Use setState for complex state",
      "Put business logic in widgets",
      "Skip null safety",
      "Use global variables",
      "Create huge build methods",
      "Skip widget tests",
      "Use String for typed IDs",
      "Ignore lint warnings",
      "Use dynamic unnecessarily",
      "Skip const constructors"
    ]
  }
}
```

**Customization tips:**
- Use `"stateManagement": "GetX"` for simpler apps
- Add platform-specific rules (iOS/Android)
- Enable web-specific rules for Flutter Web

---

### React Native

**Best for:** Mobile apps with JavaScript teams, code sharing with web

**Stack:** React Native 0.73+ | TypeScript | Expo

**Architecture:** Feature-based

```json
{
  "profile": "react-native",
  "architecture": {
    "pattern": "feature-based",
    "layers": ["features", "shared", "core", "navigation"],
    "enforceLayerDependencies": true
  },
  "quality": {
    "maxMethodLines": 30,
    "maxClassLines": 200,
    "maxFileLines": 300,
    "maxMethodParameters": 4,
    "maxCyclomaticComplexity": 10,
    "minimumTestCoverage": 60
  },
  "reactNative": {
    "architecture": {
      "newArchitecture": true,
      "fabric": true,
      "turboModules": true
    },
    "expo": {
      "managed": true,
      "prebuild": true
    },
    "navigation": {
      "library": "React Navigation",
      "typeSafe": true
    }
  },
  "stateManagement": {
    "local": ["useState", "useReducer"],
    "shared": ["Zustand", "Jotai"],
    "server": ["TanStack Query"],
    "persistence": ["MMKV", "AsyncStorage"]
  },
  "testing": {
    "framework": "Jest",
    "componentTesting": "React Native Testing Library",
    "e2e": "Detox",
    "patterns": {
      "behaviorTesting": true,
      "mockNativeModules": true
    }
  },
  "performance": {
    "optimizations": [
      "FlatList for lists",
      "useMemo for expensive computations",
      "useCallback for callbacks",
      "Hermes engine"
    ]
  },
  "typescript": {
    "strict": true,
    "paths": true
  },
  "rules": {
    "always": [
      "Use functional components",
      "Use TypeScript strict mode",
      "Use Expo for managed workflow",
      "Use React Navigation for navigation",
      "Use FlatList for long lists",
      "Implement proper error boundaries",
      "Use MMKV for fast storage",
      "Optimize images and assets",
      "Test with React Native Testing Library",
      "Use Hermes for Android"
    ],
    "never": [
      "Use ScrollView for long lists",
      "Skip TypeScript",
      "Use inline styles extensively",
      "Ignore performance warnings",
      "Use synchronous storage",
      "Skip platform-specific code when needed",
      "Use any type",
      "Ignore accessibility",
      "Skip error handling",
      "Use deprecated APIs"
    ]
  }
}
```

**Customization tips:**
- Disable Expo for bare React Native projects
- Add native module rules for ejected projects
- Use `"e2e": "Maestro"` as Detox alternative

---

## Create Your Own

These templates are starting points. To create a custom configuration:

1. Start with the closest template
2. Modify `architecture.pattern` and `layers` for your structure
3. Adjust `quality` metrics for your team's standards
4. Customize `rules.always` and `rules.never` for your conventions

### Example: Combining Templates

For a **Next.js + Python FastAPI** project:

```json
{
  "profile": "custom-fullstack",
  "frontend": {
    "extends": "nextjs"
  },
  "backend": {
    "extends": "python-fastapi"
  },
  "rules": {
    "always": [
      "Use OpenAPI for API contracts",
      "Share types between frontend and backend",
      "Use Docker for local development"
    ]
  }
}
```

---

[Back to README](../README.md) | [Setup Guide](setup.md) | [Full Documentation](full-documentation.md)
