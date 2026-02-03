/**
 * Comprehensive coverage tests for src/agent.ts
 * Targets: loadProjectConfig, detectProjectStack, getTechnicalDecision, getProjectRules, getGuardrails
 */
import { access, mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import {
  detectProjectStack,
  getGuardrails,
  getProjectRules,
  getTechnicalDecision,
  loadProjectConfig,
} from '../../src/agent.js';
import type { ProjectConfig, TaskType } from '../../src/types.js';

// Test fixtures directory
const TEST_FIXTURES_DIR = join(__dirname, '../__fixtures__/agent-coverage');

describe('Agent Coverage Tests', () => {
  // Setup test fixtures directory
  beforeAll(async () => {
    try {
      await access(TEST_FIXTURES_DIR);
    } catch {
      await mkdir(TEST_FIXTURES_DIR, { recursive: true });
    }
  });

  afterAll(async () => {
    try {
      await rm(TEST_FIXTURES_DIR, { recursive: true, force: true });
    } catch {
      // Ignore cleanup errors
    }
  });

  // ============================================================================
  // loadProjectConfig() Tests
  // ============================================================================
  describe('loadProjectConfig()', () => {
    const projectDir = join(TEST_FIXTURES_DIR, 'load-config');

    beforeAll(async () => {
      await mkdir(projectDir, { recursive: true });
    });

    afterAll(async () => {
      try {
        await rm(projectDir, { recursive: true, force: true });
      } catch {
        // Ignore
      }
    });

    it('should return valid ProjectConfig when .corbat.json exists with valid content', async () => {
      const configContent: ProjectConfig = {
        profile: 'java-spring-backend',
        autoInject: true,
        rules: {
          always: ['Use interfaces for all services'],
          onNewFile: ['Follow naming conventions'],
          onTest: ['Use AAA pattern'],
          onRefactor: ['Maintain test coverage'],
        },
        decisions: {
          database: 'PostgreSQL',
        },
      };

      await writeFile(join(projectDir, '.corbat.json'), JSON.stringify(configContent, null, 2));

      const result = await loadProjectConfig(projectDir);

      expect(result).not.toBeNull();
      expect(result?.profile).toBe('java-spring-backend');
      expect(result?.autoInject).toBe(true);
      expect(result?.rules?.always).toContain('Use interfaces for all services');
    });

    it('should return null when .corbat.json does not exist', async () => {
      const nonExistentDir = join(TEST_FIXTURES_DIR, 'non-existent-dir');
      const result = await loadProjectConfig(nonExistentDir);
      expect(result).toBeNull();
    });

    it('should return null when .corbat.json contains malformed JSON', async () => {
      const malformedDir = join(TEST_FIXTURES_DIR, 'malformed-json');
      await mkdir(malformedDir, { recursive: true });
      await writeFile(join(malformedDir, '.corbat.json'), '{ invalid json }');

      const result = await loadProjectConfig(malformedDir);
      expect(result).toBeNull();

      await rm(malformedDir, { recursive: true, force: true });
    });

    it('should return null when .corbat.json has invalid schema', async () => {
      const invalidSchemaDir = join(TEST_FIXTURES_DIR, 'invalid-schema');
      await mkdir(invalidSchemaDir, { recursive: true });
      // Missing required fields but extra invalid fields
      await writeFile(
        join(invalidSchemaDir, '.corbat.json'),
        JSON.stringify({
          // autoInject should be boolean, not string
          autoInject: 'not-a-boolean',
        })
      );

      const result = await loadProjectConfig(invalidSchemaDir);
      // Zod will fail validation
      expect(result).toBeNull();

      await rm(invalidSchemaDir, { recursive: true, force: true });
    });
  });

  // ============================================================================
  // detectProjectStack() Tests
  // ============================================================================
  describe('detectProjectStack()', () => {
    const stackDir = join(TEST_FIXTURES_DIR, 'detect-stack');

    beforeEach(async () => {
      // Clean and recreate test directory
      try {
        await rm(stackDir, { recursive: true, force: true });
      } catch {
        // Ignore
      }
      await mkdir(stackDir, { recursive: true });
    });

    afterAll(async () => {
      try {
        await rm(stackDir, { recursive: true, force: true });
      } catch {
        // Ignore
      }
    });

    it('should detect Node.js/TypeScript stack with Express', async () => {
      // Express backend project - matches Node.js pattern and detects Express framework
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            express: '^4.18.0',
            typescript: '^5.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.language).toBe('TypeScript');
      expect(result?.framework).toBe('Express');
      // Express doesn't override the profile, uses the matched pattern's profile
      expect(result?.detectedFiles).toContain('package.json');
    });

    it('should detect Java/Maven stack', async () => {
      await writeFile(join(stackDir, 'pom.xml'), '<project><dependencies></dependencies></project>');

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.language).toBe('Java');
      expect(result?.suggestedProfile).toBe('java-spring-backend');
    });

    it('should detect Spring Boot from pom.xml content', async () => {
      await writeFile(
        join(stackDir, 'pom.xml'),
        `<project>
          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter</artifactId>
            </dependency>
          </dependencies>
        </project>`
      );

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.framework).toBe('Spring Boot');
    });

    it('should detect Python stack', async () => {
      await writeFile(join(stackDir, 'pyproject.toml'), '[project]\nname = "test"');

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.language).toBe('Python');
      expect(result?.suggestedProfile).toBe('python');
    });

    it('should detect Go stack', async () => {
      await writeFile(join(stackDir, 'go.mod'), 'module example.com/test');

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.language).toBe('Go');
      expect(result?.suggestedProfile).toBe('go');
    });

    it('should detect Rust stack', async () => {
      await writeFile(join(stackDir, 'Cargo.toml'), '[package]\nname = "test"');

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.language).toBe('Rust');
      expect(result?.suggestedProfile).toBe('rust');
    });

    it('should detect React from package.json dependencies', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            react: '^18.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result).not.toBeNull();
      expect(result?.framework).toBe('React');
      expect(result?.suggestedProfile).toBe('react');
    });

    it('should detect Vue from package.json dependencies', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            vue: '^3.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.framework).toBe('Vue');
      expect(result?.suggestedProfile).toBe('vue');
    });

    it('should detect Angular from package.json dependencies', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            '@angular/core': '^17.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.framework).toBe('Angular');
      expect(result?.suggestedProfile).toBe('angular');
    });

    it('should detect Express framework', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            express: '^4.18.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.framework).toBe('Express');
    });

    it('should detect Fastify framework', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            fastify: '^4.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.framework).toBe('Fastify');
    });

    it('should detect NestJS framework', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          dependencies: {
            '@nestjs/core': '^10.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.framework).toBe('NestJS');
    });

    it('should detect Vitest as test framework', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          devDependencies: {
            vitest: '^1.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.testFramework).toBe('Vitest');
    });

    it('should detect Jest as test framework', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          devDependencies: {
            jest: '^29.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.testFramework).toBe('Jest');
    });

    it('should detect Mocha as test framework', async () => {
      await writeFile(
        join(stackDir, 'package.json'),
        JSON.stringify({
          name: 'test',
          devDependencies: {
            mocha: '^10.0.0',
          },
        })
      );
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      const result = await detectProjectStack(stackDir);

      expect(result?.testFramework).toBe('Mocha');
    });

    it('should return null when no project files are detected', async () => {
      // Empty directory
      const result = await detectProjectStack(stackDir);
      expect(result).toBeNull();
    });

    it('should handle unreadable package.json gracefully', async () => {
      await writeFile(join(stackDir, 'package.json'), '{ invalid json }');
      await writeFile(join(stackDir, 'tsconfig.json'), JSON.stringify({ compilerOptions: {} }));

      // Should still detect TypeScript/Node.js even if package.json can't be parsed
      const result = await detectProjectStack(stackDir);
      expect(result).not.toBeNull();
      expect(result?.language).toBe('TypeScript');
    });
  });

  // ============================================================================
  // getTechnicalDecision() Tests
  // ============================================================================
  describe('getTechnicalDecision()', () => {
    it('should return decision for valid category "database"', () => {
      const result = getTechnicalDecision('database', 'Need a database for user data', null);

      expect(result).not.toBeNull();
      expect(result?.options.length).toBeGreaterThan(0);
      expect(result?.recommendation).toBe('PostgreSQL');
      expect(result?.reasoning).toContain('PostgreSQL');
    });

    it('should return decision for valid category "cache"', () => {
      const result = getTechnicalDecision('cache', 'Need caching solution', null);

      expect(result).not.toBeNull();
      expect(result?.recommendation).toBe('Redis');
    });

    it('should return decision for valid category "messaging"', () => {
      const result = getTechnicalDecision('messaging', 'Need message queue', null);

      expect(result).not.toBeNull();
      expect(result?.recommendation).toBe('Apache Kafka');
    });

    it('should return decision for valid category "authentication"', () => {
      const result = getTechnicalDecision('authentication', 'Need auth', null);

      expect(result).not.toBeNull();
      expect(result?.recommendation).toBe('JWT (JSON Web Tokens)');
    });

    it('should return decision for valid category "testing"', () => {
      const result = getTechnicalDecision('testing', 'Need testing strategy', null);

      expect(result).not.toBeNull();
      expect(result?.recommendation).toBe('Unit + Integration + E2E');
    });

    it('should return null for invalid category', () => {
      const result = getTechnicalDecision('invalid-category', 'context', null);
      expect(result).toBeNull();
    });

    it('should use project predefined decision when available', () => {
      const projectConfig: ProjectConfig = {
        decisions: {
          database: 'MySQL',
        },
      };

      const result = getTechnicalDecision('database', 'context', projectConfig);

      expect(result).not.toBeNull();
      expect(result?.recommendation).toBe('MySQL');
      expect(result?.reasoning).toContain('Project configuration specifies');
    });

    it('should fall back to default when predefined decision does not match any option', () => {
      const projectConfig: ProjectConfig = {
        decisions: {
          database: 'UnknownDB',
        },
      };

      const result = getTechnicalDecision('database', 'context', projectConfig);

      expect(result).not.toBeNull();
      expect(result?.recommendation).toBe('PostgreSQL'); // default
    });

    it('should handle predefined decision with different casing', () => {
      const projectConfig: ProjectConfig = {
        decisions: {
          database: 'mysql', // lowercase
        },
      };

      const result = getTechnicalDecision('database', 'context', projectConfig);

      expect(result?.recommendation).toBe('MySQL');
    });

    it('should return all options regardless of recommendation', () => {
      const result = getTechnicalDecision('database', 'context', null);

      expect(result?.options).toHaveLength(3);
      expect(result?.options.map((o) => o.name)).toContain('PostgreSQL');
      expect(result?.options.map((o) => o.name)).toContain('MySQL');
      expect(result?.options.map((o) => o.name)).toContain('MongoDB');
    });
  });

  // ============================================================================
  // getProjectRules() Tests
  // ============================================================================
  describe('getProjectRules()', () => {
    it('should return empty array when projectConfig is null', () => {
      const result = getProjectRules('feature', null);
      expect(result).toEqual([]);
    });

    it('should return empty array when projectConfig.rules is undefined', () => {
      const projectConfig: ProjectConfig = {
        profile: 'test',
      };
      const result = getProjectRules('feature', projectConfig);
      expect(result).toEqual([]);
    });

    it('should return always rules for any task type', () => {
      const projectConfig: ProjectConfig = {
        rules: {
          always: ['Always rule 1', 'Always rule 2'],
          onNewFile: [],
          onTest: [],
          onRefactor: [],
        },
      };

      const result = getProjectRules('feature', projectConfig);
      expect(result).toContain('Always rule 1');
      expect(result).toContain('Always rule 2');
    });

    it('should include onNewFile rules for feature task type', () => {
      const projectConfig: ProjectConfig = {
        rules: {
          always: ['Always rule'],
          onNewFile: ['New file rule'],
          onTest: [],
          onRefactor: [],
        },
      };

      const result = getProjectRules('feature', projectConfig);
      expect(result).toContain('Always rule');
      expect(result).toContain('New file rule');
    });

    it('should include onTest rules for test task type', () => {
      const projectConfig: ProjectConfig = {
        rules: {
          always: ['Always rule'],
          onNewFile: [],
          onTest: ['Test rule'],
          onRefactor: [],
        },
      };

      const result = getProjectRules('test', projectConfig);
      expect(result).toContain('Always rule');
      expect(result).toContain('Test rule');
    });

    it('should include onRefactor rules for refactor task type', () => {
      const projectConfig: ProjectConfig = {
        rules: {
          always: ['Always rule'],
          onNewFile: [],
          onTest: [],
          onRefactor: ['Refactor rule'],
        },
      };

      const result = getProjectRules('refactor', projectConfig);
      expect(result).toContain('Always rule');
      expect(result).toContain('Refactor rule');
    });

    it('should not include task-specific rules for unrelated task types', () => {
      const projectConfig: ProjectConfig = {
        rules: {
          always: ['Always rule'],
          onNewFile: ['New file rule'],
          onTest: ['Test rule'],
          onRefactor: ['Refactor rule'],
        },
      };

      const bugfixResult = getProjectRules('bugfix', projectConfig);
      expect(bugfixResult).toContain('Always rule');
      expect(bugfixResult).not.toContain('New file rule');
      expect(bugfixResult).not.toContain('Test rule');
      expect(bugfixResult).not.toContain('Refactor rule');
    });

    it('should handle undefined rules arrays gracefully', () => {
      const projectConfig: ProjectConfig = {
        rules: {
          always: undefined as unknown as string[],
          onNewFile: undefined as unknown as string[],
          onTest: undefined as unknown as string[],
          onRefactor: undefined as unknown as string[],
        },
      };

      // Should not throw, but return empty or partial results
      expect(() => getProjectRules('feature', projectConfig)).not.toThrow();
    });
  });

  // ============================================================================
  // getGuardrails() Tests
  // ============================================================================
  describe('getGuardrails()', () => {
    it('should return guardrails for feature task type', async () => {
      const result = await getGuardrails('feature');

      expect(result).toBeDefined();
      expect(result.taskType).toBe('feature');
      expect(result.mandatory).toBeDefined();
      expect(result.mandatory.length).toBeGreaterThan(0);
      expect(result.recommended).toBeDefined();
      expect(result.avoid).toBeDefined();
    });

    it('should return guardrails for all task types', async () => {
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
        expect(result.mandatory).toBeDefined();
      }
    });

    it('should merge project-specific guardrails when provided', async () => {
      const projectConfig: ProjectConfig = {
        guardrails: {
          feature: {
            taskType: 'feature',
            mandatory: ['Project-specific mandatory rule'],
            recommended: ['Project-specific recommended rule'],
            avoid: ['Project-specific avoid rule'],
          },
        },
      };

      const result = await getGuardrails('feature', projectConfig);

      expect(result.mandatory).toContain('Project-specific mandatory rule');
      expect(result.recommended).toContain('Project-specific recommended rule');
      expect(result.avoid).toContain('Project-specific avoid rule');
    });

    it('should include base guardrails when project-specific are added', async () => {
      const projectConfig: ProjectConfig = {
        guardrails: {
          feature: {
            taskType: 'feature',
            mandatory: ['Extra rule'],
            recommended: [],
            avoid: [],
          },
        },
      };

      const result = await getGuardrails('feature', projectConfig);

      // Should have both base and project-specific
      expect(result.mandatory).toContain('Extra rule');
      expect(result.mandatory.length).toBeGreaterThan(1);
    });

    it('should not modify cached guardrails when merging project config', async () => {
      // First call without project config
      const baseResult = await getGuardrails('feature');
      const baseLength = baseResult.mandatory.length;

      // Second call with project config
      const projectConfig: ProjectConfig = {
        guardrails: {
          feature: {
            taskType: 'feature',
            mandatory: ['Added rule 1', 'Added rule 2'],
            recommended: [],
            avoid: [],
          },
        },
      };
      await getGuardrails('feature', projectConfig);

      // Third call without project config - should still have original length
      const finalResult = await getGuardrails('feature');
      expect(finalResult.mandatory.length).toBe(baseLength);
    });

    it('should return base guardrails when project guardrails for task type is undefined', async () => {
      const projectConfig: ProjectConfig = {
        guardrails: {
          bugfix: {
            taskType: 'bugfix',
            mandatory: ['Bugfix rule'],
            recommended: [],
            avoid: [],
          },
        },
      };

      // Request feature guardrails but project only defines bugfix
      const result = await getGuardrails('feature', projectConfig);

      expect(result.taskType).toBe('feature');
      // Should not contain bugfix rules
      expect(result.mandatory).not.toContain('Bugfix rule');
    });
  });
});
