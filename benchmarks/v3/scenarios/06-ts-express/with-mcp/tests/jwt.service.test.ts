/**
 * JWT Service Tests - Written BEFORE implementation (TDD)
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { JwtServiceImpl } from '../services/jwt.service';
import { UnauthorizedError } from '../types/errors';
import type { ITokenPayload } from '../types';

describe('JwtService', () => {
  let jwtService: JwtServiceImpl;
  const testSecret = 'test-secret-key-that-is-at-least-32-characters';

  beforeEach(() => {
    jwtService = new JwtServiceImpl(testSecret);
  });

  describe('generateToken', () => {
    it('should generate a valid JWT token', () => {
      const payload: ITokenPayload = {
        userId: '123',
        email: 'test@example.com',
        role: 'user',
      };

      const token = jwtService.generateToken(payload);

      expect(token).toBeDefined();
      expect(typeof token).toBe('string');
      expect(token.split('.')).toHaveLength(3);
    });
  });

  describe('verifyToken', () => {
    it('should verify and decode a valid token', () => {
      const payload: ITokenPayload = {
        userId: '123',
        email: 'test@example.com',
        role: 'admin',
      };

      const token = jwtService.generateToken(payload);
      const decoded = jwtService.verifyToken(token);

      expect(decoded.userId).toBe(payload.userId);
      expect(decoded.email).toBe(payload.email);
      expect(decoded.role).toBe(payload.role);
    });

    it('should reject invalid token', () => {
      expect(() => jwtService.verifyToken('invalid-token')).toThrow(UnauthorizedError);
    });

    it('should reject token with invalid signature', () => {
      const otherService = new JwtServiceImpl('different-secret-key-at-least-32-chars');
      const payload: ITokenPayload = {
        userId: '123',
        email: 'test@example.com',
        role: 'user',
      };

      const token = otherService.generateToken(payload);

      expect(() => jwtService.verifyToken(token)).toThrow(UnauthorizedError);
    });
  });

  describe('decodeToken', () => {
    it('should decode token without verification', () => {
      const payload: ITokenPayload = {
        userId: '123',
        email: 'test@example.com',
        role: 'moderator',
      };

      const token = jwtService.generateToken(payload);
      const decoded = jwtService.decodeToken(token);

      expect(decoded).not.toBeNull();
      expect(decoded?.userId).toBe(payload.userId);
    });

    it('should return null for invalid token format', () => {
      const result = jwtService.decodeToken('not-a-valid-token');

      expect(result).toBeNull();
    });
  });
});
