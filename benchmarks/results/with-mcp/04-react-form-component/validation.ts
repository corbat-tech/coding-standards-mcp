import { ContactFormData, ContactFormErrors } from './types';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^(\+?[1-9]\d{0,2}[-.\s]?)?(\(?\d{2,4}\)?[-.\s]?)?\d{3,4}[-.\s]?\d{3,4}$/;

export function validateName(value: string): string | undefined {
  const trimmed = value.trim();
  if (trimmed.length < 2) {
    return 'Name must be at least 2 characters';
  }
  if (trimmed.length > 50) {
    return 'Name must not exceed 50 characters';
  }
  return undefined;
}

export function validateEmail(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return 'Email is required';
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return 'Please enter a valid email address';
  }
  return undefined;
}

export function validatePhone(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined; // Phone is optional
  }
  if (!PHONE_REGEX.test(trimmed)) {
    return 'Please enter a valid phone number';
  }
  return undefined;
}

export function validateSubject(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return 'Subject is required';
  }
  if (trimmed.length > 100) {
    return 'Subject must not exceed 100 characters';
  }
  return undefined;
}

export function validateMessage(value: string): string | undefined {
  const trimmed = value.trim();
  if (trimmed.length < 10) {
    return 'Message must be at least 10 characters';
  }
  if (trimmed.length > 1000) {
    return 'Message must not exceed 1000 characters';
  }
  return undefined;
}

export function validateForm(data: ContactFormData): ContactFormErrors {
  return {
    name: validateName(data.name),
    email: validateEmail(data.email),
    phone: validatePhone(data.phone),
    subject: validateSubject(data.subject),
    message: validateMessage(data.message),
  };
}

export function isFormValid(errors: ContactFormErrors): boolean {
  return Object.values(errors).every((error) => error === undefined);
}
