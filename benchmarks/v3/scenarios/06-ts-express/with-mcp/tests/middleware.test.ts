/**
 * Middleware Tests - Written BEFORE implementation (TDD)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Request, Response, NextFunction } from 'express';
import { createAuthMiddleware } from '../middleware/auth.middleware';
import { errorHandler } from '../middleware/error.middleware';
import { JwtServiceImpl } from '../services/jwt.service';
import { NotFoundError, ValidationError, UnauthorizedError } from '../types/errors';
import type { ITokenPayload } from '../types';

const mockRequest = (overrides: Partial<Request> = {}): Request => {
  return {
    headers: {},
    body: {},
    params: {},
    query: {},
    ...overrides,
  } as Request;
};

const mockResponse = (): Response => {
  const res = {} as Response;
  res.status = vi.fn().mockReturnValue(res);
  res.json = vi.fn().mockReturnValue(res);
  return res;
};

const mockNext: NextFunction = vi.fn();

describe('Auth Middleware', () => {
  let jwtService: JwtServiceImpl;
  let authMiddleware: ReturnType<typeof createAuthMiddleware>;
  const testSecret = 'test-secret-key-that-is-at-least-32-characters';

  beforeEach(() => {
    vi.clearAllMocks();
    jwtService = new JwtServiceImpl(testSecret);
    authMiddleware = createAuthMiddleware(jwtService);
  });

  it('should pass with valid token', () => {
    const payload: ITokenPayload = {
      userId: '123',
      email: 'test@example.com',
      role: 'user',
    };
    const token = jwtService.generateToken(payload);
    const req = mockRequest({
      headers: { authorization: `Bearer ${token}` },
    });
    const res = mockResponse();

    authMiddleware(req, res, mockNext);

    expect(mockNext).toHaveBeenCalledWith();
    expect((req as Request & { user: ITokenPayload }).user.userId).toBe('123');
  });

  it('should fail without authorization header', () => {
    const req = mockRequest();
    const res = mockResponse();

    authMiddleware(req, res, mockNext);

    expect(mockNext).toHaveBeenCalledWith(expect.any(UnauthorizedError));
  });

  it('should fail with malformed authorization header', () => {
    const req = mockRequest({
      headers: { authorization: 'InvalidFormat token123' },
    });
    const res = mockResponse();

    authMiddleware(req, res, mockNext);

    expect(mockNext).toHaveBeenCalledWith(expect.any(UnauthorizedError));
  });

  it('should fail with invalid token', () => {
    const req = mockRequest({
      headers: { authorization: 'Bearer invalid-token' },
    });
    const res = mockResponse();

    authMiddleware(req, res, mockNext);

    expect(mockNext).toHaveBeenCalledWith(expect.any(UnauthorizedError));
  });
});

describe('Error Handler Middleware', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should handle NotFoundError', () => {
    const error = new NotFoundError('User');
    const req = mockRequest();
    const res = mockResponse();

    errorHandler(error, req, res, mockNext);

    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({
      error: {
        code: 'NOT_FOUND',
        message: 'User not found',
      },
    });
  });

  it('should handle ValidationError with details', () => {
    const error = new ValidationError('Validation failed', {
      email: ['Invalid email format'],
    });
    const req = mockRequest();
    const res = mockResponse();

    errorHandler(error, req, res, mockNext);

    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.json).toHaveBeenCalledWith({
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Validation failed',
        details: { email: ['Invalid email format'] },
      },
    });
  });

  it('should handle UnauthorizedError', () => {
    const error = new UnauthorizedError('Invalid credentials');
    const req = mockRequest();
    const res = mockResponse();

    errorHandler(error, req, res, mockNext);

    expect(res.status).toHaveBeenCalledWith(401);
    expect(res.json).toHaveBeenCalledWith({
      error: {
        code: 'UNAUTHORIZED',
        message: 'Invalid credentials',
      },
    });
  });

  it('should handle unknown errors as 500', () => {
    const error = new Error('Something went wrong');
    const req = mockRequest();
    const res = mockResponse();

    errorHandler(error, req, res, mockNext);

    expect(res.status).toHaveBeenCalledWith(500);
    expect(res.json).toHaveBeenCalledWith({
      error: {
        code: 'INTERNAL_ERROR',
        message: 'Internal server error',
      },
    });
  });
});
