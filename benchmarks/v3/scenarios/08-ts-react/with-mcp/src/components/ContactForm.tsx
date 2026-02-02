import React from 'react';
import { useContactForm } from '../hooks/useContactForm';
import { ContactFormData, Priority } from '../types/contact';

interface ContactFormProps {
  onSubmit: (data: ContactFormData) => Promise<void>;
}

const PRIORITIES: { value: Priority; label: string }[] = [
  { value: 'low', label: 'Low' },
  { value: 'medium', label: 'Medium' },
  { value: 'high', label: 'High' },
];

export function ContactForm({ onSubmit }: ContactFormProps) {
  const { formData, errors, state, handleChange, handleSubmit, reset } = useContactForm(onSubmit);

  return (
    <form onSubmit={handleSubmit} aria-label="Contact form" noValidate>
      {state.error && (
        <div role="alert" aria-live="polite" className="error-banner">
          {state.error}
        </div>
      )}

      {state.isSuccess && (
        <div role="status" aria-live="polite" className="success-banner">
          Message sent successfully!
        </div>
      )}

      <div className="form-field">
        <label htmlFor="name">Name</label>
        <input
          id="name"
          type="text"
          value={formData.name}
          onChange={(e) => handleChange('name', e.target.value)}
          aria-describedby={errors.name ? 'name-error' : undefined}
          aria-invalid={!!errors.name}
          disabled={state.isLoading}
        />
        {errors.name && (
          <span id="name-error" role="alert" className="field-error">
            {errors.name}
          </span>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          type="email"
          value={formData.email}
          onChange={(e) => handleChange('email', e.target.value)}
          aria-describedby={errors.email ? 'email-error' : undefined}
          aria-invalid={!!errors.email}
          disabled={state.isLoading}
        />
        {errors.email && (
          <span id="email-error" role="alert" className="field-error">
            {errors.email}
          </span>
        )}
      </div>

      <div className="form-field">
        <label htmlFor="message">Message</label>
        <textarea
          id="message"
          value={formData.message}
          onChange={(e) => handleChange('message', e.target.value)}
          aria-describedby={errors.message ? 'message-error' : undefined}
          aria-invalid={!!errors.message}
          disabled={state.isLoading}
          rows={4}
        />
        {errors.message && (
          <span id="message-error" role="alert" className="field-error">
            {errors.message}
          </span>
        )}
      </div>

      <fieldset className="form-field">
        <legend>Priority</legend>
        <div role="radiogroup" aria-label="Select priority">
          {PRIORITIES.map(({ value, label }) => (
            <label key={value} className="radio-label">
              <input
                type="radio"
                name="priority"
                value={value}
                checked={formData.priority === value}
                onChange={() => handleChange('priority', value)}
                disabled={state.isLoading}
              />
              {label}
            </label>
          ))}
        </div>
      </fieldset>

      <div className="form-actions">
        <button type="submit" disabled={state.isLoading} aria-busy={state.isLoading}>
          {state.isLoading ? 'Sending...' : 'Send Message'}
        </button>
        <button type="button" onClick={reset} disabled={state.isLoading}>
          Reset
        </button>
      </div>
    </form>
  );
}
