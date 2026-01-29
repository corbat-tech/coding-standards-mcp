import { Request, Response, NextFunction } from 'express';
import { AppError, ValidationError } from '../utils/errors';
import { ZodError } from 'zod';

interface ErrorResponse {
  status: 'error';
  message: string;
  errors?: Array<{ field: string; message: string }>;
  stack?: string;
}

export function errorHandler(
  err: Error,
  _req: Request,
  res: Response,
  _next: NextFunction
): void {
  const response: ErrorResponse = {
    status: 'error',
    message: err.message || 'Internal server error',
  };

  // Handle Zod validation errors
  if (err instanceof ZodError) {
    response.message = 'Validation failed';
    response.errors = err.errors.map((e) => ({
      field: e.path.join('.'),
      message: e.message,
    }));
    res.status(400).json(response);
    return;
  }

  // Handle custom validation errors
  if (err instanceof ValidationError) {
    response.errors = err.errors;
    res.status(err.statusCode).json(response);
    return;
  }

  // Handle known operational errors
  if (err instanceof AppError) {
    res.status(err.statusCode).json(response);
    return;
  }

  // Handle unknown errors
  console.error('Unhandled error:', err);

  if (process.env.NODE_ENV === 'development') {
    response.stack = err.stack;
  }

  response.message = 'Internal server error';
  res.status(500).json(response);
}

export function notFoundHandler(_req: Request, res: Response): void {
  res.status(404).json({
    status: 'error',
    message: 'Route not found',
  });
}
