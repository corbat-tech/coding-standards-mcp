/**
 * Post Validation Utilities
 */

import { CreatePostInput, UpdatePostInput, PostValidationErrors } from '@/types/post';

export const VALIDATION_RULES = {
  title: {
    minLength: 3,
    maxLength: 200,
  },
  content: {
    minLength: 10,
    maxLength: 50000,
  },
  excerpt: {
    maxLength: 500,
  },
  author: {
    minLength: 2,
    maxLength: 100,
  },
} as const;

export function validateCreatePost(input: CreatePostInput): PostValidationErrors {
  const errors: PostValidationErrors = {};

  // Validate title
  if (!input.title || input.title.trim().length === 0) {
    errors.title = 'Title is required';
  } else if (input.title.trim().length < VALIDATION_RULES.title.minLength) {
    errors.title = `Title must be at least ${VALIDATION_RULES.title.minLength} characters`;
  } else if (input.title.trim().length > VALIDATION_RULES.title.maxLength) {
    errors.title = `Title must not exceed ${VALIDATION_RULES.title.maxLength} characters`;
  }

  // Validate content
  if (!input.content || input.content.trim().length === 0) {
    errors.content = 'Content is required';
  } else if (input.content.trim().length < VALIDATION_RULES.content.minLength) {
    errors.content = `Content must be at least ${VALIDATION_RULES.content.minLength} characters`;
  } else if (input.content.trim().length > VALIDATION_RULES.content.maxLength) {
    errors.content = `Content must not exceed ${VALIDATION_RULES.content.maxLength} characters`;
  }

  // Validate author
  if (!input.author || input.author.trim().length === 0) {
    errors.author = 'Author is required';
  } else if (input.author.trim().length < VALIDATION_RULES.author.minLength) {
    errors.author = `Author name must be at least ${VALIDATION_RULES.author.minLength} characters`;
  } else if (input.author.trim().length > VALIDATION_RULES.author.maxLength) {
    errors.author = `Author name must not exceed ${VALIDATION_RULES.author.maxLength} characters`;
  }

  // Validate excerpt (optional but has max length)
  if (input.excerpt && input.excerpt.trim().length > VALIDATION_RULES.excerpt.maxLength) {
    errors.excerpt = `Excerpt must not exceed ${VALIDATION_RULES.excerpt.maxLength} characters`;
  }

  return errors;
}

export function validateUpdatePost(input: UpdatePostInput): PostValidationErrors {
  const errors: PostValidationErrors = {};

  // Validate title if provided
  if (input.title !== undefined) {
    if (input.title.trim().length === 0) {
      errors.title = 'Title cannot be empty';
    } else if (input.title.trim().length < VALIDATION_RULES.title.minLength) {
      errors.title = `Title must be at least ${VALIDATION_RULES.title.minLength} characters`;
    } else if (input.title.trim().length > VALIDATION_RULES.title.maxLength) {
      errors.title = `Title must not exceed ${VALIDATION_RULES.title.maxLength} characters`;
    }
  }

  // Validate content if provided
  if (input.content !== undefined) {
    if (input.content.trim().length === 0) {
      errors.content = 'Content cannot be empty';
    } else if (input.content.trim().length < VALIDATION_RULES.content.minLength) {
      errors.content = `Content must be at least ${VALIDATION_RULES.content.minLength} characters`;
    } else if (input.content.trim().length > VALIDATION_RULES.content.maxLength) {
      errors.content = `Content must not exceed ${VALIDATION_RULES.content.maxLength} characters`;
    }
  }

  // Validate author if provided
  if (input.author !== undefined) {
    if (input.author.trim().length === 0) {
      errors.author = 'Author name cannot be empty';
    } else if (input.author.trim().length < VALIDATION_RULES.author.minLength) {
      errors.author = `Author name must be at least ${VALIDATION_RULES.author.minLength} characters`;
    } else if (input.author.trim().length > VALIDATION_RULES.author.maxLength) {
      errors.author = `Author name must not exceed ${VALIDATION_RULES.author.maxLength} characters`;
    }
  }

  // Validate excerpt if provided
  if (input.excerpt !== undefined && input.excerpt.trim().length > VALIDATION_RULES.excerpt.maxLength) {
    errors.excerpt = `Excerpt must not exceed ${VALIDATION_RULES.excerpt.maxLength} characters`;
  }

  return errors;
}

export function hasValidationErrors(errors: PostValidationErrors): boolean {
  return Object.keys(errors).length > 0;
}

export function generateSlug(title: string): string {
  return title
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function generateExcerpt(content: string, maxLength: number = 150): string {
  const plainText = content.replace(/<[^>]*>/g, '').trim();
  if (plainText.length <= maxLength) {
    return plainText;
  }
  return plainText.substring(0, maxLength).trim() + '...';
}
