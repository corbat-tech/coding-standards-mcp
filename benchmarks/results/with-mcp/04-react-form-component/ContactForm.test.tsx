import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContactForm } from './ContactForm';

describe('ContactForm', () => {
  const mockSubmit = jest.fn();

  beforeEach(() => {
    mockSubmit.mockReset();
  });

  const fillValidForm = async () => {
    await userEvent.type(screen.getByLabelText(/name/i), 'John Doe');
    await userEvent.type(screen.getByLabelText(/email/i), 'john@example.com');
    await userEvent.type(screen.getByLabelText(/subject/i), 'Test Subject');
    await userEvent.type(
      screen.getByLabelText(/message/i),
      'This is a test message with enough characters.'
    );
  };

  describe('rendering', () => {
    it('should_render_all_form_fields', () => {
      // Arrange & Act
      render(<ContactForm onSubmit={mockSubmit} />);

      // Assert
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/phone/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/subject/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/message/i)).toBeInTheDocument();
    });

    it('should_render_submit_button', () => {
      // Arrange & Act
      render(<ContactForm onSubmit={mockSubmit} />);

      // Assert
      expect(screen.getByRole('button', { name: /send message/i })).toBeInTheDocument();
    });

    it('should_disable_submit_when_form_is_empty', () => {
      // Arrange & Act
      render(<ContactForm onSubmit={mockSubmit} />);

      // Assert
      expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
    });
  });

  describe('validation', () => {
    it('should_show_error_when_name_too_short', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await userEvent.type(screen.getByLabelText(/name/i), 'A');
      fireEvent.blur(screen.getByLabelText(/name/i));

      // Assert
      expect(await screen.findByText(/at least 2 characters/i)).toBeInTheDocument();
    });

    it('should_show_error_when_email_invalid', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await userEvent.type(screen.getByLabelText(/email/i), 'invalid-email');
      fireEvent.blur(screen.getByLabelText(/email/i));

      // Assert
      expect(await screen.findByText(/valid email/i)).toBeInTheDocument();
    });

    it('should_not_show_error_for_empty_optional_phone', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      fireEvent.blur(screen.getByLabelText(/phone/i));

      // Assert
      expect(screen.queryByText(/phone/i)).not.toHaveAttribute('role', 'alert');
    });

    it('should_show_error_when_phone_format_invalid', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await userEvent.type(screen.getByLabelText(/phone/i), 'abc');
      fireEvent.blur(screen.getByLabelText(/phone/i));

      // Assert
      expect(await screen.findByText(/valid phone/i)).toBeInTheDocument();
    });

    it('should_show_error_when_message_too_short', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await userEvent.type(screen.getByLabelText(/message/i), 'Short');
      fireEvent.blur(screen.getByLabelText(/message/i));

      // Assert
      expect(await screen.findByText(/at least 10 characters/i)).toBeInTheDocument();
    });

    it('should_enable_submit_when_form_valid', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await fillValidForm();

      // Assert
      expect(screen.getByRole('button', { name: /send message/i })).toBeEnabled();
    });
  });

  describe('submission', () => {
    it('should_call_onSubmit_with_form_data', async () => {
      // Arrange
      mockSubmit.mockResolvedValue(undefined);
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await fillValidForm();
      await userEvent.click(screen.getByRole('button', { name: /send message/i }));

      // Assert
      await waitFor(() => {
        expect(mockSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            name: 'John Doe',
            email: 'john@example.com',
            subject: 'Test Subject',
          })
        );
      });
    });

    it('should_show_loading_state_during_submission', async () => {
      // Arrange
      mockSubmit.mockImplementation(() => new Promise(() => {}));
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await fillValidForm();
      await userEvent.click(screen.getByRole('button', { name: /send message/i }));

      // Assert
      expect(screen.getByRole('button', { name: /sending/i })).toBeInTheDocument();
    });

    it('should_disable_inputs_during_submission', async () => {
      // Arrange
      mockSubmit.mockImplementation(() => new Promise(() => {}));
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await fillValidForm();
      await userEvent.click(screen.getByRole('button', { name: /send message/i }));

      // Assert
      expect(screen.getByLabelText(/name/i)).toBeDisabled();
    });

    it('should_show_success_message_after_submission', async () => {
      // Arrange
      mockSubmit.mockResolvedValue(undefined);
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await fillValidForm();
      await userEvent.click(screen.getByRole('button', { name: /send message/i }));

      // Assert
      expect(await screen.findByText(/message sent/i)).toBeInTheDocument();
    });

    it('should_show_error_message_when_submission_fails', async () => {
      // Arrange
      mockSubmit.mockRejectedValue(new Error('Network error'));
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await fillValidForm();
      await userEvent.click(screen.getByRole('button', { name: /send message/i }));

      // Assert
      expect(await screen.findByText(/network error/i)).toBeInTheDocument();
    });
  });

  describe('reset', () => {
    it('should_allow_sending_another_message_after_success', async () => {
      // Arrange
      mockSubmit.mockResolvedValue(undefined);
      render(<ContactForm onSubmit={mockSubmit} />);
      await fillValidForm();
      await userEvent.click(screen.getByRole('button', { name: /send message/i }));
      await screen.findByText(/message sent/i);

      // Act
      await userEvent.click(screen.getByRole('button', { name: /send another/i }));

      // Assert
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/name/i)).toHaveValue('');
    });
  });

  describe('accessibility', () => {
    it('should_have_aria_invalid_on_invalid_fields', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await userEvent.type(screen.getByLabelText(/email/i), 'invalid');
      fireEvent.blur(screen.getByLabelText(/email/i));

      // Assert
      expect(screen.getByLabelText(/email/i)).toHaveAttribute('aria-invalid', 'true');
    });

    it('should_have_error_messages_with_role_alert', async () => {
      // Arrange
      render(<ContactForm onSubmit={mockSubmit} />);

      // Act
      await userEvent.type(screen.getByLabelText(/name/i), 'A');
      fireEvent.blur(screen.getByLabelText(/name/i));

      // Assert
      const errorMessage = await screen.findByText(/at least 2 characters/i);
      expect(errorMessage).toHaveAttribute('role', 'alert');
    });
  });
});
