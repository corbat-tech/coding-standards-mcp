/**
 * Authentication Middleware
 * Validates JWT tokens and attaches user info to request
 */

import { Request, Response, NextFunction } from 'express';
import { IJwtService } from '../types/service.interfaces';
import { ITokenPayload } from '../types/user.types';
import { UnauthorizedError } from '../types/errors';

export interface AuthenticatedRequest extends Request {
  user: ITokenPayload;
}

export function createAuthMiddleware(jwtService: IJwtService) {
  return (req: Request, res: Response, next: NextFunction): void => {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
      next(new UnauthorizedError('No authorization header'));
      return;
    }

    const parts = authHeader.split(' ');
    if (parts.length !== 2 || parts[0] !== 'Bearer') {
      next(new UnauthorizedError('Invalid authorization header format'));
      return;
    }

    const token = parts[1];

    try {
      const payload = jwtService.verifyToken(token);
      (req as AuthenticatedRequest).user = payload;
      next();
    } catch (error) {
      next(error);
    }
  };
}
