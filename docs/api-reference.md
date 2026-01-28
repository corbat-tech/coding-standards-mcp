# API Reference

Complete reference for Corbat MCP tools, resources, and prompts.

## Tools

### get_context

**Primary tool** - Returns complete coding standards context for a task.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `task` | string | Yes | Description of what to implement |
| `project_dir` | string | No | Project directory for auto-detection |

**Returns:**
- Detected stack (language, framework, build tool)
- Task type classification
- MUST rules (mandatory guidelines)
- AVOID rules (anti-patterns)
- Code quality thresholds
- Naming conventions
- Recommended workflow

**Example:**
```json
{
  "task": "Create payment service with Stripe integration",
  "project_dir": "/path/to/my-project"
}
```

---

### validate

Validate code against coding standards.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `code` | string | Yes | The code to validate |
| `task_type` | enum | No | One of: `feature`, `bugfix`, `refactor`, `test` |

**Returns:**
- Code quality thresholds
- Guardrails for task type
- Review checklist template

**Example:**
```json
{
  "code": "public class UserService { ... }",
  "task_type": "feature"
}
```

---

### search

Search standards documentation for specific topics.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | Yes | Search terms |

**Returns:** Up to 5 matching results with excerpts.

**Example Queries:**
- `kafka` - Kafka messaging patterns
- `testing` - Testing guidelines
- `docker` - Docker configuration
- `archunit` - Architecture testing

**Example:**
```json
{
  "query": "kafka consumer"
}
```

---

### profiles

List all available coding standards profiles.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| (none) | - | - | - |

**Returns:** List of profile IDs with descriptions.

**Available Profiles:**
- `java-spring-backend` - Enterprise Java with Hexagonal Architecture
- `kotlin-spring` - Kotlin with Spring Boot
- `nodejs` - Node.js/TypeScript
- `nextjs` - Next.js 14+
- `react` - React 18+
- `vue` - Vue 3.5+
- `angular` - Angular 19+
- `python` - Python with FastAPI
- `go` - Go 1.22+
- `rust` - Rust with Axum
- `csharp-dotnet` - C# with ASP.NET Core
- `flutter` - Flutter/Dart
- `minimal` - Basic rules only

---

### health

Check server status and usage metrics.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| (none) | - | - | - |

**Returns:**
- Server status (OK/ERROR)
- Version
- Uptime
- Profiles loaded count
- Standards documents count
- Usage metrics (if available)

---

### init

Generate a `.corbat.json` configuration file.

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `project_dir` | string | Yes | Project directory to analyze |

**Returns:**
- Detected stack information
- Suggested `.corbat.json` content
- Available profiles list
- Setup instructions

**Example:**
```json
{
  "project_dir": "/path/to/my-project"
}
```

---

## Resources

Resources provide direct access to profiles and standards.

### Profile Resources

URI format: `corbat://profile/{profile-id}`

**Example:** `corbat://profile/java-spring-backend`

Returns the complete profile configuration in YAML format.

### Standard Resources

URI format: `corbat://standard/{standard-id}`

**Example:** `corbat://standard/testing-guidelines`

Returns the standard document content in Markdown format.

---

## Prompts

### implement

Guided implementation prompt for new features.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `task` | string | Yes | What to implement |
| `project_dir` | string | No | Project directory |

**Returns:** Complete implementation guide with:
- Task classification
- Guardrails
- TDD workflow
- Review checklist

### review

Expert code review prompt.

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `code` | string | Yes | Code to review |
| `role` | enum | No | Expert role: `architect`, `backend`, `security`, `performance`, `frontend` |

**Returns:** Structured review with:
- Role-specific perspective
- CRITICAL issues
- WARNINGS
- SUGGESTIONS
- Compliance score (1-10)

---

## Configuration

### .corbat.json

Project-level configuration file.

```json
{
  "profile": "java-spring-backend",
  "autoInject": true,
  "rules": {
    "always": ["Use constructor injection"],
    "onNewFile": ["Add file header"],
    "onTest": ["Follow AAA pattern"],
    "onRefactor": ["Ensure tests pass"]
  },
  "overrides": {
    "maxMethodLines": 25,
    "minimumTestCoverage": 90
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `profile` | string | Profile ID to use |
| `autoInject` | boolean | Auto-inject guardrails |
| `rules` | object | Custom rules by context |
| `overrides` | object | Override profile thresholds |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `CORBAT_PROFILES_DIR` | `./profiles` | Profiles directory |
| `CORBAT_STANDARDS_DIR` | `./standards` | Standards directory |
| `CORBAT_DEFAULT_PROFILE` | `java-spring-backend` | Default profile |
| `CORBAT_LOG_LEVEL` | `info` | Log level (debug/info/warn/error) |
| `CORBAT_CACHE_TTL_MS` | `60000` | Cache TTL in milliseconds |

---

## Error Codes

| Code | Description |
|------|-------------|
| `PROFILE_NOT_FOUND` | Requested profile does not exist |
| `INVALID_CONFIG` | Configuration file is invalid |
| `STACK_DETECTION_FAILED` | Could not detect project stack |
| `INVALID_GUARDRAIL` | Guardrail file is malformed |
| `TOOL_INPUT_ERROR` | Invalid tool input |
| `RESOURCE_NOT_FOUND` | Requested resource does not exist |
