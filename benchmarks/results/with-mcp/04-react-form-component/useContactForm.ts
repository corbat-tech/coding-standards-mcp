import { useCallback, useState } from 'react';
import {
  ContactFormData,
  ContactFormErrors,
  ContactFormProps,
  FormStatus,
} from './types';
import { isFormValid, validateForm } from './validation';

const INITIAL_FORM_DATA: ContactFormData = {
  name: '',
  email: '',
  phone: '',
  subject: '',
  message: '',
};

export function useContactForm(props: ContactFormProps) {
  const { onSubmit, initialValues } = props;

  const [formData, setFormData] = useState<ContactFormData>({
    ...INITIAL_FORM_DATA,
    ...initialValues,
  });
  const [errors, setErrors] = useState<ContactFormErrors>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [status, setStatus] = useState<FormStatus>('idle');
  const [submitError, setSubmitError] = useState<string | null>(null);

  const handleChange = useCallback(
    (field: keyof ContactFormData) => (
      event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
    ) => {
      const value = event.target.value;
      setFormData((prev) => ({ ...prev, [field]: value }));

      if (touched[field]) {
        const fieldErrors = validateForm({ ...formData, [field]: value });
        setErrors((prev) => ({ ...prev, [field]: fieldErrors[field] }));
      }
    },
    [formData, touched]
  );

  const handleBlur = useCallback(
    (field: keyof ContactFormData) => () => {
      setTouched((prev) => ({ ...prev, [field]: true }));
      const fieldErrors = validateForm(formData);
      setErrors((prev) => ({ ...prev, [field]: fieldErrors[field] }));
    },
    [formData]
  );

  const handleSubmit = useCallback(
    async (event: React.FormEvent) => {
      event.preventDefault();

      const allTouched = Object.keys(INITIAL_FORM_DATA).reduce(
        (acc, key) => ({ ...acc, [key]: true }),
        {}
      );
      setTouched(allTouched);

      const formErrors = validateForm(formData);
      setErrors(formErrors);

      if (!isFormValid(formErrors)) {
        return;
      }

      setStatus('loading');
      setSubmitError(null);

      try {
        await onSubmit(formData);
        setStatus('success');
        setFormData(INITIAL_FORM_DATA);
        setTouched({});
      } catch (error) {
        setStatus('error');
        setSubmitError(
          error instanceof Error ? error.message : 'An error occurred'
        );
      }
    },
    [formData, onSubmit]
  );

  const resetForm = useCallback(() => {
    setFormData({ ...INITIAL_FORM_DATA, ...initialValues });
    setErrors({});
    setTouched({});
    setStatus('idle');
    setSubmitError(null);
  }, [initialValues]);

  const currentErrors = validateForm(formData);
  const canSubmit = isFormValid(currentErrors) && status !== 'loading';

  return {
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
  };
}
