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

  // VALIDATE - Real code analysis
  {
    name: 'validate',
    description: `Analyze code against coding standards with REAL code analysis.

WHEN TO USE:
- After writing code, to check for issues
- During iterative development
- Before calling verify for final approval

PERFORMS REAL ANALYSIS:
- Detects anti-patterns (empty catch, hardcoded secrets, etc.)
- Measures method/class lengths
- Checks for interfaces and tests
- Calculates quality score

RETURNS:
- Score (0-100)
- CRITICAL issues (must fix)
- WARNINGS (should fix)
- Metrics (lines, methods, tests, etc.)
- PASSED/NEEDS WORK verdict

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
          enum: ['feature', 'bugfix', 'refactor', 'test', 'security', 'performance'],
          description: 'Type of task for context-aware validation (optional)',
        },
      },
      required: ['code'],
    },
  },

  // VERIFY - Gate before presenting code to user (NEW in v2.0)
  {
    name: 'verify',
    description: `REQUIRED: Verify generated code before presenting to user.

WHEN TO USE:
- ALWAYS call this AFTER generating code
- BEFORE presenting code to the user
- This is the final quality gate

WHAT IT CHECKS:
- Tests are provided (TDD compliance)
- Interfaces exist (DI compliance)
- No critical code issues
- Quality score >= 50

RETURNS:
- PASS: Code meets standards, present to user
- FAIL: Issues to fix, iterate and verify again

WORKFLOW:
1. Generate code following get_context guidelines
2. Call verify({ code, tests, interfaces })
3. If FAIL: fix issues and call verify again
4. If PASS: present code to user

EXAMPLE: verify({ code: "class UserServiceImpl...", tests: "describe('UserService')...", interfaces: "interface UserService..." })`,
    inputSchema: {
      type: 'object' as const,
      properties: {
        code: {
          type: 'string',
          description: 'All implementation code',
        },
        tests: {
          type: 'string',
          description: 'All test code (REQUIRED for TDD compliance)',
        },
        interfaces: {
          type: 'string',
          description: 'All interfaces and type definitions',
        },
        task_type: {
          type: 'string',
          enum: ['feature', 'bugfix', 'refactor', 'test', 'security', 'performance'],
          description: 'Type of task for context-aware verification',
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
export type ToolName = 'get_context' | 'validate' | 'verify' | 'search' | 'profiles' | 'health' | 'init';
