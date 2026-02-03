import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContactForm } from '../src/components/ContactForm';

describe('ContactForm', () => {
  const mockOnSubmit = vi.fn();
  const user = userEvent.setup();

  beforeEach(() => {
    mockOnSubmit.mockClear();
    mockOnSubmit.mockResolvedValue(undefined);
  });

  it('renders all form fields', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: /priority/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send message/i })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/name is required/i)).toBeInTheDocument();
    expect(screen.getByText(/email is required/i)).toBeInTheDocument();
    expect(screen.getByText(/message is required/i)).toBeInTheDocument();
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('shows error for invalid email', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'invalid-email');
    await user.type(screen.getByLabelText(/message/i), 'This is a test message');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/valid email/i)).toBeInTheDocument();
  });

  it('shows error for short name', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'J');
    await user.type(screen.getByLabelText(/email/i), 'test@test.com');
    await user.type(screen.getByLabelText(/message/i), 'This is a test message');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/at least 2 characters/i)).toBeInTheDocument();
  });

  it('shows error for short message', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'test@test.com');
    await user.type(screen.getByLabelText(/message/i), 'Short');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/at least 10 characters/i)).toBeInTheDocument();
  });

  it('submits form with valid data', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@test.com');
    await user.type(screen.getByLabelText(/message/i), 'This is a valid test message');
    await user.click(screen.getByLabelText(/high/i));
    await user.click(screen.getByRole('button', { name: /send message/i }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith({
        name: 'John Doe',
        email: 'john@test.com',
        message: 'This is a valid test message',
        priority: 'high',
      });
    });
  });

  it('shows loading state during submission', async () => {
    mockOnSubmit.mockImplementation(() => new Promise((resolve) => setTimeout(resolve, 100)));
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@test.com');
    await user.type(screen.getByLabelText(/message/i), 'This is a valid test message');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/sending/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sending/i })).toBeDisabled();
  });

  it('shows success message after successful submission', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@test.com');
    await user.type(screen.getByLabelText(/message/i), 'This is a valid test message');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/successfully/i)).toBeInTheDocument();
  });

  it('shows error message on submission failure', async () => {
    mockOnSubmit.mockRejectedValue(new Error('Network error'));
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@test.com');
    await user.type(screen.getByLabelText(/message/i), 'This is a valid test message');
    await user.click(screen.getByRole('button', { name: /send message/i }));

    expect(await screen.findByText(/network error/i)).toBeInTheDocument();
  });

  it('resets form when reset button is clicked', async () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.click(screen.getByRole('button', { name: /reset/i }));

    expect(screen.getByLabelText(/name/i)).toHaveValue('');
  });

  it('has proper ARIA attributes', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    expect(screen.getByRole('form', { name: /contact form/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/name/i)).toHaveAttribute('aria-invalid', 'false');
  });
});
