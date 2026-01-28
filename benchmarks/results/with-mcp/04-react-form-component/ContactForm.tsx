import React from 'react';
import { ContactFormProps } from './types';
import { useContactForm } from './useContactForm';
import './ContactForm.css';

export const ContactForm: React.FC<ContactFormProps> = (props) => {
  const {
    formData,
    errors,
    touched,
    status,
    submitError,
    canSubmit,
    handleChange,
    handleBlur,
    handleSubmit,
    resetForm,
  } = useContactForm(props);

  const isLoading = status === 'loading';

  if (status === 'success') {
    return (
      <div className="contact-form-success" role="alert">
        <h3>Message Sent!</h3>
        <p>Thank you for your message. We'll get back to you soon.</p>
        <button type="button" onClick={resetForm} className="btn-reset">
          Send Another Message
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="contact-form" noValidate>
      {status === 'error' && submitError && (
        <div className="form-error-banner" role="alert">
          {submitError}
        </div>
      )}

      <div className="form-group">
        <label htmlFor="name">Name *</label>
        <input
          id="name"
          type="text"
          value={formData.name}
          onChange={handleChange('name')}
          onBlur={handleBlur('name')}
          aria-invalid={touched.name && !!errors.name}
          aria-describedby={errors.name ? 'name-error' : undefined}
          disabled={isLoading}
          autoComplete="name"
        />
        {touched.name && errors.name && (
          <span id="name-error" className="field-error" role="alert">
            {errors.name}
          </span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="email">Email *</label>
        <input
          id="email"
          type="email"
          value={formData.email}
          onChange={handleChange('email')}
          onBlur={handleBlur('email')}
          aria-invalid={touched.email && !!errors.email}
          aria-describedby={errors.email ? 'email-error' : undefined}
          disabled={isLoading}
          autoComplete="email"
        />
        {touched.email && errors.email && (
          <span id="email-error" className="field-error" role="alert">
            {errors.email}
          </span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="phone">Phone (optional)</label>
        <input
          id="phone"
          type="tel"
          value={formData.phone}
          onChange={handleChange('phone')}
          onBlur={handleBlur('phone')}
          aria-invalid={touched.phone && !!errors.phone}
          aria-describedby={errors.phone ? 'phone-error' : undefined}
          disabled={isLoading}
          autoComplete="tel"
        />
        {touched.phone && errors.phone && (
          <span id="phone-error" className="field-error" role="alert">
            {errors.phone}
          </span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="subject">Subject *</label>
        <input
          id="subject"
          type="text"
          value={formData.subject}
          onChange={handleChange('subject')}
          onBlur={handleBlur('subject')}
          aria-invalid={touched.subject && !!errors.subject}
          aria-describedby={errors.subject ? 'subject-error' : undefined}
          disabled={isLoading}
        />
        {touched.subject && errors.subject && (
          <span id="subject-error" className="field-error" role="alert">
            {errors.subject}
          </span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="message">Message *</label>
        <textarea
          id="message"
          value={formData.message}
          onChange={handleChange('message')}
          onBlur={handleBlur('message')}
          aria-invalid={touched.message && !!errors.message}
          aria-describedby={errors.message ? 'message-error' : undefined}
          disabled={isLoading}
          rows={5}
        />
        <span className="char-count">
          {formData.message.length}/1000 characters
        </span>
        {touched.message && errors.message && (
          <span id="message-error" className="field-error" role="alert">
            {errors.message}
          </span>
        )}
      </div>

      <button
        type="submit"
        className="btn-submit"
        disabled={!canSubmit}
        aria-busy={isLoading}
      >
        {isLoading ? 'Sending...' : 'Send Message'}
      </button>
    </form>
  );
};

export default ContactForm;
