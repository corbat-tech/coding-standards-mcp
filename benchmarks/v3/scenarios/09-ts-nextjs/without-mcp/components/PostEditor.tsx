'use client';

/**
 * PostEditor - Client Component
 * Interactive form for creating and editing blog posts
 */

import { useState, useCallback, FormEvent, ChangeEvent } from 'react';
import { useRouter } from 'next/navigation';
import { Post, CreatePostInput, UpdatePostInput, PostValidationErrors } from '@/types/post';
import { PostApiClient } from '@/lib/api-client';
import { VALIDATION_RULES } from '@/lib/validation';

interface PostEditorProps {
  post?: Post;
  mode: 'create' | 'edit';
}

interface FormState {
  title: string;
  content: string;
  excerpt: string;
  author: string;
  published: boolean;
}

type FormStatus = 'idle' | 'submitting' | 'success' | 'error';

function validateField(name: keyof FormState, value: string): string | undefined {
  switch (name) {
    case 'title':
      if (!value.trim()) return 'Title is required';
      if (value.trim().length < VALIDATION_RULES.title.minLength) {
        return `Title must be at least ${VALIDATION_RULES.title.minLength} characters`;
      }
      if (value.trim().length > VALIDATION_RULES.title.maxLength) {
        return `Title must not exceed ${VALIDATION_RULES.title.maxLength} characters`;
      }
      break;
    case 'content':
      if (!value.trim()) return 'Content is required';
      if (value.trim().length < VALIDATION_RULES.content.minLength) {
        return `Content must be at least ${VALIDATION_RULES.content.minLength} characters`;
      }
      if (value.trim().length > VALIDATION_RULES.content.maxLength) {
        return `Content must not exceed ${VALIDATION_RULES.content.maxLength} characters`;
      }
      break;
    case 'author':
      if (!value.trim()) return 'Author is required';
      if (value.trim().length < VALIDATION_RULES.author.minLength) {
        return `Author name must be at least ${VALIDATION_RULES.author.minLength} characters`;
      }
      if (value.trim().length > VALIDATION_RULES.author.maxLength) {
        return `Author name must not exceed ${VALIDATION_RULES.author.maxLength} characters`;
      }
      break;
    case 'excerpt':
      if (value.trim().length > VALIDATION_RULES.excerpt.maxLength) {
        return `Excerpt must not exceed ${VALIDATION_RULES.excerpt.maxLength} characters`;
      }
      break;
  }
  return undefined;
}

function validateForm(formData: FormState): PostValidationErrors {
  const errors: PostValidationErrors = {};

  const titleError = validateField('title', formData.title);
  if (titleError) errors.title = titleError;

  const contentError = validateField('content', formData.content);
  if (contentError) errors.content = contentError;

  const authorError = validateField('author', formData.author);
  if (authorError) errors.author = authorError;

  const excerptError = validateField('excerpt', formData.excerpt);
  if (excerptError) errors.excerpt = excerptError;

  return errors;
}

export default function PostEditor({ post, mode }: PostEditorProps) {
  const router = useRouter();

  const [formData, setFormData] = useState<FormState>({
    title: post?.title || '',
    content: post?.content || '',
    excerpt: post?.excerpt || '',
    author: post?.author || '',
    published: post?.published || false,
  });

  const [errors, setErrors] = useState<PostValidationErrors>({});
  const [touched, setTouched] = useState<Record<keyof FormState, boolean>>({
    title: false,
    content: false,
    excerpt: false,
    author: false,
    published: false,
  });
  const [status, setStatus] = useState<FormStatus>('idle');
  const [apiError, setApiError] = useState<string | null>(null);

  const handleChange = useCallback((
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    const newValue = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;

    setFormData((prev) => ({ ...prev, [name]: newValue }));

    // Clear error when user starts typing
    if (errors[name as keyof PostValidationErrors]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }

    // Clear API error when user makes changes
    if (apiError) {
      setApiError(null);
    }
  }, [errors, apiError]);

  const handleBlur = useCallback((
    e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;

    setTouched((prev) => ({ ...prev, [name]: true }));

    // Validate on blur
    const error = validateField(name as keyof FormState, value);
    if (error) {
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
  }, []);

  const handleSubmit = useCallback(async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // Validate all fields
    const validationErrors = validateForm(formData);
    setErrors(validationErrors);

    // Mark all fields as touched
    setTouched({
      title: true,
      content: true,
      excerpt: true,
      author: true,
      published: true,
    });

    // If there are validation errors, don't submit
    if (Object.keys(validationErrors).length > 0) {
      return;
    }

    setStatus('submitting');
    setApiError(null);

    try {
      if (mode === 'create') {
        const input: CreatePostInput = {
          title: formData.title.trim(),
          content: formData.content.trim(),
          excerpt: formData.excerpt.trim() || undefined,
          author: formData.author.trim(),
          published: formData.published,
        };

        const response = await PostApiClient.createPost(input);

        if (response.success && response.data) {
          setStatus('success');
          router.push(`/posts/${response.data.id}`);
        } else {
          setStatus('error');
          if (response.errors) {
            setErrors(response.errors);
          }
          setApiError(response.error || 'Failed to create post');
        }
      } else if (post) {
        const input: UpdatePostInput = {};

        // Only include changed fields
        if (formData.title.trim() !== post.title) input.title = formData.title.trim();
        if (formData.content.trim() !== post.content) input.content = formData.content.trim();
        if (formData.excerpt.trim() !== post.excerpt) input.excerpt = formData.excerpt.trim();
        if (formData.author.trim() !== post.author) input.author = formData.author.trim();
        if (formData.published !== post.published) input.published = formData.published;

        // Check if there are any changes
        if (Object.keys(input).length === 0) {
          setApiError('No changes to save');
          setStatus('idle');
          return;
        }

        const response = await PostApiClient.updatePost(post.id, input);

        if (response.success && response.data) {
          setStatus('success');
          router.push(`/posts/${response.data.id}`);
          router.refresh();
        } else {
          setStatus('error');
          if (response.errors) {
            setErrors(response.errors);
          }
          setApiError(response.error || 'Failed to update post');
        }
      }
    } catch (error) {
      setStatus('error');
      setApiError('An unexpected error occurred. Please try again.');
      console.error('Form submission error:', error);
    }
  }, [formData, mode, post, router]);

  const handleCancel = useCallback(() => {
    if (post) {
      router.push(`/posts/${post.id}`);
    } else {
      router.push('/posts');
    }
  }, [post, router]);

  const isSubmitting = status === 'submitting';
  const hasErrors = Object.keys(errors).length > 0;

  return (
    <form
      className="post-editor"
      onSubmit={handleSubmit}
      noValidate
      data-testid="post-editor-form"
    >
      <h1 className="post-editor-title">
        {mode === 'create' ? 'Create New Post' : 'Edit Post'}
      </h1>

      {apiError && (
        <div className="post-editor-error" role="alert" data-testid="api-error">
          {apiError}
        </div>
      )}

      <div className="form-group">
        <label htmlFor="title" className="form-label">
          Title <span className="required">*</span>
        </label>
        <input
          type="text"
          id="title"
          name="title"
          value={formData.title}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={isSubmitting}
          className={`form-input ${touched.title && errors.title ? 'form-input-error' : ''}`}
          placeholder="Enter post title"
          aria-describedby={errors.title ? 'title-error' : undefined}
          aria-invalid={touched.title && !!errors.title}
          data-testid="title-input"
        />
        {touched.title && errors.title && (
          <span id="title-error" className="form-error" role="alert" data-testid="title-error">
            {errors.title}
          </span>
        )}
        <span className="form-hint">
          {formData.title.length}/{VALIDATION_RULES.title.maxLength} characters
        </span>
      </div>

      <div className="form-group">
        <label htmlFor="author" className="form-label">
          Author <span className="required">*</span>
        </label>
        <input
          type="text"
          id="author"
          name="author"
          value={formData.author}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={isSubmitting}
          className={`form-input ${touched.author && errors.author ? 'form-input-error' : ''}`}
          placeholder="Enter author name"
          aria-describedby={errors.author ? 'author-error' : undefined}
          aria-invalid={touched.author && !!errors.author}
          data-testid="author-input"
        />
        {touched.author && errors.author && (
          <span id="author-error" className="form-error" role="alert" data-testid="author-error">
            {errors.author}
          </span>
        )}
      </div>

      <div className="form-group">
        <label htmlFor="excerpt" className="form-label">
          Excerpt
        </label>
        <textarea
          id="excerpt"
          name="excerpt"
          value={formData.excerpt}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={isSubmitting}
          className={`form-textarea form-textarea-small ${touched.excerpt && errors.excerpt ? 'form-input-error' : ''}`}
          placeholder="Brief description (optional - auto-generated if empty)"
          rows={3}
          aria-describedby={errors.excerpt ? 'excerpt-error' : undefined}
          aria-invalid={touched.excerpt && !!errors.excerpt}
          data-testid="excerpt-input"
        />
        {touched.excerpt && errors.excerpt && (
          <span id="excerpt-error" className="form-error" role="alert" data-testid="excerpt-error">
            {errors.excerpt}
          </span>
        )}
        <span className="form-hint">
          {formData.excerpt.length}/{VALIDATION_RULES.excerpt.maxLength} characters
        </span>
      </div>

      <div className="form-group">
        <label htmlFor="content" className="form-label">
          Content <span className="required">*</span>
        </label>
        <textarea
          id="content"
          name="content"
          value={formData.content}
          onChange={handleChange}
          onBlur={handleBlur}
          disabled={isSubmitting}
          className={`form-textarea ${touched.content && errors.content ? 'form-input-error' : ''}`}
          placeholder="Write your post content here..."
          rows={15}
          aria-describedby={errors.content ? 'content-error' : undefined}
          aria-invalid={touched.content && !!errors.content}
          data-testid="content-input"
        />
        {touched.content && errors.content && (
          <span id="content-error" className="form-error" role="alert" data-testid="content-error">
            {errors.content}
          </span>
        )}
        <span className="form-hint">
          {formData.content.length}/{VALIDATION_RULES.content.maxLength} characters
        </span>
      </div>

      <div className="form-group form-group-checkbox">
        <label className="form-checkbox-label">
          <input
            type="checkbox"
            name="published"
            checked={formData.published}
            onChange={handleChange}
            disabled={isSubmitting}
            className="form-checkbox"
            data-testid="published-input"
          />
          <span>Publish immediately</span>
        </label>
        <span className="form-hint">
          {formData.published
            ? 'This post will be visible to everyone'
            : 'This post will be saved as a draft'}
        </span>
      </div>

      <div className="form-actions">
        <button
          type="button"
          onClick={handleCancel}
          disabled={isSubmitting}
          className="btn btn-secondary"
          data-testid="cancel-button"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={isSubmitting || hasErrors}
          className="btn btn-primary"
          data-testid="submit-button"
        >
          {isSubmitting ? 'Saving...' : mode === 'create' ? 'Create Post' : 'Save Changes'}
        </button>
      </div>
    </form>
  );
}

// Export validation function for testing
export { validateField, validateForm };
