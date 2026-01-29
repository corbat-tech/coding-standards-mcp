/**
 * Form Validation Logic
 * Implements FormValidator interface for contact form validation
 */

import {
  ContactFormData,
  ContactFormErrors,
  ValidationResult,
  FormValidator,
  VALIDATION_MESSAGES,
  EMAIL_REGEX,
} from '../types';

/**
 * Validates a name field
 */
const validateName = (name: string): string | undefined => {
  const trimmedName = name.trim();
  if (!trimmedName) {
    return VALIDATION_MESSAGES.NAME_REQUIRED;
  }
  if (trimmedName.length < 2) {
    return VALIDATION_MESSAGES.NAME_MIN_LENGTH;
  }
  return undefined;
};

/**
 * Validates an email field
 */
const validateEmail = (email: string): string | undefined => {
  const trimmedEmail = email.trim();
  if (!trimmedEmail) {
    return VALIDATION_MESSAGES.EMAIL_REQUIRED;
  }
  if (!EMAIL_REGEX.test(trimmedEmail)) {
    return VALIDATION_MESSAGES.EMAIL_INVALID;
  }
  return undefined;
};

/**
 * Validates a message field
 */
const validateMessage = (message: string): string | undefined => {
  const trimmedMessage = message.trim();
  if (!trimmedMessage) {
    return VALIDATION_MESSAGES.MESSAGE_REQUIRED;
  }
  if (trimmedMessage.length < 10) {
    return VALIDATION_MESSAGES.MESSAGE_MIN_LENGTH;
  }
  return undefined;
};

/**
 * Validates a priority field
 */
const validatePriority = (priority: string): string | undefined => {
  if (!priority) {
    return VALIDATION_MESSAGES.PRIORITY_REQUIRED;
  }
  const validPriorities = ['low', 'medium', 'high'];
  if (!validPriorities.includes(priority)) {
    return VALIDATION_MESSAGES.PRIORITY_INVALID;
  }
  return undefined;
};

/**
 * Contact form validator implementation
 */
class ContactFormValidator implements FormValidator {
  /**
   * Validates the entire form data
   */
  validate(data: ContactFormData): ValidationResult {
    const errors: ContactFormErrors = {};

    const nameError = validateName(data.name);
    if (nameError) errors.name = nameError;

    const emailError = validateEmail(data.email);
    if (emailError) errors.email = emailError;

    const messageError = validateMessage(data.message);
    if (messageError) errors.message = messageError;

    const priorityError = validatePriority(data.priority);
    if (priorityError) errors.priority = priorityError;

    return {
      isValid: Object.keys(errors).length === 0,
      errors,
    };
  }

  /**
   * Validates a single field
   */
  validateField(field: keyof ContactFormData, value: string): string | undefined {
    switch (field) {
      case 'name':
        return validateName(value);
      case 'email':
        return validateEmail(value);
      case 'message':
        return validateMessage(value);
      case 'priority':
        return validatePriority(value);
      default:
        return undefined;
    }
  }
}

/**
 * Singleton instance for dependency injection
 */
export const contactFormValidator = new ContactFormValidator();
