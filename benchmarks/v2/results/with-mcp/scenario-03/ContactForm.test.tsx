import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContactForm } from './ContactForm';
import { ContactFormSubmitter } from './useContactForm';

function createMockSubmitter(): ContactFormSubmitter {
  return {
    submit: vi.fn().mockResolvedValue(undefined),
  };
}

describe('ContactForm', () => {
  let submitter: ContactFormSubmitter;

  beforeEach(() => {
    submitter = createMockSubmitter();
  });

  describe('rendering', () => {
    it('should_render_all_fields_when_mounted', () => {
      // Arrange & Act
      render(<ContactForm submitter={submitter} />);

      // Assert
      expect(screen.getByLabelText('Name')).toBeInTheDocument();
      expect(screen.getByLabelText('Email')).toBeInTheDocument();
      expect(screen.getByLabelText('Message')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /send/i })).toBeInTheDocument();
    });
  });

  describe('validation', () => {
    it('should_show_error_when_name_empty', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      expect(screen.getByText('Name is required')).toBeInTheDocument();
    });

    it('should_show_error_when_email_invalid', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'invalid-email');
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      expect(screen.getByText('Invalid email format')).toBeInTheDocument();
    });

    it('should_show_error_when_message_too_short', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'john@example.com');
      await user.type(screen.getByLabelText('Message'), 'Hi');
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      expect(
        screen.getByText('Message must be at least 10 characters')
      ).toBeInTheDocument();
    });

    it('should_clear_field_error_when_user_types', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Act
      await user.type(screen.getByLabelText('Name'), 'John');

      // Assert
      expect(screen.queryByText('Name is required')).not.toBeInTheDocument();
    });
  });

  describe('submission', () => {
    it('should_submit_form_when_valid', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'john@example.com');
      await user.type(
        screen.getByLabelText('Message'),
        'This is a test message'
      );
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      await waitFor(() => {
        expect(submitter.submit).toHaveBeenCalledWith({
          name: 'John Doe',
          email: 'john@example.com',
          message: 'This is a test message',
        });
      });
    });

    it('should_show_success_message_when_submitted', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'john@example.com');
      await user.type(
        screen.getByLabelText('Message'),
        'This is a test message'
      );
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      await waitFor(() => {
        expect(screen.getByTestId('success-message')).toBeInTheDocument();
      });
    });

    it('should_disable_button_when_submitting', async () => {
      // Arrange
      (submitter.submit as ReturnType<typeof vi.fn>).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'john@example.com');
      await user.type(
        screen.getByLabelText('Message'),
        'This is a test message'
      );
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      expect(screen.getByRole('button')).toBeDisabled();
      expect(screen.getByText('Sending...')).toBeInTheDocument();
    });

    it('should_show_error_when_submission_fails', async () => {
      // Arrange
      (submitter.submit as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Network error')
      );
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();

      // Act
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'john@example.com');
      await user.type(
        screen.getByLabelText('Message'),
        'This is a test message'
      );
      await user.click(screen.getByRole('button', { name: /send/i }));

      // Assert
      await waitFor(() => {
        expect(screen.getByText('Failed to submit form')).toBeInTheDocument();
      });
    });
  });

  describe('reset', () => {
    it('should_show_form_again_when_reset_clicked', async () => {
      // Arrange
      render(<ContactForm submitter={submitter} />);
      const user = userEvent.setup();
      await user.type(screen.getByLabelText('Name'), 'John Doe');
      await user.type(screen.getByLabelText('Email'), 'john@example.com');
      await user.type(
        screen.getByLabelText('Message'),
        'This is a test message'
      );
      await user.click(screen.getByRole('button', { name: /send/i }));
      await waitFor(() => {
        expect(screen.getByTestId('success-message')).toBeInTheDocument();
      });

      // Act
      await user.click(
        screen.getByRole('button', { name: /send another message/i })
      );

      // Assert
      expect(screen.getByTestId('contact-form')).toBeInTheDocument();
    });
  });
});
