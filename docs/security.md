# Security Model

## Execution Model

Corbat runs locally as an MCP server over stdio. It loads profiles, standards, guardrails, and optional project configuration from the local filesystem. It does not send source code, prompts, secrets, or telemetry to an external service.

## Data Processed

Corbat may process:

- Task descriptions passed to `get_context`.
- Code snippets passed to `validate` or `verify`.
- Local project metadata used for stack detection.
- Local `.corbat.json` configuration.
- Bundled profiles, standards, and guardrails.

Corbat does not need production credentials. Do not pass secrets to validation tools unless you are deliberately testing secret detection.

## Filesystem Access

Corbat reads bundled package files and, when `project_dir` is provided, project metadata needed for stack detection and configuration. Resource and profile lookups validate identifiers to prevent path traversal. Code validation operates on code strings provided by the caller.

## Network Behavior

The MCP server itself does not require outbound network access at runtime. Package installation through `npm` or `npx` uses npm registry network access before the server starts.

## Validator Limits

The built-in analyzer is a fast local quality gate, not a full SAST product. It combines TypeScript AST checks with heuristic fallback for other languages. It can miss vulnerabilities and can report false positives. Use it alongside:

- Repository tests and type checks.
- Dependency scanning such as `npm audit` or Snyk where configured.
- Code review.
- Threat modeling for sensitive systems.
- SAST/DAST tools required by your organization.

## Supply Chain

Recommended controls:

- Pin package versions in production MCP configs where reproducibility matters.
- Keep `package-lock.json` committed.
- Run `npm audit --audit-level=high` in CI.
- Treat optional Snyk scans as additive unless a token is configured.
- Verify package contents with `npm pack --dry-run` before publishing.

## Reporting Vulnerabilities

Report suspected vulnerabilities through the repository security advisories or by contacting the maintainers at `info@corbat.tech`. Include affected version, reproduction steps, impact, and any suggested mitigation.
