import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContactForm } from './ContactForm';

describe('ContactForm', () => {
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    mockOnSubmit.mockClear();
  });

  const fillForm = async (user: ReturnType<typeof userEvent.setup>, overrides: Partial<{
    name: string;
    email: string;
    phone: string;
    subject: string;
    message: string;
  }> = {}) => {
    const values = {
      name: 'John Doe',
      email: 'john@example.com',
      phone: '1234567890',
      subject: 'Test Subject',
      message: 'This is a test message that is long enough.',
      ...overrides,
    };

    await user.type(screen.getByLabelText(/name/i), values.name);
    await user.type(screen.getByLabelText(/email/i), values.email);
    if (values.phone) {
      await user.type(screen.getByLabelText(/phone/i), values.phone);
    }
    await user.type(screen.getByLabelText(/subject/i), values.subject);
    await user.type(screen.getByLabelText(/message/i), values.message);
  };

  it('renders all form fields', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/subject/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send message/i })).toBeInTheDocument();
  });

  it('submit button is disabled initially', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
  });

  describe('Name validation', () => {
    it('shows error when name is empty on blur', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const nameInput = screen.getByLabelText(/name/i);
      await user.click(nameInput);
      await user.tab();

      expect(screen.getByText(/name is required/i)).toBeInTheDocument();
    });

    it('shows error when name is less than 2 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'A');
      await user.tab();

      expect(screen.getByText(/name must be at least 2 characters/i)).toBeInTheDocument();
    });

    it('shows error when name exceeds 50 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/name/i), 'A'.repeat(51));
      await user.tab();

      expect(screen.getByText(/name must be at most 50 characters/i)).toBeInTheDocument();
    });
  });

  describe('Email validation', () => {
    it('shows error when email is empty on blur', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const emailInput = screen.getByLabelText(/email/i);
      await user.click(emailInput);
      await user.tab();

      expect(screen.getByText(/email is required/i)).toBeInTheDocument();
    });

    it('shows error for invalid email format', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/email/i), 'invalid-email');
      await user.tab();

      expect(screen.getByText(/please enter a valid email/i)).toBeInTheDocument();
    });
  });

  describe('Phone validation', () => {
    it('does not show error when phone is empty (optional)', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const phoneInput = screen.getByLabelText(/phone/i);
      await user.click(phoneInput);
      await user.tab();

      expect(screen.queryByText(/phone/i, { selector: '.error' })).not.toBeInTheDocument();
    });

    it('shows error for invalid phone format', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/phone/i), '123');
      await user.tab();

      expect(screen.getByText(/please enter a valid phone number/i)).toBeInTheDocument();
    });
  });

  describe('Subject validation', () => {
    it('shows error when subject exceeds 100 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/subject/i), 'A'.repeat(101));
      await user.tab();

      expect(screen.getByText(/subject must be at most 100 characters/i)).toBeInTheDocument();
    });
  });

  describe('Message validation', () => {
    it('shows error when message is less than 10 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/message/i), 'Short');
      await user.tab();

      expect(screen.getByText(/message must be at least 10 characters/i)).toBeInTheDocument();
    });

    it('shows error when message exceeds 1000 characters', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.type(screen.getByLabelText(/message/i), 'A'.repeat(1001));
      await user.tab();

      expect(screen.getByText(/message must be at most 1000 characters/i)).toBeInTheDocument();
    });

    it('displays character count', () => {
      render(<ContactForm onSubmit={mockOnSubmit} />);

      expect(screen.getByText('0/1000')).toBeInTheDocument();
    });
  });

  describe('Form submission', () => {
    it('enables submit button when form is valid', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);

      expect(screen.getByRole('button', { name: /send message/i })).toBeEnabled();
    });

    it('calls onSubmit with form data on valid submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(undefined);
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          name: 'John Doe',
          email: 'john@example.com',
          phone: '1234567890',
          subject: 'Test Subject',
          message: 'This is a test message that is long enough.',
        });
      });
    });

    it('shows loading state during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(screen.getByRole('button', { name: /sending/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /sending/i })).toBeDisabled();
    });

    it('shows success message after successful submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(undefined);
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByText(/your message has been sent successfully/i)).toBeInTheDocument();
      });
    });

    it('shows error message after failed submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockRejectedValue(new Error('Network error'));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByText(/network error/i)).toBeInTheDocument();
      });
    });

    it('resets form after successful submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(undefined);
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toHaveValue('');
        expect(screen.getByLabelText(/email/i)).toHaveValue('');
        expect(screen.getByLabelText(/phone/i)).toHaveValue('');
        expect(screen.getByLabelText(/subject/i)).toHaveValue('');
        expect(screen.getByLabelText(/message/i)).toHaveValue('');
      });
    });

    it('disables form fields during submission', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await fillForm(user);
      await user.click(screen.getByRole('button', { name: /send message/i }));

      expect(screen.getByLabelText(/name/i)).toBeDisabled();
      expect(screen.getByLabelText(/email/i)).toBeDisabled();
      expect(screen.getByLabelText(/phone/i)).toBeDisabled();
      expect(screen.getByLabelText(/subject/i)).toBeDisabled();
      expect(screen.getByLabelText(/message/i)).toBeDisabled();
    });
  });

  describe('Accessibility', () => {
    it('has proper aria attributes for invalid fields', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      const nameInput = screen.getByLabelText(/name/i);
      await user.click(nameInput);
      await user.tab();

      expect(nameInput).toHaveAttribute('aria-invalid', 'true');
      expect(nameInput).toHaveAttribute('aria-describedby', 'name-error');
    });

    it('error messages have role="alert"', async () => {
      const user = userEvent.setup();
      render(<ContactForm onSubmit={mockOnSubmit} />);

      await user.click(screen.getByLabelText(/name/i));
      await user.tab();

      expect(screen.getByRole('alert')).toHaveTextContent(/name is required/i);
    });
  });
});
