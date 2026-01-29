/**
 * ContactForm Component Tests
 * Tests written following TDD approach before implementation
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContactForm } from '../components/ContactForm';
import { ContactFormData } from '../types';

describe('ContactForm', () => {
  const mockOnSubmit = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render all form fields', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/priority/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /submit/i })).toBeInTheDocument();
    });

    it('should render priority options', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const prioritySelect = screen.getByLabelText(/priority/i);
      expect(prioritySelect).toBeInTheDocument();

      // Check for priority options
      expect(screen.getByRole('option', { name: /low/i })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: /medium/i })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: /high/i })).toBeInTheDocument();
    });

    it('should render with initial values when provided', () => {
      const initialValues: Partial<ContactFormData> = {
        name: 'John Doe',
        email: 'john@example.com',
      };

      render(<ContactForm onSubmit={mockOnSubmit} initialValues={initialValues} />);

      expect(screen.getByLabelText(/name/i)).toHaveValue('John Doe');
      expect(screen.getByLabelText(/email/i)).toHaveValue('john@example.com');
    });
  });

  describe('Accessibility', () => {
    it('should have accessible aria-labels on all inputs', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const nameInput = screen.getByLabelText(/name/i);
      const emailInput = screen.getByLabelText(/email/i);
      const messageInput = screen.getByLabelText(/message/i);
      const prioritySelect = screen.getByLabelText(/priority/i);

      expect(nameInput).toHaveAttribute('aria-describedby');
      expect(emailInput).toHaveAttribute('aria-describedby');
      expect(messageInput).toHaveAttribute('aria-describedby');
      expect(prioritySelect).toHaveAttribute('aria-describedby');
    });

    it('should mark required fields with aria-required', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      expect(screen.getByLabelText(/name/i)).toHaveAttribute('aria-required', 'true');
      expect(screen.getByLabelText(/email/i)).toHaveAttribute('aria-required', 'true');
      expect(screen.getByLabelText(/message/i)).toHaveAttribute('aria-required', 'true');
      expect(screen.getByLabelText(/priority/i)).toHaveAttribute('aria-required', 'true');
    });

    it('should set aria-invalid when field has error', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toHaveAttribute('aria-invalid', 'true');
      });
    });

    it('should associate error messages with inputs via aria-describedby', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        const nameInput = screen.getByLabelText(/name/i);
        const describedBy = nameInput.getAttribute('aria-describedby');
        expect(describedBy).toContain('name-error');
      });
    });
  });

  describe('Validation', () => {
    it('should show validation errors for empty required fields on submit', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument();
        expect(screen.getByText(/email is required/i)).toBeInTheDocument();
        expect(screen.getByText(/message is required/i)).toBeInTheDocument();
      });

      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('should show validation error for invalid email format', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const emailInput = screen.getByLabelText(/email/i);
      await user.type(emailInput, 'invalid-email');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/please enter a valid email/i)).toBeInTheDocument();
      });
    });

    it('should show validation error for name less than 2 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const nameInput = screen.getByLabelText(/name/i);
      await user.type(nameInput, 'A');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/name must be at least 2 characters/i)).toBeInTheDocument();
      });
    });

    it('should show validation error for message less than 10 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const messageInput = screen.getByLabelText(/message/i);
      await user.type(messageInput, 'Short');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/message must be at least 10 characters/i)).toBeInTheDocument();
      });
    });

    it('should clear field error when user starts typing', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      // Submit to trigger validation errors
      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument();
      });

      // Start typing in name field
      const nameInput = screen.getByLabelText(/name/i);
      await user.type(nameInput, 'John');

      await waitFor(() => {
        expect(screen.queryByText(/name is required/i)).not.toBeInTheDocument();
      });
    });
  });

  describe('Form Submission', () => {
    it('should call onSubmit with form data when form is valid', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValueOnce(undefined);

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');
      await user.selectOptions(screen.getByLabelText(/priority/i), 'high');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          name: 'John Doe',
          email: 'john@example.com',
          message: 'This is a test message that is long enough',
          priority: 'high',
        });
      });
    });

    it('should show loading state during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');
      await user.selectOptions(screen.getByLabelText(/priority/i), 'medium');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      expect(screen.getByText(/submitting/i)).toBeInTheDocument();
      expect(submitButton).toBeDisabled();

      await waitFor(() => {
        expect(screen.queryByText(/submitting/i)).not.toBeInTheDocument();
      });
    });

    it('should show error state on submission failure', async () => {
      const user = userEvent.setup();
      const errorMessage = 'Network error occurred';
      mockOnSubmit.mockRejectedValueOnce(new Error(errorMessage));

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');
      await user.selectOptions(screen.getByLabelText(/priority/i), 'low');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(errorMessage);
      });
    });

    it('should show success message after successful submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValueOnce(undefined);

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');
      await user.selectOptions(screen.getByLabelText(/priority/i), 'medium');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/message sent successfully/i)).toBeInTheDocument();
      });
    });

    it('should reset form after successful submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValueOnce(undefined);

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');
      await user.selectOptions(screen.getByLabelText(/priority/i), 'high');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toHaveValue('');
        expect(screen.getByLabelText(/email/i)).toHaveValue('');
        expect(screen.getByLabelText(/message/i)).toHaveValue('');
        expect(screen.getByLabelText(/priority/i)).toHaveValue('medium');
      });
    });
  });

  describe('Form State', () => {
    it('should disable submit button during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      expect(submitButton).toBeDisabled();

      await waitFor(() => {
        expect(submitButton).not.toBeDisabled();
      });
    });

    it('should disable all inputs during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));

      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough');

      const submitButton = screen.getByRole('button', { name: /submit/i });
      await user.click(submitButton);

      expect(screen.getByLabelText(/name/i)).toBeDisabled();
      expect(screen.getByLabelText(/email/i)).toBeDisabled();
      expect(screen.getByLabelText(/message/i)).toBeDisabled();
      expect(screen.getByLabelText(/priority/i)).toBeDisabled();

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).not.toBeDisabled();
      });
    });
  });
});
