/**
 * Custom Error Classes for Blog Post Feature
 * Provides meaningful error handling with proper error codes
 */

/** Base error class for post-related errors */
export class PostError extends Error {
  constructor(
    message: string,
    public readonly code: string,
  ) {
    super(message);
    this.name = 'PostError';
  }
}

/** Error thrown when a post is not found */
export class PostNotFoundError extends PostError {
  constructor(id: string) {
    super(`Post with id '${id}' not found`, 'POST_NOT_FOUND');
    this.name = 'PostNotFoundError';
  }
}

/** Error thrown when validation fails */
export class ValidationFailedError extends PostError {
  constructor(
    message: string,
    public readonly errors: Array<{ field: string; message: string }>,
  ) {
    super(message, 'VALIDATION_FAILED');
    this.name = 'ValidationFailedError';
  }
}

/** Error thrown when a duplicate post is detected */
export class DuplicatePostError extends PostError {
  constructor(title: string) {
    super(`Post with title '${title}' already exists`, 'DUPLICATE_POST');
    this.name = 'DuplicatePostError';
  }
}

/** Error thrown for database operations */
export class DatabaseError extends PostError {
  constructor(operation: string) {
    super(`Database error during ${operation}`, 'DATABASE_ERROR');
    this.name = 'DatabaseError';
  }
}
