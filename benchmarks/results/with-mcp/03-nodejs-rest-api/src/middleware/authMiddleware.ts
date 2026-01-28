import { NextFunction, Request, Response } from 'express';
import { UnauthorizedError } from '../domain/errors';
import { AuthService } from '../services/authService';
import { JwtPayload } from '../types/user';

declare global {
  namespace Express {
    interface Request {
      user?: JwtPayload;
    }
  }
}

export function createAuthMiddleware(authService: AuthService) {
  return (req: Request, _res: Response, next: NextFunction): void => {
    const authHeader = req.headers.authorization;

    if (!authHeader) {
      throw new UnauthorizedError('Authorization header required');
    }

    const [type, token] = authHeader.split(' ');

    if (type !== 'Bearer' || !token) {
      throw new UnauthorizedError('Invalid authorization format');
    }

    const payload = authService.verifyToken(token);
    req.user = payload;
    next();
  };
}
