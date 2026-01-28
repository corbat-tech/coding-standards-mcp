/**
 * Guardrails loader module.
 * Loads guardrails from YAML files for better maintainability and customization.
 */
import { readdir, readFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse as parseYaml } from 'yaml';
import type { Guardrails, TaskType } from './types.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/**
 * Extended guardrails with workflow guidance.
 */
export interface ExtendedGuardrails extends Guardrails {
  workflow?: {
    steps: Array<{
      name: string;
      description: string;
      actions: string[];
    }>;
  };
  patterns?: Record<string, unknown>;
  antiPatterns?: Record<string, unknown>;
  commonPatterns?: Record<string, unknown>;
  codeSmells?: Record<string, unknown>;
}

// Cache for loaded guardrails
let guardrailsCache: Record<TaskType, ExtendedGuardrails> | null = null;
let cacheTimestamp = 0;
const CACHE_TTL = 60000; // 1 minute

/**
 * Default guardrails directory path.
 */
const DEFAULT_GUARDRAILS_DIR = join(__dirname, '..', 'guardrails');

/**
 * Loads a single guardrail file.
 */
async function loadGuardrailFile(filePath: string): Promise<ExtendedGuardrails | null> {
  try {
    const content = await readFile(filePath, 'utf-8');
    const parsed = parseYaml(content) as ExtendedGuardrails;
    return parsed;
  } catch {
    return null;
  }
}

/**
 * Loads all guardrails from the guardrails directory.
 * Uses caching to avoid repeated file system reads.
 */
export async function loadGuardrails(
  guardrailsDir: string = DEFAULT_GUARDRAILS_DIR
): Promise<Record<TaskType, ExtendedGuardrails>> {
  const now = Date.now();

  // Return cached if valid
  if (guardrailsCache && now - cacheTimestamp < CACHE_TTL) {
    return guardrailsCache;
  }

  const guardrails: Partial<Record<TaskType, ExtendedGuardrails>> = {};

  try {
    const files = await readdir(guardrailsDir);
    const yamlFiles = files.filter((f) => f.endsWith('.yaml') || f.endsWith('.yml'));

    // Load all guardrail files in parallel
    const loadPromises = yamlFiles.map(async (file) => {
      const filePath = join(guardrailsDir, file);
      const guardrail = await loadGuardrailFile(filePath);
      if (guardrail?.taskType) {
        return { taskType: guardrail.taskType as TaskType, guardrail };
      }
      return null;
    });

    const results = await Promise.all(loadPromises);

    for (const result of results) {
      if (result) {
        guardrails[result.taskType] = result.guardrail;
      }
    }
  } catch {
    // If guardrails directory doesn't exist, return fallback
    return getFallbackGuardrails();
  }

  // Ensure all task types have guardrails (merge with fallback)
  const fallback = getFallbackGuardrails();
  const merged = { ...fallback, ...guardrails } as Record<TaskType, ExtendedGuardrails>;

  guardrailsCache = merged;
  cacheTimestamp = now;

  return merged;
}

/**
 * Gets guardrails for a specific task type.
 */
export async function getGuardrails(taskType: TaskType, guardrailsDir?: string): Promise<ExtendedGuardrails> {
  const allGuardrails = await loadGuardrails(guardrailsDir);
  return allGuardrails[taskType] || getFallbackGuardrails()[taskType];
}

/**
 * Clears the guardrails cache.
 */
export function clearGuardrailsCache(): void {
  guardrailsCache = null;
  cacheTimestamp = 0;
}

/**
 * Fallback guardrails when files are not available.
 */
function getFallbackGuardrails(): Record<TaskType, ExtendedGuardrails> {
  return {
    feature: {
      taskType: 'feature',
      mandatory: [
        'Follow TDD: write tests before implementation',
        'Ensure 80%+ unit test coverage for new code',
        'Apply SOLID principles',
        'Follow project naming conventions',
        'Document public APIs',
        'Validate inputs at boundaries',
      ],
      recommended: [
        'Keep methods under 20 lines',
        'Keep classes under 200 lines',
        'Use dependency injection',
        'Apply single responsibility principle',
        'Write integration tests for critical paths',
      ],
      avoid: [
        'God classes or methods',
        'Hard-coded configuration',
        'Mixing business logic with infrastructure',
        'Circular dependencies',
        'Over-engineering for hypothetical futures',
      ],
    },
    bugfix: {
      taskType: 'bugfix',
      mandatory: [
        'First write a failing test that reproduces the bug',
        'Make the minimum change necessary to fix',
        'Verify fix does not break existing tests',
        'Document root cause in commit message',
      ],
      recommended: [
        'Add regression test if not already covered',
        'Consider if bug exists elsewhere (same pattern)',
        'Update documentation if behavior changed',
      ],
      avoid: [
        'Refactoring unrelated code',
        'Adding features while fixing bugs',
        'Changing APIs without necessity',
        'Fixing symptoms instead of root cause',
      ],
    },
    refactor: {
      taskType: 'refactor',
      mandatory: [
        'All existing tests must pass before AND after',
        'No behavior changes (only structure)',
        'Commit in small, reviewable increments',
        'Extract one concept at a time',
      ],
      recommended: [
        'Increase test coverage if below threshold',
        'Apply design patterns where appropriate',
        'Improve naming and readability',
        'Remove dead code',
      ],
      avoid: [
        'Changing behavior during refactor',
        'Big bang refactoring',
        'Refactoring without tests',
        'Premature abstraction',
      ],
    },
    test: {
      taskType: 'test',
      mandatory: [
        'Follow Arrange-Act-Assert pattern',
        'One logical assertion per test',
        'Test names describe behavior (should_X_when_Y)',
        'Tests must be independent and repeatable',
      ],
      recommended: [
        'Use test fixtures for complex setup',
        'Mock external dependencies',
        'Test edge cases and error conditions',
        'Use parameterized tests for variations',
      ],
      avoid: [
        'Testing implementation details',
        'Flaky tests',
        'Tests that depend on order',
        'Assertions without clear purpose',
      ],
    },
    documentation: {
      taskType: 'documentation',
      mandatory: [
        'Use clear, concise language',
        'Include code examples where applicable',
        'Keep documentation close to code',
        'Document the WHY, not just the WHAT',
      ],
      recommended: [
        'Use consistent formatting',
        'Include diagrams for complex flows',
        'Document assumptions and constraints',
        'Keep README updated',
      ],
      avoid: [
        'Outdated documentation',
        'Duplicating code in comments',
        'Over-documenting obvious code',
        'Documentation without context',
      ],
    },
    performance: {
      taskType: 'performance',
      mandatory: [
        'Measure before optimizing (baseline metrics)',
        'Profile to identify actual bottlenecks',
        'Document performance requirements',
        'Add performance tests/benchmarks',
      ],
      recommended: [
        'Consider caching strategies',
        'Optimize database queries',
        'Use async/non-blocking where appropriate',
        'Consider lazy loading',
      ],
      avoid: [
        'Premature optimization',
        'Optimizing without measurements',
        'Sacrificing readability without significant gain',
        'Micro-optimizations in non-critical paths',
      ],
    },
    security: {
      taskType: 'security',
      mandatory: [
        'Validate ALL user inputs',
        'Use parameterized queries (prevent SQL injection)',
        'Escape output (prevent XSS)',
        'Apply principle of least privilege',
        'Never log sensitive data',
      ],
      recommended: [
        'Use established security libraries',
        'Implement rate limiting',
        'Add security headers',
        'Use HTTPS everywhere',
        'Implement proper authentication/authorization',
      ],
      avoid: [
        'Rolling your own crypto',
        'Hardcoded secrets',
        'Trusting client-side validation alone',
        'Exposing stack traces to users',
        'Using deprecated crypto algorithms',
      ],
    },
    infrastructure: {
      taskType: 'infrastructure',
      mandatory: [
        'Infrastructure as Code (no manual changes)',
        'Version control all configurations',
        'Test in staging before production',
        'Document deployment procedures',
      ],
      recommended: [
        'Use immutable infrastructure',
        'Implement health checks',
        'Set up proper monitoring/alerting',
        'Plan for rollback',
      ],
      avoid: [
        'Manual server configuration',
        'Snowflake servers',
        'Deploying directly to production',
        'Ignoring resource limits',
      ],
    },
  };
}

/**
 * Formats guardrails as markdown with workflow guidance.
 */
export function formatGuardrailsAsMarkdown(guardrails: ExtendedGuardrails): string {
  const lines: string[] = [];

  lines.push(`## Guardrails for ${guardrails.taskType.toUpperCase()} Tasks`, '');

  lines.push('### Mandatory', '');
  for (const item of guardrails.mandatory) {
    lines.push(`- ✅ ${item}`);
  }
  lines.push('');

  lines.push('### Recommended', '');
  for (const item of guardrails.recommended) {
    lines.push(`- 💡 ${item}`);
  }
  lines.push('');

  lines.push('### Avoid', '');
  for (const item of guardrails.avoid) {
    lines.push(`- ❌ ${item}`);
  }
  lines.push('');

  // Add workflow if present
  if (guardrails.workflow?.steps) {
    lines.push('### Workflow', '');
    for (let i = 0; i < guardrails.workflow.steps.length; i++) {
      const step = guardrails.workflow.steps[i];
      lines.push(`**${i + 1}. ${step.name}**: ${step.description}`);
      for (const action of step.actions) {
        lines.push(`   - ${action}`);
      }
      lines.push('');
    }
  }

  return lines.join('\n');
}
