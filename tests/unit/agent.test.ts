import { describe, expect, it } from 'vitest';
import { classifyTaskType } from '../../src/agent.js';

describe('Agent - classifyTaskType', () => {
  describe('bugfix classification', () => {
    it.each([
      ['fix login bug', 'bugfix'],
      ['Fix the broken API', 'bugfix'],
      ['fix: resolve null pointer error', 'bugfix'],
      ['Bug in payment processing', 'bugfix'],
      ['There is an error in the user service', 'bugfix'],
      ['Fix issue with database connection', 'bugfix'],
      ['The problem is in the authentication module', 'bugfix'],
      ['Broken link in navigation', 'bugfix'],
    ])('classifies "%s" as bugfix', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('refactor classification', () => {
    it.each([
      ['refactor user service', 'refactor'],
      ['Refactor the payment module', 'refactor'],
      ['cleanup old code', 'refactor'],
      ['Clean up unused imports', 'refactor'],
      ['reorganize folder structure', 'refactor'],
      ['Restructure the API layer', 'refactor'],
      ['improve structure of components', 'refactor'],
    ])('classifies "%s" as refactor', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('test classification', () => {
    it.each([
      ['write unit tests for OrderService', 'test'],
      ['Add test coverage for auth module', 'test'],
      ['Create spec for payment processor', 'test'],
      ['Improve coverage of user service', 'test'],
      ['unit test the validation logic', 'test'],
      ['integration test for API endpoints', 'test'],
    ])('classifies "%s" as test', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('documentation classification', () => {
    it.each([
      ['document the API endpoints', 'documentation'],
      ['Update the README file', 'documentation'],
      ['Add comments to complex functions', 'documentation'],
      ['Write JSDoc for public methods', 'documentation'],
      ['Add javadoc to all services', 'documentation'],
    ])('classifies "%s" as documentation', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('performance classification', () => {
    it.each([
      ['improve performance of queries', 'performance'],
      ['Optimize database queries', 'performance'],
      ['The application is too slow', 'performance'],
      ['Speed up the API response time', 'performance'],
      ['Memory leak in the service', 'performance'],
      ['Add cache for expensive operations', 'performance'],
    ])('classifies "%s" as performance', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('security classification', () => {
    it.each([
      ['secure the authentication flow', 'security'],
      ['Security vulnerability in login', 'security'], // Note: "Fix security..." would match bugfix first
      ['Add auth middleware', 'security'],
      ['Implement permission checks', 'security'],
      ['Encrypt sensitive data', 'security'],
    ])('classifies "%s" as security', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('infrastructure classification', () => {
    it.each([
      ['deploy to kubernetes', 'infrastructure'],
      ['Set up Docker containers', 'infrastructure'],
      ['Configure CI/CD pipeline', 'infrastructure'],
      ['Update infrastructure config', 'infrastructure'],
      ['Create deployment pipeline', 'infrastructure'],
    ])('classifies "%s" as infrastructure', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('feature classification (default)', () => {
    it.each([
      ['add new payment feature', 'feature'],
      ['Create payment service', 'feature'],
      ['Implement user registration', 'feature'],
      ['Add shopping cart functionality', 'feature'],
      ['Build order management module', 'feature'],
      ['create REST API for products', 'feature'],
      ['implement email notifications', 'feature'],
    ])('classifies "%s" as feature', (input, expected) => {
      expect(classifyTaskType(input)).toBe(expected);
    });
  });

  describe('edge cases', () => {
    it('handles empty string as feature', () => {
      expect(classifyTaskType('')).toBe('feature');
    });

    it('handles mixed case input', () => {
      expect(classifyTaskType('FIX THE BUG')).toBe('bugfix');
      expect(classifyTaskType('REFACTOR Code')).toBe('refactor');
    });

    it('prioritizes first matching pattern', () => {
      // "fix" comes before "test" in the checks
      expect(classifyTaskType('fix the test')).toBe('bugfix');
    });
  });
});
