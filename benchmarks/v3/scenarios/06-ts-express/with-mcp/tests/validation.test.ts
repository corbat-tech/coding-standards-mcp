/**
 * Validation Schema Tests - Written BEFORE implementation (TDD)
 */

import { describe, it, expect } from 'vitest';
import { createUserSchema, updateUserSchema, loginSchema, idParamSchema } from '../types/validation.schemas';

describe('Validation Schemas', () => {
  describe('createUserSchema', () => {
    it('should validate correct user data', () => {
      const data = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
        role: 'admin',
      };

      const result = createUserSchema.safeParse(data);

      expect(result.success).toBe(true);
    });

    it('should fail with invalid email', () => {
      const data = {
        email: 'invalid-email',
        name: 'Test User',
        password: 'password123',
      };

      const result = createUserSchema.safeParse(data);

      expect(result.success).toBe(false);
    });

    it('should fail with short password', () => {
      const data = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'short',
      };

      const result = createUserSchema.safeParse(data);

      expect(result.success).toBe(false);
    });

    it('should fail with short name', () => {
      const data = {
        email: 'test@example.com',
        name: 'A',
        password: 'password123',
      };

      const result = createUserSchema.safeParse(data);

      expect(result.success).toBe(false);
    });

    it('should set default role to user', () => {
      const data = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      };

      const result = createUserSchema.safeParse(data);

      expect(result.success).toBe(true);
      if (result.success) {
        expect(result.data.role).toBe('user');
      }
    });

    it('should fail with invalid role', () => {
      const data = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
        role: 'superadmin',
      };

      const result = createUserSchema.safeParse(data);

      expect(result.success).toBe(false);
    });
  });

  describe('updateUserSchema', () => {
    it('should validate partial update', () => {
      const data = { name: 'New Name' };

      const result = updateUserSchema.safeParse(data);

      expect(result.success).toBe(true);
    });

    it('should allow empty object', () => {
      const result = updateUserSchema.safeParse({});

      expect(result.success).toBe(true);
    });

    it('should fail with invalid email', () => {
      const data = { email: 'not-an-email' };

      const result = updateUserSchema.safeParse(data);

      expect(result.success).toBe(false);
    });
  });

  describe('loginSchema', () => {
    it('should validate login credentials', () => {
      const data = {
        email: 'test@example.com',
        password: 'password123',
      };

      const result = loginSchema.safeParse(data);

      expect(result.success).toBe(true);
    });

    it('should fail with missing password', () => {
      const data = { email: 'test@example.com' };

      const result = loginSchema.safeParse(data);

      expect(result.success).toBe(false);
    });
  });

  describe('idParamSchema', () => {
    it('should validate UUID', () => {
      const data = { id: '550e8400-e29b-41d4-a716-446655440000' };

      const result = idParamSchema.safeParse(data);

      expect(result.success).toBe(true);
    });

    it('should fail with invalid UUID', () => {
      const data = { id: 'not-a-uuid' };

      const result = idParamSchema.safeParse(data);

      expect(result.success).toBe(false);
    });
  });
});
