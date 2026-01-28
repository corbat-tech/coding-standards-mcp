import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ContactForm } from './ContactForm';

describe('ContactForm', () => {
  const mockOnSubmit = jest.fn();

  beforeEach(() => {
    mockOnSubmit.mockClear();
  });

  it('renders all form fields', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send/i })).toBeInTheDocument();
  });

  it('shows error when name is empty', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(screen.getByTestId('name-error')).toHaveTextContent('Name is required');
  });

  it('shows error when email is empty', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'John' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(screen.getByTestId('email-error')).toHaveTextContent('Email is required');
  });

  it('shows error for invalid email format', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'invalid' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(screen.getByTestId('email-error')).toHaveTextContent('Invalid email format');
  });

  it('shows error when message is empty', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'john@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(screen.getByTestId('message-error')).toHaveTextContent('Message is required');
  });

  it('submits form with valid data', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'Hello!' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(mockOnSubmit).toHaveBeenCalledWith({
      name: 'John',
      email: 'john@example.com',
      message: 'Hello!',
    });
  });

  it('shows success message after submission', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'John' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'john@example.com' } });
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'Hello!' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    expect(screen.getByTestId('success-message')).toBeInTheDocument();
  });

  it('clears error when user types', () => {
    render(<ContactForm onSubmit={mockOnSubmit} />);

    fireEvent.click(screen.getByRole('button', { name: /send/i }));
    expect(screen.getByTestId('name-error')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'J' } });
    expect(screen.queryByTestId('name-error')).not.toBeInTheDocument();
  });
});
