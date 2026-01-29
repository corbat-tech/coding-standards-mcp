import { HttpException, HttpStatus } from '@nestjs/common';

/**
 * Base exception for article-related errors
 */
export abstract class ArticleException extends HttpException {
  constructor(message: string, status: HttpStatus) {
    super(message, status);
  }
}

/**
 * Exception thrown when an article is not found
 */
export class ArticleNotFoundException extends ArticleException {
  constructor(id: string) {
    super(`Article with id '${id}' not found`, HttpStatus.NOT_FOUND);
  }
}

/**
 * Exception thrown when article validation fails
 */
export class ArticleValidationException extends ArticleException {
  constructor(message: string) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}

/**
 * Exception thrown when article operation is forbidden
 */
export class ArticleForbiddenException extends ArticleException {
  constructor(message: string) {
    super(message, HttpStatus.FORBIDDEN);
  }
}

/**
 * Exception thrown when duplicate article is detected
 */
export class ArticleDuplicateException extends ArticleException {
  constructor(title: string) {
    super(`Article with title '${title}' already exists`, HttpStatus.CONFLICT);
  }
}
