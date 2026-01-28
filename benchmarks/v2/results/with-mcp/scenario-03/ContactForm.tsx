import React, { FormEvent } from 'react';
import { useContactForm, ContactFormSubmitter } from './useContactForm';
import { ContactFormData } from './types';

interface ContactFormProps {
  submitter: ContactFormSubmitter;
  onSuccess?: () => void;
}

export function ContactForm({ submitter, onSuccess }: ContactFormProps) {
  const {
    data,
    isSubmitting,
    isSubmitted,
    updateField,
    getFieldError,
    handleSubmit,
    reset,
  } = useContactForm(submitter);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await handleSubmit();
    if (onSuccess) {
      onSuccess();
    }
  };

  if (isSubmitted) {
    return (
      <div data-testid="success-message">
        <p>Thank you for your message!</p>
        <button type="button" onClick={reset}>
          Send another message
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} data-testid="contact-form">
      <FormField
        label="Name"
        name="name"
        value={data.name}
        error={getFieldError('name')}
        onChange={(value) => updateField('name', value)}
        disabled={isSubmitting}
      />
      <FormField
        label="Email"
        name="email"
        type="email"
        value={data.email}
        error={getFieldError('email')}
        onChange={(value) => updateField('email', value)}
        disabled={isSubmitting}
      />
      <FormField
        label="Message"
        name="message"
        value={data.message}
        error={getFieldError('message')}
        onChange={(value) => updateField('message', value)}
        disabled={isSubmitting}
        multiline
      />
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Sending...' : 'Send Message'}
      </button>
    </form>
  );
}

interface FormFieldProps {
  label: string;
  name: keyof ContactFormData;
  type?: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  multiline?: boolean;
}

function FormField({
  label,
  name,
  type = 'text',
  value,
  error,
  onChange,
  disabled,
  multiline,
}: FormFieldProps) {
  const inputId = `field-${name}`;
  const errorId = `error-${name}`;

  return (
    <div className="form-field">
      <label htmlFor={inputId}>{label}</label>
      {multiline ? (
        <textarea
          id={inputId}
          name={name}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          aria-describedby={error ? errorId : undefined}
          aria-invalid={!!error}
        />
      ) : (
        <input
          id={inputId}
          type={type}
          name={name}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          aria-describedby={error ? errorId : undefined}
          aria-invalid={!!error}
        />
      )}
      {error && (
        <span id={errorId} className="error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
