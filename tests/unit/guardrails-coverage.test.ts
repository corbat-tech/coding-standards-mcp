/**
 * Comprehensive coverage tests for src/guardrails.ts
 * Targets: clearGuardrailsCache, formatGuardrailsAsMarkdown, getFallbackGuardrails,
 *          loadGuardrailFile error cases, loadGuardrails branches
 */
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import {
  clearGuardrailsCache,
  type ExtendedGuardrails,
  formatGuardrailsAsMarkdown,
  getGuardrails,
  loadGuardrails,
} from '../../src/guardrails.js';
import type { TaskType } from '../../src/types.js';

// Test fixtures directory
const TEST_FIXTURES_DIR = join(__dirname, '../__fixtures__/guardrails-coverage');

describe('Guardrails Coverage Tests', () => {
  beforeAll(async () => {
    await mkdir(TEST_FIXTURES_DIR, { recursive: true });
  });

  afterAll(async () => {
    try {
      await rm(TEST_FIXTURES_DIR, { recursive: true, force: true });
    } catch {
      // Ignore cleanup errors
    }
  });

  // ============================================================================
  // clearGuardrailsCache() Tests
  // ============================================================================
  describe('clearGuardrailsCache()', () => {
    beforeEach(() => {
      // Clear cache before each test
      clearGuardrailsCache();
    });

    it('should clear the cache and allow fresh load', async () => {
      // First load to populate cache
      const firstLoad = await loadGuardrails();
      expect(firstLoad).toBeDefined();

      // Clear cache
      clearGuardrailsCache();

      // Second load should work without errors
      const secondLoad = await loadGuardrails();
      expect(secondLoad).toBeDefined();
    });

    it('should allow cache hit after loading', async () => {
      // Load twice without clearing - second should be from cache
      const firstLoad = await loadGuardrails();
      const secondLoad = await loadGuardrails();

      // Both should return the same structure
      expect(firstLoad.feature).toBeDefined();
      expect(secondLoad.feature).toBeDefined();
      expect(firstLoad.feature.taskType).toBe(secondLoad.feature.taskType);
    });

    it('should not throw when called multiple times', () => {
      expect(() => {
        clearGuardrailsCache();
        clearGuardrailsCache();
        clearGuardrailsCache();
      }).not.toThrow();
    });
  });

  // ============================================================================
  // formatGuardrailsAsMarkdown() Tests
  // ============================================================================
  describe('formatGuardrailsAsMarkdown()', () => {
    it('should format basic guardrails without workflow', () => {
      const guardrails: ExtendedGuardrails = {
        taskType: 'feature',
        mandatory: ['Rule 1', 'Rule 2'],
        recommended: ['Recommendation 1'],
        avoid: ['Avoid 1', 'Avoid 2'],
      };

      const result = formatGuardrailsAsMarkdown(guardrails);

      expect(result).toContain('## Guardrails for FEATURE Tasks');
      expect(result).toContain('### Mandatory');
      expect(result).toContain('- ✅ Rule 1');
      expect(result).toContain('- ✅ Rule 2');
      expect(result).toContain('### Recommended');
      expect(result).toContain('- 💡 Recommendation 1');
      expect(result).toContain('### Avoid');
      expect(result).toContain('- ❌ Avoid 1');
      expect(result).toContain('- ❌ Avoid 2');
    });

    it('should format guardrails with workflow steps', () => {
      const guardrails: ExtendedGuardrails = {
        taskType: 'bugfix',
        mandatory: ['Fix the bug'],
        recommended: ['Add test'],
        avoid: ['Breaking changes'],
        workflow: {
          steps: [
            {
              name: 'Reproduce',
              description: 'First reproduce the bug',
              actions: ['Create failing test', 'Verify bug exists'],
            },
            {
              name: 'Fix',
              description: 'Implement the fix',
              actions: ['Make minimal change', 'Run tests'],
            },
          ],
        },
      };

      const result = formatGuardrailsAsMarkdown(guardrails);

      expect(result).toContain('### Workflow');
      expect(result).toContain('**1. Reproduce**: First reproduce the bug');
      expect(result).toContain('   - Create failing test');
      expect(result).toContain('   - Verify bug exists');
      expect(result).toContain('**2. Fix**: Implement the fix');
      expect(result).toContain('   - Make minimal change');
    });

    it('should handle empty arrays gracefully', () => {
      const guardrails: ExtendedGuardrails = {
        taskType: 'test',
        mandatory: [],
        recommended: [],
        avoid: [],
      };

      const result = formatGuardrailsAsMarkdown(guardrails);

      expect(result).toContain('## Guardrails for TEST Tasks');
      expect(result).toContain('### Mandatory');
      expect(result).toContain('### Recommended');
      expect(result).toContain('### Avoid');
    });

    it('should format all 8 task types correctly', () => {
      const taskTypes: TaskType[] = [
        'feature',
        'bugfix',
        'refactor',
        'test',
        'documentation',
        'performance',
        'security',
        'infrastructure',
      ];

      for (const taskType of taskTypes) {
        const guardrails: ExtendedGuardrails = {
          taskType,
          mandatory: ['Rule'],
          recommended: ['Rec'],
          avoid: ['Avoid'],
        };

        const result = formatGuardrailsAsMarkdown(guardrails);
        expect(result).toContain(`## Guardrails for ${taskType.toUpperCase()} Tasks`);
      }
    });

    it('should not include workflow section when workflow is undefined', () => {
      const guardrails: ExtendedGuardrails = {
        taskType: 'feature',
        mandatory: ['Rule'],
        recommended: ['Rec'],
        avoid: ['Avoid'],
        // No workflow property
      };

      const result = formatGuardrailsAsMarkdown(guardrails);

      expect(result).not.toContain('### Workflow');
    });

    it('should not include workflow section when workflow.steps is undefined', () => {
      const guardrails: ExtendedGuardrails = {
        taskType: 'feature',
        mandatory: ['Rule'],
        recommended: ['Rec'],
        avoid: ['Avoid'],
        workflow: {
          steps: undefined as unknown as Array<{
            name: string;
            description: string;
            actions: string[];
          }>,
        },
      };

      const result = formatGuardrailsAsMarkdown(guardrails);

      expect(result).not.toContain('### Workflow');
    });
  });

  // ============================================================================
  // getFallbackGuardrails() Tests (via getGuardrails when files unavailable)
  // ============================================================================
  describe('getFallbackGuardrails()', () => {
    beforeEach(() => {
      clearGuardrailsCache();
    });

    it('should return complete fallback guardrails for refactor', async () => {
      // Use a non-existent directory to trigger fallback
      const result = await getGuardrails('refactor', '/non-existent-dir');

      expect(result).toBeDefined();
      expect(result.taskType).toBe('refactor');
      expect(result.mandatory).toContain('All existing tests must pass before AND after');
      expect(result.mandatory).toContain('No behavior changes (only structure)');
      expect(result.avoid).toContain('Changing behavior during refactor');
    });

    it('should return complete fallback guardrails for test', async () => {
      const result = await getGuardrails('test', '/non-existent-dir');

      expect(result.taskType).toBe('test');
      expect(result.mandatory).toContain('Follow Arrange-Act-Assert pattern');
      expect(result.mandatory).toContain('One logical assertion per test');
    });

    it('should return complete fallback guardrails for documentation', async () => {
      const result = await getGuardrails('documentation', '/non-existent-dir');

      expect(result.taskType).toBe('documentation');
      expect(result.mandatory).toContain('Use clear, concise language');
    });

    it('should return complete fallback guardrails for performance', async () => {
      const result = await getGuardrails('performance', '/non-existent-dir');

      expect(result.taskType).toBe('performance');
      expect(result.mandatory).toContain('Measure before optimizing (baseline metrics)');
      expect(result.avoid).toContain('Premature optimization');
    });

    it('should return complete fallback guardrails for security', async () => {
      const result = await getGuardrails('security', '/non-existent-dir');

      expect(result.taskType).toBe('security');
      expect(result.mandatory).toContain('Validate ALL user inputs');
      expect(result.mandatory).toContain('Never log sensitive data');
      expect(result.avoid).toContain('Rolling your own crypto');
    });

    it('should return complete fallback guardrails for infrastructure', async () => {
      const result = await getGuardrails('infrastructure', '/non-existent-dir');

      expect(result.taskType).toBe('infrastructure');
      expect(result.mandatory).toContain('Infrastructure as Code (no manual changes)');
      expect(result.avoid).toContain('Manual server configuration');
    });

    it('should return complete fallback guardrails for bugfix', async () => {
      const result = await getGuardrails('bugfix', '/non-existent-dir');

      expect(result.taskType).toBe('bugfix');
      expect(result.mandatory).toContain('First write a failing test that reproduces the bug');
      expect(result.avoid).toContain('Refactoring unrelated code');
    });

    it('should return complete fallback guardrails for feature', async () => {
      const result = await getGuardrails('feature', '/non-existent-dir');

      expect(result.taskType).toBe('feature');
      expect(result.mandatory).toContain('Follow TDD: write tests before implementation');
      expect(result.mandatory).toContain('Ensure 80%+ unit test coverage for new code');
    });
  });

  // ============================================================================
  // loadGuardrailFile() Error Cases (tested via loadGuardrails)
  // ============================================================================
  describe('loadGuardrailFile() error handling', () => {
    const errorTestDir = join(TEST_FIXTURES_DIR, 'error-cases');

    beforeEach(async () => {
      clearGuardrailsCache();
      await mkdir(errorTestDir, { recursive: true });
    });

    afterEach(async () => {
      try {
        await rm(errorTestDir, { recursive: true, force: true });
      } catch {
        // Ignore
      }
    });

    it('should handle file not found gracefully via fallback', async () => {
      // Directory exists but is empty
      const result = await loadGuardrails(errorTestDir);

      // Should return fallback guardrails
      expect(result).toBeDefined();
      expect(result.feature).toBeDefined();
    });

    it('should handle YAML parse error gracefully', async () => {
      // Create a file with invalid YAML
      await writeFile(
        join(errorTestDir, 'feature.yaml'),
        `
taskType: feature
mandatory:
  - Valid item
  invalid yaml here: [not closed
        `
      );

      const result = await loadGuardrails(errorTestDir);

      // Should still work, returning fallback
      expect(result).toBeDefined();
      expect(result.feature).toBeDefined();
    });
  });

  // ============================================================================
  // loadGuardrails() Branch Coverage
  // ============================================================================
  describe('loadGuardrails() branches', () => {
    const branchTestDir = join(TEST_FIXTURES_DIR, 'branch-cases');

    beforeEach(async () => {
      clearGuardrailsCache();
      try {
        await rm(branchTestDir, { recursive: true, force: true });
      } catch {
        // Ignore
      }
    });

    afterEach(async () => {
      try {
        await rm(branchTestDir, { recursive: true, force: true });
      } catch {
        // Ignore
      }
    });

    it('should return fallback when readdir fails (directory does not exist)', async () => {
      const result = await loadGuardrails('/definitely/not/a/real/path/12345');

      expect(result).toBeDefined();
      expect(result.feature).toBeDefined();
      expect(result.bugfix).toBeDefined();
    });

    it('should skip files without valid taskType in YAML', async () => {
      await mkdir(branchTestDir, { recursive: true });

      // Create a YAML file without taskType
      await writeFile(
        join(branchTestDir, 'invalid.yaml'),
        `
mandatory:
  - Rule 1
recommended:
  - Rec 1
avoid:
  - Avoid 1
        `
      );

      // Create a valid YAML file
      await writeFile(
        join(branchTestDir, 'feature.yaml'),
        `
taskType: feature
mandatory:
  - Valid rule
recommended: []
avoid: []
        `
      );

      const result = await loadGuardrails(branchTestDir);

      // Should have feature from file, others from fallback
      expect(result.feature).toBeDefined();
      expect(result.feature.mandatory).toContain('Valid rule');
    });

    it('should filter out non-YAML files', async () => {
      await mkdir(branchTestDir, { recursive: true });

      // Create various file types
      await writeFile(join(branchTestDir, 'readme.md'), '# README');
      await writeFile(join(branchTestDir, 'config.json'), '{}');
      await writeFile(join(branchTestDir, 'script.js'), 'console.log("test")');
      await writeFile(
        join(branchTestDir, 'feature.yaml'),
        `
taskType: feature
mandatory:
  - From YAML file
recommended: []
avoid: []
        `
      );

      const result = await loadGuardrails(branchTestDir);

      // Should only process the YAML file
      expect(result.feature.mandatory).toContain('From YAML file');
    });

    it('should merge loaded guardrails with fallback for missing task types', async () => {
      await mkdir(branchTestDir, { recursive: true });

      // Only create feature guardrails
      await writeFile(
        join(branchTestDir, 'feature.yaml'),
        `
taskType: feature
mandatory:
  - Custom feature rule
recommended:
  - Custom recommendation
avoid:
  - Custom avoid
        `
      );

      const result = await loadGuardrails(branchTestDir);

      // Feature should have custom rules
      expect(result.feature.mandatory).toContain('Custom feature rule');

      // Other task types should have fallback
      expect(result.bugfix).toBeDefined();
      expect(result.bugfix.mandatory).toContain('First write a failing test that reproduces the bug');
    });

    it('should handle yml extension in addition to yaml', async () => {
      await mkdir(branchTestDir, { recursive: true });

      await writeFile(
        join(branchTestDir, 'refactor.yml'),
        `
taskType: refactor
mandatory:
  - YML file rule
recommended: []
avoid: []
        `
      );

      const result = await loadGuardrails(branchTestDir);

      expect(result.refactor.mandatory).toContain('YML file rule');
    });

    it('should use cache on subsequent calls within TTL', async () => {
      await mkdir(branchTestDir, { recursive: true });

      await writeFile(
        join(branchTestDir, 'test.yaml'),
        `
taskType: test
mandatory:
  - Initial rule
recommended: []
avoid: []
        `
      );

      // First load
      const firstResult = await loadGuardrails(branchTestDir);
      expect(firstResult.test.mandatory).toContain('Initial rule');

      // Modify the file
      await writeFile(
        join(branchTestDir, 'test.yaml'),
        `
taskType: test
mandatory:
  - Modified rule
recommended: []
avoid: []
        `
      );

      // Second load should return cached version (within TTL)
      const secondResult = await loadGuardrails(branchTestDir);
      expect(secondResult.test.mandatory).toContain('Initial rule');
    });
  });

  // ============================================================================
  // getGuardrails() Additional Tests
  // ============================================================================
  describe('getGuardrails() additional coverage', () => {
    beforeEach(() => {
      clearGuardrailsCache();
    });

    it('should return correct guardrails for each task type from default directory', async () => {
      const taskTypes: TaskType[] = [
        'feature',
        'bugfix',
        'refactor',
        'test',
        'documentation',
        'performance',
        'security',
        'infrastructure',
      ];

      for (const taskType of taskTypes) {
        const result = await getGuardrails(taskType);
        expect(result.taskType).toBe(taskType);
        expect(Array.isArray(result.mandatory)).toBe(true);
        expect(Array.isArray(result.recommended)).toBe(true);
        expect(Array.isArray(result.avoid)).toBe(true);
      }
    });

    it('should handle undefined guardrailsDir parameter', async () => {
      const result = await getGuardrails('feature', undefined);

      expect(result).toBeDefined();
      expect(result.taskType).toBe('feature');
    });
  });
});
