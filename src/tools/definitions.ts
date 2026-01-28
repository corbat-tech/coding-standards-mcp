/**
 * MCP Tool Definitions.
 *
 * This file contains ONLY the tool definitions (name, description, inputSchema).
 * Handler logic is in separate files under handlers/.
 *
 * Design principles:
 * - One primary tool (get_context) that does everything
 * - Supporting tools for specific use cases
 * - Names are short and intuitive
 * - Descriptions are optimized for LLM understanding
 */

export const tools = [
  // PRIMARY TOOL - Everything in one call
  {
    name: 'get_context',
    description: `Returns coding standards, guardrails, and workflow for implementing a task.

WHEN TO USE:
- ALWAYS call this FIRST before writing any code
- When starting a new feature, bugfix, or refactor
- When unsure about project conventions

RETURNS:
- Detected stack (Java/Python/TypeScript/Go/Rust/etc)
- Task type classification (feature/bugfix/refactor/test/security/performance)
- MUST rules (mandatory guidelines)
- AVOID rules (anti-patterns to prevent)
- Code quality thresholds (max lines, coverage %)
- Naming conventions (classes, methods, files)
- Recommended TDD workflow

EXAMPLE: get_context({ task: "Create payment service", project_dir: "/path/to/project" })`,
    inputSchema: {
      type: 'object' as const,
      properties: {
        task: {
          type: 'string',
          description: 'What you\'re implementing (e.g., "Create payment service", "Fix login bug")',
        },
        project_dir: {
          type: 'string',
          description: 'Project directory for auto-detection of stack and .corbat.json config (optional)',
        },
      },
      required: ['task'],
    },
  },

  // VALIDATE - Check code against standards
  {
    name: 'validate',
    description: `Validate code against coding standards. Returns validation criteria and checklist.

WHEN TO USE:
- After writing code, before committing
- During code review
- To check if code follows project standards

RETURNS:
- Code quality thresholds (max method lines, coverage)
- Guardrails for the task type
- Naming convention checks
- Review checklist (CRITICAL/WARNINGS/Score)

EXAMPLE: validate({ code: "public class UserService { ... }", task_type: "feature" })`,
    inputSchema: {
      type: 'object' as const,
      properties: {
        code: {
          type: 'string',
          description: 'The code to validate',
        },
        task_type: {
          type: 'string',
          enum: ['feature', 'bugfix', 'refactor', 'test'],
          description: 'Type of task for context-aware validation (optional)',
        },
      },
      required: ['code'],
    },
  },

  // SEARCH - Find specific topics in documentation
  {
    name: 'search',
    description: `Search standards documentation for specific topics.

WHEN TO USE:
- Looking for specific technology guidance (kafka, docker, kubernetes)
- Need detailed information on a pattern or practice
- Exploring available standards

EXAMPLE QUERIES: "kafka", "testing", "docker", "logging", "metrics", "archunit", "flyway"

RETURNS: Up to 5 matching results with excerpts from documentation.`,
    inputSchema: {
      type: 'object' as const,
      properties: {
        query: {
          type: 'string',
          description: 'Search query (e.g., "kafka", "testing", "docker")',
        },
      },
      required: ['query'],
    },
  },

  // PROFILES - List available profiles
  {
    name: 'profiles',
    description: `List all available coding standards profiles.

RETURNS: List of profiles with ID and description. Profiles include:
- java-spring-backend: Enterprise Java with Hexagonal Architecture
- nodejs: Node.js/TypeScript with Clean Architecture
- react, vue, angular: Frontend frameworks
- python: FastAPI/Django
- go, rust: Systems programming
- And more...

Use profile ID in .corbat.json or get_context will auto-detect.`,
    inputSchema: {
      type: 'object' as const,
      properties: {},
    },
  },

  // HEALTH - Server status
  {
    name: 'health',
    description: `Check server status, loaded profiles, and usage metrics.

RETURNS:
- Server status (OK/ERROR)
- Version
- Load time
- Profiles loaded
- Standards documents count
- Usage metrics (tool calls, most used profile)`,
    inputSchema: {
      type: 'object' as const,
      properties: {},
    },
  },

  // INIT - Generate .corbat.json
  {
    name: 'init',
    description: `Generate a .corbat.json configuration file for a project.

WHEN TO USE:
- Setting up Corbat for a new project
- Want to customize coding standards for a project
- Need to see available profiles and options

Analyzes the project directory and suggests optimal configuration based on detected stack.

RETURNS:
- Detected stack information
- Suggested .corbat.json content
- Available profiles list
- Setup instructions`,
    inputSchema: {
      type: 'object' as const,
      properties: {
        project_dir: {
          type: 'string',
          description: 'Project directory to analyze',
        },
      },
      required: ['project_dir'],
    },
  },
];

// Tool names for type safety
export type ToolName = 'get_context' | 'validate' | 'search' | 'profiles' | 'health' | 'init';
