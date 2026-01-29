/**
 * API Error Handling Utilities
 */

export class ApiError extends Error {
  public statusCode: number;
  public code: string;

  constructor(message: string, statusCode: number = 500, code: string = 'INTERNAL_ERROR') {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
    this.code = code;
    Object.setPrototypeOf(this, ApiError.prototype);
  }

  static badRequest(message: string): ApiError {
    return new ApiError(message, 400, 'BAD_REQUEST');
  }

  static notFound(message: string = 'Resource not found'): ApiError {
    return new ApiError(message, 404, 'NOT_FOUND');
  }

  static validationError(message: string): ApiError {
    return new ApiError(message, 422, 'VALIDATION_ERROR');
  }

  static internalError(message: string = 'Internal server error'): ApiError {
    return new ApiError(message, 500, 'INTERNAL_ERROR');
  }

  static methodNotAllowed(method: string): ApiError {
    return new ApiError(`Method ${method} not allowed`, 405, 'METHOD_NOT_ALLOWED');
  }

  toJSON() {
    return {
      success: false,
      error: this.message,
      code: this.code,
    };
  }
}

export function handleApiError(error: unknown): { status: number; body: object } {
  if (error instanceof ApiError) {
    return {
      status: error.statusCode,
      body: error.toJSON(),
    };
  }

  if (error instanceof SyntaxError) {
    return {
      status: 400,
      body: {
        success: false,
        error: 'Invalid JSON in request body',
        code: 'INVALID_JSON',
      },
    };
  }

  console.error('Unexpected error:', error);
  return {
    status: 500,
    body: {
      success: false,
      error: 'An unexpected error occurred',
      code: 'INTERNAL_ERROR',
    },
  };
}
