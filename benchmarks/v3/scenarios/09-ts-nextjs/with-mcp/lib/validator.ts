/**
 * Post Validator Implementation
 * Validates post creation and update requests
 */

import type {
  CreatePostRequest,
  UpdatePostRequest,
  PostValidator,
  ValidationResult,
  ValidationError,
} from '../types';

/** Validation constants */
const VALIDATION_RULES = {
  TITLE_MIN_LENGTH: 3,
  TITLE_MAX_LENGTH: 200,
  CONTENT_MIN_LENGTH: 10,
  CONTENT_MAX_LENGTH: 50000,
  AUTHOR_MIN_LENGTH: 2,
  AUTHOR_MAX_LENGTH: 100,
} as const;

/** Post validator implementation */
export class PostValidatorImpl implements PostValidator {
  validateCreate(request: CreatePostRequest): ValidationResult {
    const errors: ValidationError[] = [];

    this.validateTitle(request.title, errors);
    this.validateContent(request.content, errors);
    this.validateAuthor(request.author, errors);

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  validateUpdate(request: UpdatePostRequest): ValidationResult {
    const errors: ValidationError[] = [];

    if (request.title !== undefined) {
      this.validateTitle(request.title, errors);
    }

    if (request.content !== undefined) {
      this.validateContent(request.content, errors);
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }

  private validateTitle(title: string, errors: ValidationError[]): void {
    if (!title || title.trim().length === 0) {
      errors.push({ field: 'title', message: 'Title is required' });
      return;
    }

    const trimmedTitle = title.trim();

    if (trimmedTitle.length < VALIDATION_RULES.TITLE_MIN_LENGTH) {
      errors.push({
        field: 'title',
        message: `Title must be at least ${VALIDATION_RULES.TITLE_MIN_LENGTH} characters`,
      });
    }

    if (trimmedTitle.length > VALIDATION_RULES.TITLE_MAX_LENGTH) {
      errors.push({
        field: 'title',
        message: `Title must not exceed ${VALIDATION_RULES.TITLE_MAX_LENGTH} characters`,
      });
    }
  }

  private validateContent(content: string, errors: ValidationError[]): void {
    if (!content || content.trim().length === 0) {
      errors.push({ field: 'content', message: 'Content is required' });
      return;
    }

    const trimmedContent = content.trim();

    if (trimmedContent.length < VALIDATION_RULES.CONTENT_MIN_LENGTH) {
      errors.push({
        field: 'content',
        message: `Content must be at least ${VALIDATION_RULES.CONTENT_MIN_LENGTH} characters`,
      });
    }

    if (trimmedContent.length > VALIDATION_RULES.CONTENT_MAX_LENGTH) {
      errors.push({
        field: 'content',
        message: `Content must not exceed ${VALIDATION_RULES.CONTENT_MAX_LENGTH} characters`,
      });
    }
  }

  private validateAuthor(author: string, errors: ValidationError[]): void {
    if (!author || author.trim().length === 0) {
      errors.push({ field: 'author', message: 'Author is required' });
      return;
    }

    const trimmedAuthor = author.trim();

    if (trimmedAuthor.length < VALIDATION_RULES.AUTHOR_MIN_LENGTH) {
      errors.push({
        field: 'author',
        message: `Author must be at least ${VALIDATION_RULES.AUTHOR_MIN_LENGTH} characters`,
      });
    }

    if (trimmedAuthor.length > VALIDATION_RULES.AUTHOR_MAX_LENGTH) {
      errors.push({
        field: 'author',
        message: `Author must not exceed ${VALIDATION_RULES.AUTHOR_MAX_LENGTH} characters`,
      });
    }
  }
}

/** Factory function for creating validator instance */
export function createPostValidator(): PostValidator {
  return new PostValidatorImpl();
}
