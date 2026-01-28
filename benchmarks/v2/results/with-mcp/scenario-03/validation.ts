import { ContactFormData, ValidationError } from './types';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_MESSAGE_LENGTH = 10;
const MAX_MESSAGE_LENGTH = 1000;

export function validateContactForm(data: ContactFormData): ValidationError[] {
  const errors: ValidationError[] = [];

  errors.push(...validateName(data.name));
  errors.push(...validateEmail(data.email));
  errors.push(...validateMessage(data.message));

  return errors;
}

function validateName(name: string): ValidationError[] {
  const trimmed = name.trim();
  if (!trimmed) {
    return [{ field: 'name', message: 'Name is required' }];
  }
  if (trimmed.length < 2) {
    return [{ field: 'name', message: 'Name must be at least 2 characters' }];
  }
  return [];
}

function validateEmail(email: string): ValidationError[] {
  const trimmed = email.trim();
  if (!trimmed) {
    return [{ field: 'email', message: 'Email is required' }];
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return [{ field: 'email', message: 'Invalid email format' }];
  }
  return [];
}

function validateMessage(message: string): ValidationError[] {
  const trimmed = message.trim();
  if (!trimmed) {
    return [{ field: 'message', message: 'Message is required' }];
  }
  if (trimmed.length < MIN_MESSAGE_LENGTH) {
    return [
      {
        field: 'message',
        message: `Message must be at least ${MIN_MESSAGE_LENGTH} characters`,
      },
    ];
  }
  if (trimmed.length > MAX_MESSAGE_LENGTH) {
    return [
      {
        field: 'message',
        message: `Message cannot exceed ${MAX_MESSAGE_LENGTH} characters`,
      },
    ];
  }
  return [];
}
