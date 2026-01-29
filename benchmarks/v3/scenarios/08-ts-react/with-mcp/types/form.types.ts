/**
 * Contact Form Types
 * Defines all interfaces and types for the contact form component
 */

/**
 * Priority levels for contact form messages
 */
export type Priority = 'low' | 'medium' | 'high';

/**
 * Contact form data structure
 */
export interface ContactFormData {
  name: string;
  email: string;
  message: string;
  priority: Priority;
}

/**
 * Validation error messages for each field
 */
export interface ContactFormErrors {
  name?: string;
  email?: string;
  message?: string;
  priority?: string;
}

/**
 * Result of form validation
 */
export interface ValidationResult {
  isValid: boolean;
  errors: ContactFormErrors;
}

/**
 * Form submission state
 */
export type FormStatus = 'idle' | 'submitting' | 'success' | 'error';

/**
 * Props for the ContactForm component
 */
export interface ContactFormProps {
  onSubmit: (data: ContactFormData) => Promise<void>;
  initialValues?: Partial<ContactFormData>;
}

/**
 * Return type for useForm hook
 */
export interface UseFormReturn<T> {
  values: T;
  errors: ContactFormErrors;
  status: FormStatus;
  errorMessage: string | null;
  handleChange: (field: keyof T, value: T[keyof T]) => void;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  resetForm: () => void;
  isValid: boolean;
}
