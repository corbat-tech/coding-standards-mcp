import { Request, Response, NextFunction } from 'express';
import { AuthService } from '../../application/authService.js';
import { UnauthorizedError } from '../../domain/errors.js';

export interface AuthenticatedRequest extends Request {
  userId?: string;
  userRole?: string;
}

export function createAuthMiddleware(authService: AuthService) {
  return (req: AuthenticatedRequest, _res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;

    if (!authHeader?.startsWith('Bearer ')) {
      return next(new UnauthorizedError('Missing authorization header'));
    }

    const token = authHeader.substring(7);

    try {
      const decoded = authService.verifyToken(token);
      req.userId = decoded.userId;
      req.userRole = decoded.role;
      next();
    } catch (error) {
      next(error);
    }
  };
}
