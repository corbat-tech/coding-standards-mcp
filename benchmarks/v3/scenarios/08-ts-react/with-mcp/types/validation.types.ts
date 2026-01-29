/**
 * Validation Types
 * Defines validation-related interfaces and types
 */

import { ContactFormData, ContactFormErrors, ValidationResult } from './form.types';

/**
 * Validator function type
 */
export type FieldValidator<T> = (value: T) => string | undefined;

/**
 * Validation rules for a field
 */
export interface ValidationRule<T> {
  validate: FieldValidator<T>;
  message: string;
}

/**
 * Form validator interface for dependency injection
 */
export interface FormValidator {
  validate(data: ContactFormData): ValidationResult;
  validateField(field: keyof ContactFormData, value: string): string | undefined;
}

/**
 * Default validation error messages
 */
export const VALIDATION_MESSAGES = {
  NAME_REQUIRED: 'Name is required',
  NAME_MIN_LENGTH: 'Name must be at least 2 characters',
  EMAIL_REQUIRED: 'Email is required',
  EMAIL_INVALID: 'Please enter a valid email address',
  MESSAGE_REQUIRED: 'Message is required',
  MESSAGE_MIN_LENGTH: 'Message must be at least 10 characters',
  PRIORITY_REQUIRED: 'Priority is required',
  PRIORITY_INVALID: 'Please select a valid priority level',
} as const;

/**
 * Email validation regex pattern
 */
export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
