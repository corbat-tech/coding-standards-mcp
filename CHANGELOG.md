# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.0.0] - 2026-07-03

### Added
- TypeScript AST-backed analyzer path with heuristic fallback for unsupported languages.
- Richer `health` output with package version, standards version, runtime Node, guardrail count, and enabled tools.
- Multi-agent architecture guide describing planner, implementer, reviewer, security, and release agent workflows.
- Security model and security policy documentation.

### Changed
- Breaking: minimum Node.js version is now `>=22.0.0`.
- Breaking: package version, MCP server metadata, and runtime version are aligned to `3.0.0`.
- CI matrix now targets Node.js 22, 24, and 26, with Node.js 24 used for single-version jobs.
- `get_context` now provides recommended workflow guidance instead of mandatory response JSON and exact output formats.
- `verify` analyzes implementation, tests, and interfaces as separate inputs before applying verification policy.
- README and benchmark documentation now use more defensible claims and distinguish the primary v3 benchmark from the value analysis.
- `src/tools.ts` is now a backward-compatible re-export of the production tool registry.

### Fixed
- `validate.task_type` schema now matches advertised task types, including security, performance, documentation, and infrastructure.
- `package.json.files` now includes `guardrails`, which are required at runtime.
- Empty `search` queries and unknown tool calls return structured tool input errors.

### Security
- Dependency baseline updated for Node.js 22+, including `p-retry@8`, `dependency-cruiser@18`, `typescript@6`, and `@types/node@26`.
- Snyk remains optional when `SNYK_TOKEN` is absent while `npm audit --audit-level=high` remains blocking.

### Added
- Benchmark v3 value analysis assets (value report, metrics JSON, Python analyzers) quantifying code reduction, security, and maintainability.
- Migration guide for v1.x→v2.x with validation commands and troubleshooting tips (`docs/MIGRATION.md`).
- Audit log capturing quality rounds, coverage goals, and remediation status (`docs/audits/AUDIT_LOG.md`).

### Changed
- V3 scenario implementations rewritten to be leaner while keeping production patterns; benchmark datasets and results refreshed.
- README now highlights 30-second setup, value-focused examples, and updated benchmark summary.
- CI now enforces coverage thresholds (70% lines / 60% branches) and blocks on npm audit/Snyk failures.

### Fixed
- `serverVersion` alignment to 2.0.0 with safer env parsing for cache TTL and log levels.
- Structured error responses across tools via `errors.ts` integration.

### Security
- Profile/category resource lookups now validate IDs to prevent path traversal.
- Logger supports correlation IDs for request-scoped tracing; code analyzer rejects inputs over 1MB to avoid DoS-style processing.

## [2.0.0] - 2026-01-28

### Added - Smart Enforcement System
- **New `verify` tool** - Quality gate that must pass before presenting code to user
  - Validates tests are provided (TDD compliance)
  - Checks for interfaces (DI compliance)
  - Detects critical code issues
  - Returns PASS/FAIL with specific feedback

- **Real code analysis in `validate` tool**
  - Regex-based anti-pattern detection (15+ patterns)
  - Method/class length analysis
  - Quality score calculation (0-100)
  - Actionable suggestions for each issue

- **New `code-analyzer` module** (`src/analysis/code-analyzer.ts`)
  - Detects: empty catch, hardcoded secrets, eval, innerHTML, generic exceptions
  - Detects: console statements, field injection, any type, loose equality
  - Measures: method lines, class lines, interface count, test count

- **Mandatory Checkpoint JSON** in `get_context` output
  - Forces LLM to commit to architecture decisions before coding
  - Includes: interfaces_to_create, tests_to_write, quality_commitments

- **Contractual Response Format** in `get_context` output
  - Enforces order: CHECKPOINT → INTERFACES → TESTS → IMPLEMENTATION → SELF-REVIEW
  - Prevents skipping TDD steps

- **Mandatory Self-Review JSON** in `get_context` output
  - LLM must audit own code: methods_over_20_lines, tests_written, etc.
  - Must achieve quality_score >= 7 before presenting code

### Changed
- `get_context` output now includes Smart Enforcement sections (~400 tokens extra)
- `validate` returns real analysis instead of just checklist
- Added `verify` to tool list and dispatcher

### Technical
- 52 new tests (37 for analyzer, 15 for verify)
- Coverage maintained at 82.62%
- No new external dependencies

### Breaking Changes
- `validate` output format changed from checklist to analysis results

---

## [1.1.0] - 2026-01-15

### Added
- `search_standards` tool for querying documentation by topic (kafka, docker, testing, etc.)
- Enhanced Zod schemas for CQRS, Event-Driven, ArchUnit, HttpClients, Observability
- New profiles for different tech stacks:
  - `minimal.yaml` - Lightweight standards for MVPs and small projects
  - `nodejs.yaml` - Node.js/TypeScript backend standards
  - `python.yaml` - Python/FastAPI backend standards
  - `react.yaml` - React + TypeScript standards
- GitHub Actions CI workflow with multi-node testing
- Test coverage thresholds (80% lines, 70% branches)
- Troubleshooting section in README
- Integration tests for handlers and resources
- Cache TTL for profile hot-reloading

### Changed
- Improved markdown output with detailed sections for architecture, DDD, CQRS
- Enhanced naming conventions output with nested structure support
- Better error messages for profile/resource not found

## [1.0.0] - 2024-01-10

### Added
- Initial release of CORBAT (Coding Standards MCP)
- MCP server implementation with STDIO transport
- 4 core tools:
  - `get_coding_standards` - Get complete standards for a profile
  - `list_profiles` - List available profiles
  - `get_architecture_guidelines` - Get architecture rules
  - `get_naming_conventions` - Get naming conventions
- MCP resources:
  - `corbat://profiles` - List all profiles
  - `corbat://profiles/{id}` - Get specific profile
  - `corbat://standards` - Get all standards
  - `corbat://standards/{category}` - Get standards by category
- MCP prompts:
  - `code_review` - Review code against standards
  - `refactor_suggestion` - Suggest refactoring based on standards
  - `architecture_check` - Validate architecture compliance
- `default.yaml` profile with enterprise Java/Spring Boot standards:
  - Hexagonal Architecture
  - Domain-Driven Design (DDD)
  - CQRS patterns
  - Event-Driven Architecture
  - Code quality rules
  - Testing guidelines (JUnit5, Testcontainers, ArchUnit)
  - Observability (logging, metrics, tracing)
- Standards documentation:
  - `architecture/hexagonal.md`
  - `architecture/ddd.md`
  - `clean-code/principles.md`
  - `clean-code/naming.md`
  - `testing/guidelines.md`
  - `spring-boot/best-practices.md`
  - `event-driven/domain-events.md`
  - `observability/guidelines.md`
  - `containerization/dockerfile.md`
  - `kubernetes/deployment.md`
  - `cicd/github-actions.md`
  - `database/selection-guide.md`
  - `project-setup/initialization-checklist.md`
- Zod schema validation for all profile configurations
- Profile caching for performance
- Environment variable configuration

### Technical
- TypeScript with strict mode
- ESM modules
- Vitest for testing
- @modelcontextprotocol/sdk v1.x

---

## How to Update This Changelog

When making changes:

1. Add entries under `[Unreleased]`
2. Use these categories:
   - `Added` for new features
   - `Changed` for changes in existing functionality
   - `Deprecated` for soon-to-be removed features
   - `Removed` for now removed features
   - `Fixed` for any bug fixes
   - `Security` for vulnerability fixes

3. When releasing, rename `[Unreleased]` to `[X.Y.Z] - YYYY-MM-DD`
