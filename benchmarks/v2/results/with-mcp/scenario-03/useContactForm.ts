import { useState, useCallback } from 'react';
import { ContactFormData, ContactFormState, ValidationError } from './types';
import { validateContactForm } from './validation';

const INITIAL_DATA: ContactFormData = {
  name: '',
  email: '',
  message: '',
};

const INITIAL_STATE: ContactFormState = {
  data: INITIAL_DATA,
  errors: [],
  isSubmitting: false,
  isSubmitted: false,
};

export interface ContactFormSubmitter {
  submit(data: ContactFormData): Promise<void>;
}

export function useContactForm(submitter: ContactFormSubmitter) {
  const [state, setState] = useState<ContactFormState>(INITIAL_STATE);

  const updateField = useCallback(
    (field: keyof ContactFormData, value: string) => {
      setState((prev) => ({
        ...prev,
        data: { ...prev.data, [field]: value },
        errors: prev.errors.filter((e) => e.field !== field),
      }));
    },
    []
  );

  const getFieldError = useCallback(
    (field: keyof ContactFormData): string | undefined => {
      return state.errors.find((e) => e.field === field)?.message;
    },
    [state.errors]
  );

  const handleSubmit = useCallback(async () => {
    const errors = validateContactForm(state.data);
    if (errors.length > 0) {
      setState((prev) => ({ ...prev, errors }));
      return;
    }

    setState((prev) => ({ ...prev, isSubmitting: true, errors: [] }));

    try {
      await submitter.submit(state.data);
      setState((prev) => ({
        ...prev,
        isSubmitting: false,
        isSubmitted: true,
        data: INITIAL_DATA,
      }));
    } catch {
      setState((prev) => ({
        ...prev,
        isSubmitting: false,
        errors: [{ field: 'message', message: 'Failed to submit form' }],
      }));
    }
  }, [state.data, submitter]);

  const reset = useCallback(() => {
    setState(INITIAL_STATE);
  }, []);

  return {
    data: state.data,
    errors: state.errors,
    isSubmitting: state.isSubmitting,
    isSubmitted: state.isSubmitted,
    updateField,
    getFieldError,
    handleSubmit,
    reset,
  };
}
