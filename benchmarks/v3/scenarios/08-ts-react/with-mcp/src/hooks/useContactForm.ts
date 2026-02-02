import { useState, useCallback } from 'react';
import { ContactFormData, ContactFormErrors, FormState, Priority } from '../types/contact';

const INITIAL_FORM_DATA: ContactFormData = {
  name: '',
  email: '',
  message: '',
  priority: 'medium',
};

const INITIAL_STATE: FormState = {
  isLoading: false,
  isSuccess: false,
  error: null,
};

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateContactForm(data: ContactFormData): ContactFormErrors {
  const errors: ContactFormErrors = {};

  if (!data.name.trim()) {
    errors.name = 'Name is required';
  } else if (data.name.trim().length < 2) {
    errors.name = 'Name must be at least 2 characters';
  }

  if (!data.email.trim()) {
    errors.email = 'Email is required';
  } else if (!EMAIL_REGEX.test(data.email)) {
    errors.email = 'Please enter a valid email address';
  }

  if (!data.message.trim()) {
    errors.message = 'Message is required';
  } else if (data.message.trim().length < 10) {
    errors.message = 'Message must be at least 10 characters';
  }

  return errors;
}

interface UseContactFormReturn {
  formData: ContactFormData;
  errors: ContactFormErrors;
  state: FormState;
  handleChange: (field: keyof ContactFormData, value: string | Priority) => void;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  reset: () => void;
}

export function useContactForm(
  onSubmit: (data: ContactFormData) => Promise<void>
): UseContactFormReturn {
  const [formData, setFormData] = useState<ContactFormData>(INITIAL_FORM_DATA);
  const [errors, setErrors] = useState<ContactFormErrors>({});
  const [state, setState] = useState<FormState>(INITIAL_STATE);

  const handleChange = useCallback((field: keyof ContactFormData, value: string | Priority) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  }, []);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();

      const validationErrors = validateContactForm(formData);
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
        return;
      }

      setState({ isLoading: true, isSuccess: false, error: null });

      try {
        await onSubmit(formData);
        setState({ isLoading: false, isSuccess: true, error: null });
        setFormData(INITIAL_FORM_DATA);
      } catch (err) {
        setState({
          isLoading: false,
          isSuccess: false,
          error: err instanceof Error ? err.message : 'An error occurred',
        });
      }
    },
    [formData, onSubmit]
  );

  const reset = useCallback(() => {
    setFormData(INITIAL_FORM_DATA);
    setErrors({});
    setState(INITIAL_STATE);
  }, []);

  return { formData, errors, state, handleChange, handleSubmit, reset };
}
