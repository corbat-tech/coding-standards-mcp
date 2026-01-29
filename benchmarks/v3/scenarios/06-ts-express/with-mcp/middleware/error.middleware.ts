/**
 * Error Handler Middleware
 * Centralized error handling for the application
 */

import { Request, Response, NextFunction } from 'express';
import { AppError } from '../types/errors';

export function errorHandler(
  error: Error,
  req: Request,
  res: Response,
  _next: NextFunction
): void {
  if (error instanceof AppError) {
    res.status(error.statusCode).json(error.toJSON());
    return;
  }

  // Log unexpected errors (in production, use proper logging)
  console.error('Unexpected error:', error);

  res.status(500).json({
    error: {
      code: 'INTERNAL_ERROR',
      message: 'Internal server error',
    },
  });
}
