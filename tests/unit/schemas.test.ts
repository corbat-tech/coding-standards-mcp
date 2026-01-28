import { describe, expect, it } from 'vitest';
import { ZodError } from 'zod';
import { GetContextSchema, InitSchema, SearchSchema, ValidateSchema } from '../../src/tools/schemas.js';

describe('Tool Schemas Validation', () => {
  describe('GetContextSchema', () => {
    it('should accept valid input with task only', () => {
      const input = { task: 'Create payment service' };
      const result = GetContextSchema.parse(input);
      expect(result.task).toBe('Create payment service');
      expect(result.project_dir).toBeUndefined();
    });

    it('should accept valid input with task and project_dir', () => {
      const input = { task: 'Create payment service', project_dir: '/path/to/project' };
      const result = GetContextSchema.parse(input);
      expect(result.task).toBe('Create payment service');
      expect(result.project_dir).toBe('/path/to/project');
    });

    it('should reject empty task', () => {
      const input = { task: '' };
      expect(() => GetContextSchema.parse(input)).toThrow(ZodError);
    });

    it('should reject missing task', () => {
      const input = {};
      expect(() => GetContextSchema.parse(input)).toThrow(ZodError);
    });
  });

  describe('ValidateSchema', () => {
    it('should accept valid input with code only', () => {
      const input = { code: 'const x = 1;' };
      const result = ValidateSchema.parse(input);
      expect(result.code).toBe('const x = 1;');
      expect(result.task_type).toBeUndefined();
    });

    it('should accept valid task_type values', () => {
      const validTypes = ['feature', 'bugfix', 'refactor', 'test'] as const;
      for (const taskType of validTypes) {
        const input = { code: 'const x = 1;', task_type: taskType };
        const result = ValidateSchema.parse(input);
        expect(result.task_type).toBe(taskType);
      }
    });

    it('should reject invalid task_type', () => {
      const input = { code: 'const x = 1;', task_type: 'invalid' };
      expect(() => ValidateSchema.parse(input)).toThrow(ZodError);
    });

    it('should reject empty code', () => {
      const input = { code: '' };
      expect(() => ValidateSchema.parse(input)).toThrow(ZodError);
    });
  });

  describe('SearchSchema', () => {
    it('should accept valid query', () => {
      const input = { query: 'kafka' };
      const result = SearchSchema.parse(input);
      expect(result.query).toBe('kafka');
    });

    it('should reject empty query', () => {
      const input = { query: '' };
      expect(() => SearchSchema.parse(input)).toThrow(ZodError);
    });

    it('should reject missing query', () => {
      const input = {};
      expect(() => SearchSchema.parse(input)).toThrow(ZodError);
    });
  });

  describe('InitSchema', () => {
    it('should accept valid project_dir', () => {
      const input = { project_dir: '/path/to/project' };
      const result = InitSchema.parse(input);
      expect(result.project_dir).toBe('/path/to/project');
    });

    it('should reject empty project_dir', () => {
      const input = { project_dir: '' };
      expect(() => InitSchema.parse(input)).toThrow(ZodError);
    });

    it('should reject missing project_dir', () => {
      const input = {};
      expect(() => InitSchema.parse(input)).toThrow(ZodError);
    });
  });
});
