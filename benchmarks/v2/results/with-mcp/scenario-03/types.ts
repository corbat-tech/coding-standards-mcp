export interface ContactFormData {
  name: string;
  email: string;
  message: string;
}

export interface ValidationError {
  field: keyof ContactFormData;
  message: string;
}

export interface ContactFormState {
  data: ContactFormData;
  errors: ValidationError[];
  isSubmitting: boolean;
  isSubmitted: boolean;
}
