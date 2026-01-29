import React, { useState, useCallback, FormEvent, ChangeEvent } from 'react';

export type Priority = 'low' | 'medium' | 'high';

export interface ContactFormData {
  name: string;
  email: string;
  message: string;
  priority: Priority;
}

export interface ContactFormErrors {
  name?: string;
  email?: string;
  message?: string;
  priority?: string;
}

export interface ContactFormProps {
  onSubmit: (data: ContactFormData) => Promise<void>;
}

const initialFormData: ContactFormData = {
  name: '',
  email: '',
  message: '',
  priority: 'medium',
};

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateContactForm(data: ContactFormData): ContactFormErrors {
  const errors: ContactFormErrors = {};

  if (!data.name.trim()) {
    errors.name = 'Name is required';
  } else if (data.name.trim().length < 2) {
    errors.name = 'Name must be at least 2 characters';
  }

  if (!data.email.trim()) {
    errors.email = 'Email is required';
  } else if (!EMAIL_REGEX.test(data.email.trim())) {
    errors.email = 'Please enter a valid email address';
  }

  if (!data.message.trim()) {
    errors.message = 'Message is required';
  } else if (data.message.trim().length < 10) {
    errors.message = 'Message must be at least 10 characters';
  }

  if (!['low', 'medium', 'high'].includes(data.priority)) {
    errors.priority = 'Please select a valid priority';
  }

  return errors;
}

export const ContactForm: React.FC<ContactFormProps> = ({ onSubmit }) => {
  const [formData, setFormData] = useState<ContactFormData>(initialFormData);
  const [errors, setErrors] = useState<ContactFormErrors>({});
  const [isLoading, setIsLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleChange = useCallback(
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      const { name, value } = e.target;
      setFormData((prev) => ({ ...prev, [name]: value }));

      // Clear field error when user starts typing
      if (errors[name as keyof ContactFormErrors]) {
        setErrors((prev) => ({ ...prev, [name]: undefined }));
      }

      // Clear submit error when user modifies form
      if (submitError) {
        setSubmitError(null);
      }
    },
    [errors, submitError]
  );

  const handleSubmit = useCallback(
    async (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();

      // Validate form
      const validationErrors = validateContactForm(formData);
      if (Object.keys(validationErrors).length > 0) {
        setErrors(validationErrors);
        return;
      }

      setIsLoading(true);
      setSubmitError(null);

      try {
        await onSubmit(formData);
        setIsSubmitted(true);
        setFormData(initialFormData);
        setErrors({});
      } catch (error) {
        setSubmitError(
          error instanceof Error ? error.message : 'An unexpected error occurred. Please try again.'
        );
      } finally {
        setIsLoading(false);
      }
    },
    [formData, onSubmit]
  );

  if (isSubmitted) {
    return (
      <div role="status" aria-live="polite" className="contact-form-success">
        <h2>Thank you!</h2>
        <p>Your message has been sent successfully. We will get back to you soon.</p>
        <button
          type="button"
          onClick={() => setIsSubmitted(false)}
          aria-label="Send another message"
        >
          Send another message
        </button>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="contact-form"
      aria-label="Contact form"
      noValidate
    >
      <h1>Contact Us</h1>

      {submitError && (
        <div
          role="alert"
          aria-live="assertive"
          className="contact-form-error"
        >
          {submitError}
        </div>
      )}

      <div className="form-field">
        <label htmlFor="contact-name">
          Name <span aria-hidden="true">*</span>
        </label>
        <input
          type="text"
          id="contact-name"
          name="name"
          value={formData.name}
          onChange={handleChange}
          disabled={isLoading}
          aria-required="true"
          aria-invalid={!!errors.name}
          aria-describedby={errors.name ? 'name-error' : undefined}
          autoComplete="name"
        />
        {errors.name && (
          <span id="name-error" role="alert" className="field-error">
            {errors.name}
          </span>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="contact-email">
          Email <span aria-hidden="true">*</span>
        </label>
        <input
          type="email"
          id="contact-email"
          name="email"
          value={formData.email}
          onChange={handleChange}
          disabled={isLoading}
          aria-required="true"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? 'email-error' : undefined}
          autoComplete="email"
        />
        {errors.email && (
          <span id="email-error" role="alert" className="field-error">
            {errors.email}
          </span>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="contact-message">
          Message <span aria-hidden="true">*</span>
        </label>
        <textarea
          id="contact-message"
          name="message"
          value={formData.message}
          onChange={handleChange}
          disabled={isLoading}
          rows={5}
          aria-required="true"
          aria-invalid={!!errors.message}
          aria-describedby={errors.message ? 'message-error' : undefined}
        />
        {errors.message && (
          <span id="message-error" role="alert" className="field-error">
            {errors.message}
          </span>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="contact-priority">
          Priority <span aria-hidden="true">*</span>
        </label>
        <select
          id="contact-priority"
          name="priority"
          value={formData.priority}
          onChange={handleChange}
          disabled={isLoading}
          aria-required="true"
          aria-invalid={!!errors.priority}
          aria-describedby={errors.priority ? 'priority-error' : undefined}
        >
          <option value="low">Low</option>
          <option value="medium">Medium</option>
          <option value="high">High</option>
        </select>
        {errors.priority && (
          <span id="priority-error" role="alert" className="field-error">
            {errors.priority}
          </span>
        )}
      </div>

      <button
        type="submit"
        disabled={isLoading}
        aria-busy={isLoading}
        aria-label={isLoading ? 'Sending message...' : 'Send message'}
      >
        {isLoading ? 'Sending...' : 'Send Message'}
      </button>
    </form>
  );
};

export default ContactForm;
