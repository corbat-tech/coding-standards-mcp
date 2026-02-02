export type Priority = 'low' | 'medium' | 'high';

export interface ContactFormData {
  name: string;
  email: string;
  message: string;
  priority: Priority;
}

export interface ContactFormErrors {
  name?: string;
  email?: string;
  message?: string;
  priority?: string;
}

export interface FormState {
  isLoading: boolean;
  isSuccess: boolean;
  error: string | null;
}
