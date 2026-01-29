/**
 * ContactForm Component
 * Accessible contact form with validation, loading and error states
 */

import React from 'react';
import {
  ContactFormData,
  ContactFormProps,
  Priority,
} from '../types';
import { useForm, contactFormValidator } from '../hooks';

/**
 * Default form values
 */
const DEFAULT_VALUES: ContactFormData = {
  name: '',
  email: '',
  message: '',
  priority: 'medium',
};

/**
 * Priority options for the select dropdown
 */
const PRIORITY_OPTIONS: { value: Priority; label: string }[] = [
  { value: 'low', label: 'Low' },
  { value: 'medium', label: 'Medium' },
  { value: 'high', label: 'High' },
];

/**
 * ContactForm - Accessible form component with validation
 */
export const ContactForm: React.FC<ContactFormProps> = ({
  onSubmit,
  initialValues,
}) => {
  const {
    values,
    errors,
    status,
    errorMessage,
    handleChange,
    handleSubmit,
  } = useForm({
    defaultValues: DEFAULT_VALUES,
    initialValues,
    onSubmit,
    validator: contactFormValidator,
  });

  const isSubmitting = status === 'submitting';
  const isSuccess = status === 'success';
  const isError = status === 'error';

  /**
   * Generate aria-describedby value for a field
   */
  const getAriaDescribedBy = (fieldId: string, hasError: boolean): string => {
    const ids = [`${fieldId}-hint`];
    if (hasError) {
      ids.push(`${fieldId}-error`);
    }
    return ids.join(' ');
  };

  return (
    <form
      onSubmit={handleSubmit}
      noValidate
      aria-label="Contact form"
    >
      {/* Success Message */}
      {isSuccess && (
        <div
          role="status"
          aria-live="polite"
          className="success-message"
        >
          Message sent successfully!
        </div>
      )}

      {/* Error Message */}
      {isError && errorMessage && (
        <div
          role="alert"
          aria-live="assertive"
          className="error-banner"
        >
          {errorMessage}
        </div>
      )}

      {/* Name Field */}
      <div className="form-field">
        <label htmlFor="name">Name</label>
        <input
          type="text"
          id="name"
          name="name"
          value={values.name}
          onChange={(e) => handleChange('name', e.target.value)}
          disabled={isSubmitting}
          aria-required="true"
          aria-invalid={!!errors.name}
          aria-describedby={getAriaDescribedBy('name', !!errors.name)}
        />
        <span id="name-hint" className="hint">
          Enter your full name
        </span>
        {errors.name && (
          <span id="name-error" className="error" role="alert">
            {errors.name}
          </span>
        )}
      </div>

      {/* Email Field */}
      <div className="form-field">
        <label htmlFor="email">Email</label>
        <input
          type="email"
          id="email"
          name="email"
          value={values.email}
          onChange={(e) => handleChange('email', e.target.value)}
          disabled={isSubmitting}
          aria-required="true"
          aria-invalid={!!errors.email}
          aria-describedby={getAriaDescribedBy('email', !!errors.email)}
        />
        <span id="email-hint" className="hint">
          We will never share your email
        </span>
        {errors.email && (
          <span id="email-error" className="error" role="alert">
            {errors.email}
          </span>
        )}
      </div>

      {/* Message Field */}
      <div className="form-field">
        <label htmlFor="message">Message</label>
        <textarea
          id="message"
          name="message"
          value={values.message}
          onChange={(e) => handleChange('message', e.target.value)}
          disabled={isSubmitting}
          rows={4}
          aria-required="true"
          aria-invalid={!!errors.message}
          aria-describedby={getAriaDescribedBy('message', !!errors.message)}
        />
        <span id="message-hint" className="hint">
          Minimum 10 characters
        </span>
        {errors.message && (
          <span id="message-error" className="error" role="alert">
            {errors.message}
          </span>
        )}
      </div>

      {/* Priority Field */}
      <div className="form-field">
        <label htmlFor="priority">Priority</label>
        <select
          id="priority"
          name="priority"
          value={values.priority}
          onChange={(e) => handleChange('priority', e.target.value as Priority)}
          disabled={isSubmitting}
          aria-required="true"
          aria-invalid={!!errors.priority}
          aria-describedby={getAriaDescribedBy('priority', !!errors.priority)}
        >
          {PRIORITY_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <span id="priority-hint" className="hint">
          Select the urgency of your message
        </span>
        {errors.priority && (
          <span id="priority-error" className="error" role="alert">
            {errors.priority}
          </span>
        )}
      </div>

      {/* Submit Button */}
      <button
        type="submit"
        disabled={isSubmitting}
        aria-busy={isSubmitting}
      >
        {isSubmitting ? 'Submitting...' : 'Submit'}
      </button>
    </form>
  );
};
