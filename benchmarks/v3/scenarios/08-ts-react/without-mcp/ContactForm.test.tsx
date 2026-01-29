import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContactForm, validateContactForm, ContactFormData } from './ContactForm';

describe('ContactForm', () => {
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    mockOnSubmit.mockClear();
  });

  describe('rendering', () => {
    it('renders all form fields', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/priority/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /send message/i })).toBeInTheDocument();
    });

    it('renders priority options', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const prioritySelect = screen.getByLabelText(/priority/i);
      expect(prioritySelect).toHaveValue('medium');

      const options = screen.getAllByRole('option');
      expect(options).toHaveLength(3);
      expect(options[0]).toHaveValue('low');
      expect(options[1]).toHaveValue('medium');
      expect(options[2]).toHaveValue('high');
    });

    it('has proper accessibility attributes', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      expect(screen.getByRole('form', { name: /contact form/i })).toBeInTheDocument();

      const nameInput = screen.getByLabelText(/name/i);
      expect(nameInput).toHaveAttribute('aria-required', 'true');

      const emailInput = screen.getByLabelText(/email/i);
      expect(emailInput).toHaveAttribute('aria-required', 'true');

      const messageInput = screen.getByLabelText(/message/i);
      expect(messageInput).toHaveAttribute('aria-required', 'true');
    });
  });

  describe('validation', () => {
    it('shows error when name is empty', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/name is required/i)).toBeInTheDocument();
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });

    it('shows error when name is too short', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'A');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/name must be at least 2 characters/i)).toBeInTheDocument();
    });

    it('shows error when email is empty', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    });

    it('shows error when email is invalid', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'invalid-email');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/please enter a valid email address/i)).toBeInTheDocument();
    });

    it('shows error when message is empty', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/message is required/i)).toBeInTheDocument();
    });

    it('shows error when message is too short', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'Hi');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/message must be at least 10 characters/i)).toBeInTheDocument();
    });

    it('clears field error when user starts typing', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.click(screen.getByRole('button', { name: /send message/i }));
      expect(await screen.findByText(/name is required/i)).toBeInTheDocument();

      await user.type(screen.getByLabelText(/name/i), 'John');
      expect(screen.queryByText(/name is required/i)).not.toBeInTheDocument();
    });

    it('sets aria-invalid on fields with errors', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toHaveAttribute('aria-invalid', 'true');
      });
    });
  });

  describe('submission', () => {
    const fillValidForm = async (user: ReturnType<typeof userEvent.setup>) => {
      await user.type(screen.getByLabelText(/name/i), 'John Doe');
      await user.type(screen.getByLabelText(/email/i), 'john@example.com');
      await user.type(screen.getByLabelText(/message/i), 'This is a test message that is long enough.');
      await user.selectOptions(screen.getByLabelText(/priority/i), 'high');
    };

    it('submits form with valid data', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValueOnce(undefined);
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          name: 'John Doe',
          email: 'john@example.com',
          message: 'This is a test message that is long enough.',
          priority: 'high',
        });
      });
    });

    it('shows loading state during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByRole('button', { name: /sending/i })).toBeDisabled();
      expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
    });

    it('disables form fields during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toBeDisabled();
        expect(screen.getByLabelText(/email/i)).toBeDisabled();
        expect(screen.getByLabelText(/message/i)).toBeDisabled();
        expect(screen.getByLabelText(/priority/i)).toBeDisabled();
      });
    });

    it('shows success message after successful submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValueOnce(undefined);
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByText(/thank you/i)).toBeInTheDocument();
      expect(screen.getByText(/your message has been sent successfully/i)).toBeInTheDocument();
    });

    it('allows sending another message after success', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValueOnce(undefined);
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await screen.findByText(/thank you/i);
      await user.click(screen.getByRole('button', { name: /send another message/i }));

      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/name/i)).toHaveValue('');
    });

    it('shows error message when submission fails', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockRejectedValueOnce(new Error('Network error'));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByRole('alert')).toHaveTextContent(/network error/i);
    });

    it('shows generic error message for non-Error exceptions', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockRejectedValueOnce('Something went wrong');
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(await screen.findByRole('alert')).toHaveTextContent(/unexpected error/i);
    });

    it('clears error message when user modifies form', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockRejectedValueOnce(new Error('Network error'));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await screen.findByRole('alert');
      await user.type(screen.getByLabelText(/name/i), ' Jr.');

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });
});

describe('validateContactForm', () => {
  const validData: ContactFormData = {
    name: 'John Doe',
    email: 'john@example.com',
    message: 'This is a valid test message.',
    priority: 'medium',
  };

  it('returns no errors for valid data', () => {
    expect(validateContactForm(validData)).toEqual({});
  });

  it('returns error for empty name', () => {
    const errors = validateContactForm({ ...validData, name: '' });
    expect(errors.name).toBe('Name is required');
  });

  it('returns error for whitespace-only name', () => {
    const errors = validateContactForm({ ...validData, name: '   ' });
    expect(errors.name).toBe('Name is required');
  });

  it('returns error for name with only one character', () => {
    const errors = validateContactForm({ ...validData, name: 'A' });
    expect(errors.name).toBe('Name must be at least 2 characters');
  });

  it('returns error for empty email', () => {
    const errors = validateContactForm({ ...validData, email: '' });
    expect(errors.email).toBe('Email is required');
  });

  it('returns error for invalid email formats', () => {
    const invalidEmails = ['invalid', 'no@domain', '@example.com', 'no spaces@example.com'];

    invalidEmails.forEach((email) => {
      const errors = validateContactForm({ ...validData, email });
      expect(errors.email).toBe('Please enter a valid email address');
    });
  });

  it('returns error for empty message', () => {
    const errors = validateContactForm({ ...validData, message: '' });
    expect(errors.message).toBe('Message is required');
  });

  it('returns error for message too short', () => {
    const errors = validateContactForm({ ...validData, message: 'Short' });
    expect(errors.message).toBe('Message must be at least 10 characters');
  });

  it('returns error for invalid priority', () => {
    const errors = validateContactForm({ ...validData, priority: 'urgent' as any });
    expect(errors.priority).toBe('Please select a valid priority');
  });

  it('returns multiple errors when multiple fields are invalid', () => {
    const errors = validateContactForm({
      name: '',
      email: 'invalid',
      message: 'short',
      priority: 'invalid' as any,
    });

    expect(Object.keys(errors)).toHaveLength(4);
    expect(errors.name).toBeDefined();
    expect(errors.email).toBeDefined();
    expect(errors.message).toBeDefined();
    expect(errors.priority).toBeDefined();
  });
});
