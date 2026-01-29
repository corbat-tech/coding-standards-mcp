/**
 * useForm Hook
 * Custom hook for form state management with validation
 */

import { useState, useCallback, useMemo } from 'react';
import {
  ContactFormData,
  ContactFormErrors,
  FormStatus,
  UseFormReturn,
  FormValidator,
} from '../types';

/**
 * Configuration options for useForm hook
 */
interface UseFormOptions<T extends ContactFormData> {
  defaultValues: T;
  initialValues?: Partial<T>;
  onSubmit: (data: T) => Promise<void>;
  validator: FormValidator;
}

/**
 * Custom hook for managing form state with validation
 * Supports loading states, error handling, and form reset
 */
export function useForm<T extends ContactFormData>({
  defaultValues,
  initialValues,
  onSubmit,
  validator,
}: UseFormOptions<T>): UseFormReturn<T> {
  const mergedInitialValues = useMemo(
    () => ({ ...defaultValues, ...initialValues }) as T,
    [defaultValues, initialValues]
  );

  const [values, setValues] = useState<T>(mergedInitialValues);
  const [errors, setErrors] = useState<ContactFormErrors>({});
  const [status, setStatus] = useState<FormStatus>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  /**
   * Handle field value change
   * Clears the field error when value changes
   */
  const handleChange = useCallback(
    (field: keyof T, value: T[keyof T]) => {
      setValues((prev) => ({ ...prev, [field]: value }));
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field as keyof ContactFormErrors];
        return newErrors;
      });
      // Reset error state when user starts correcting
      if (status === 'error') {
        setStatus('idle');
        setErrorMessage(null);
      }
    },
    [status]
  );

  /**
   * Handle form submission
   * Validates form and calls onSubmit if valid
   */
  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();

      // Validate form
      const validationResult = validator.validate(values as ContactFormData);

      if (!validationResult.isValid) {
        setErrors(validationResult.errors);
        return;
      }

      // Clear errors and set submitting state
      setErrors({});
      setStatus('submitting');
      setErrorMessage(null);

      try {
        await onSubmit(values);
        setStatus('success');
        // Reset form on success
        setValues(mergedInitialValues);
      } catch (error) {
        setStatus('error');
        setErrorMessage(
          error instanceof Error ? error.message : 'An unexpected error occurred'
        );
      }
    },
    [values, validator, onSubmit, mergedInitialValues]
  );

  /**
   * Reset form to initial state
   */
  const resetForm = useCallback(() => {
    setValues(mergedInitialValues);
    setErrors({});
    setStatus('idle');
    setErrorMessage(null);
  }, [mergedInitialValues]);

  /**
   * Check if form is currently valid
   */
  const isValid = useMemo(() => {
    const validationResult = validator.validate(values as ContactFormData);
    return validationResult.isValid;
  }, [values, validator]);

  return {
    values,
    errors,
    status,
    errorMessage,
    handleChange,
    handleSubmit,
    resetForm,
    isValid,
  };
}
