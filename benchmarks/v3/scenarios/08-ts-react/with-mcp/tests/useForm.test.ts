/**
 * useForm Hook Tests
 * Tests for the custom form management hook
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useForm } from '../hooks/useForm';
import { ContactFormData } from '../types';
import { contactFormValidator } from '../hooks/validation';

describe('useForm', () => {
  const defaultValues: ContactFormData = {
    name: '',
    email: '',
    message: '',
    priority: 'medium',
  };

  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Initialization', () => {
    it('should initialize with default values', () => {
      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      expect(result.current.values).toEqual(defaultValues);
      expect(result.current.errors).toEqual({});
      expect(result.current.status).toBe('idle');
      expect(result.current.errorMessage).toBeNull();
    });

    it('should merge initial values with defaults', () => {
      const initialValues: Partial<ContactFormData> = {
        name: 'John',
        email: 'john@example.com',
      };

      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          initialValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      expect(result.current.values).toEqual({
        ...defaultValues,
        ...initialValues,
      });
    });
  });

  describe('handleChange', () => {
    it('should update field value', () => {
      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      act(() => {
        result.current.handleChange('name', 'John Doe');
      });

      expect(result.current.values.name).toBe('John Doe');
    });

    it('should clear field error when value changes', async () => {
      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      // Trigger validation error
      await act(async () => {
        await result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(result.current.errors.name).toBeDefined();

      // Change value to clear error
      act(() => {
        result.current.handleChange('name', 'John');
      });

      expect(result.current.errors.name).toBeUndefined();
    });
  });

  describe('handleSubmit', () => {
    it('should validate form before submission', async () => {
      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      await act(async () => {
        await result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(result.current.errors).not.toEqual({});
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('should call onSubmit when form is valid', async () => {
      mockOnSubmit.mockResolvedValueOnce(undefined);

      const validData: ContactFormData = {
        name: 'John Doe',
        email: 'john@example.com',
        message: 'This is a valid test message',
        priority: 'high',
      };

      const { result } = renderHook(() =>
        useForm({
          defaultValues: validData,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      await act(async () => {
        await result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(mockOnSubmit).toHaveBeenCalledWith(validData);
    });

    it('should set status to submitting during submission', async () => {
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 50)));

      const validData: ContactFormData = {
        name: 'John Doe',
        email: 'john@example.com',
        message: 'This is a valid test message',
        priority: 'medium',
      };

      const { result } = renderHook(() =>
        useForm({
          defaultValues: validData,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      act(() => {
        result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(result.current.status).toBe('submitting');

      await waitFor(() => {
        expect(result.current.status).toBe('success');
      });
    });

    it('should set status to success after successful submission', async () => {
      mockOnSubmit.mockResolvedValueOnce(undefined);

      const validData: ContactFormData = {
        name: 'John Doe',
        email: 'john@example.com',
        message: 'This is a valid test message',
        priority: 'low',
      };

      const { result } = renderHook(() =>
        useForm({
          defaultValues: validData,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      await act(async () => {
        await result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(result.current.status).toBe('success');
    });

    it('should set status to error and capture error message on failure', async () => {
      const errorMessage = 'Submission failed';
      mockOnSubmit.mockRejectedValueOnce(new Error(errorMessage));

      const validData: ContactFormData = {
        name: 'John Doe',
        email: 'john@example.com',
        message: 'This is a valid test message',
        priority: 'high',
      };

      const { result } = renderHook(() =>
        useForm({
          defaultValues: validData,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      await act(async () => {
        await result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(result.current.status).toBe('error');
      expect(result.current.errorMessage).toBe(errorMessage);
    });
  });

  describe('resetForm', () => {
    it('should reset form to initial values', async () => {
      mockOnSubmit.mockResolvedValueOnce(undefined);

      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      act(() => {
        result.current.handleChange('name', 'John Doe');
        result.current.handleChange('email', 'john@example.com');
      });

      expect(result.current.values.name).toBe('John Doe');

      act(() => {
        result.current.resetForm();
      });

      expect(result.current.values).toEqual(defaultValues);
      expect(result.current.errors).toEqual({});
      expect(result.current.status).toBe('idle');
    });
  });

  describe('isValid', () => {
    it('should return false when form has validation errors', async () => {
      const { result } = renderHook(() =>
        useForm({
          defaultValues,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      await act(async () => {
        await result.current.handleSubmit({ preventDefault: vi.fn() } as unknown as React.FormEvent);
      });

      expect(result.current.isValid).toBe(false);
    });

    it('should return true when form has no validation errors', () => {
      const validData: ContactFormData = {
        name: 'John Doe',
        email: 'john@example.com',
        message: 'This is a valid test message',
        priority: 'high',
      };

      const { result } = renderHook(() =>
        useForm({
          defaultValues: validData,
          onSubmit: mockOnSubmit,
          validator: contactFormValidator,
        })
      );

      expect(result.current.isValid).toBe(true);
    });
  });
});
