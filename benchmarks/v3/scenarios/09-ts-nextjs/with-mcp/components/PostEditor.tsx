'use client';

/**
 * PostEditor Client Component
 * Form for creating and editing blog posts with validation
 */

import { useState, useCallback, type FormEvent, type ChangeEvent } from 'react';
import type {
  Post,
  CreatePostRequest,
  UpdatePostRequest,
  ValidationError,
} from '../types';
import { PostStatus } from '../types';

/** Props for PostEditor component */
interface PostEditorProps {
  post?: Post;
  onSubmit: (data: CreatePostRequest | UpdatePostRequest) => Promise<void>;
  onCancel?: () => void;
  isLoading?: boolean;
}

/** Form state interface */
interface FormState {
  title: string;
  content: string;
  author: string;
  status: PostStatus;
}

/** Initial form state */
function getInitialState(post?: Post): FormState {
  return {
    title: post?.title ?? '',
    content: post?.content ?? '',
    author: post?.author ?? '',
    status: post?.status ?? PostStatus.DRAFT,
  };
}

/** Client-side validation */
function validateForm(
  state: FormState,
  isEditing: boolean,
): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!state.title.trim()) {
    errors.push({ field: 'title', message: 'Title is required' });
  } else if (state.title.trim().length < 3) {
    errors.push({
      field: 'title',
      message: 'Title must be at least 3 characters',
    });
  }

  if (!state.content.trim()) {
    errors.push({ field: 'content', message: 'Content is required' });
  } else if (state.content.trim().length < 10) {
    errors.push({
      field: 'content',
      message: 'Content must be at least 10 characters',
    });
  }

  if (!isEditing && !state.author.trim()) {
    errors.push({ field: 'author', message: 'Author is required' });
  }

  return errors;
}

/** Form field component */
function FormField({
  id,
  label,
  error,
  children,
}: {
  id: string;
  label: string;
  error?: string;
  children: React.ReactNode;
}): JSX.Element {
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-medium text-gray-700">
        {label}
      </label>
      {children}
      {error && (
        <p className="text-sm text-red-600" data-testid={`error-${id}`}>
          {error}
        </p>
      )}
    </div>
  );
}

/** Main PostEditor component - Client Component */
export default function PostEditor({
  post,
  onSubmit,
  onCancel,
  isLoading = false,
}: PostEditorProps): JSX.Element {
  const isEditing = !!post;
  const [formState, setFormState] = useState<FormState>(() =>
    getInitialState(post),
  );
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const getFieldError = (field: string): string | undefined => {
    return errors.find((e) => e.field === field)?.message;
  };

  const handleChange = useCallback(
    (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
      const { name, value } = e.target;
      setFormState((prev) => ({ ...prev, [name]: value }));
      setErrors((prev) => prev.filter((err) => err.field !== name));
      setSubmitError(null);
    },
    [],
  );

  const handleSubmit = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      setSubmitError(null);

      const validationErrors = validateForm(formState, isEditing);

      if (validationErrors.length > 0) {
        setErrors(validationErrors);
        return;
      }

      try {
        if (isEditing) {
          const updateData: UpdatePostRequest = {
            title: formState.title,
            content: formState.content,
            status: formState.status,
          };
          await onSubmit(updateData);
        } else {
          const createData: CreatePostRequest = {
            title: formState.title,
            content: formState.content,
            author: formState.author,
            status: formState.status,
          };
          await onSubmit(createData);
        }
      } catch (error) {
        setSubmitError(
          error instanceof Error ? error.message : 'Failed to save post',
        );
      }
    },
    [formState, isEditing, onSubmit],
  );

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-6"
      data-testid="post-editor-form"
    >
      <h2 className="text-xl font-semibold text-gray-900">
        {isEditing ? 'Edit Post' : 'Create New Post'}
      </h2>

      {submitError && (
        <div
          className="p-4 bg-red-50 border border-red-200 rounded-md"
          data-testid="submit-error"
        >
          <p className="text-sm text-red-700">{submitError}</p>
        </div>
      )}

      <FormField id="title" label="Title" error={getFieldError('title')}>
        <input
          type="text"
          id="title"
          name="title"
          value={formState.title}
          onChange={handleChange}
          disabled={isLoading}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
          placeholder="Enter post title"
          data-testid="input-title"
        />
      </FormField>

      <FormField id="content" label="Content" error={getFieldError('content')}>
        <textarea
          id="content"
          name="content"
          value={formState.content}
          onChange={handleChange}
          disabled={isLoading}
          rows={8}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
          placeholder="Write your post content..."
          data-testid="input-content"
        />
      </FormField>

      {!isEditing && (
        <FormField id="author" label="Author" error={getFieldError('author')}>
          <input
            type="text"
            id="author"
            name="author"
            value={formState.author}
            onChange={handleChange}
            disabled={isLoading}
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
            placeholder="Your name"
            data-testid="input-author"
          />
        </FormField>
      )}

      <FormField id="status" label="Status" error={getFieldError('status')}>
        <select
          id="status"
          name="status"
          value={formState.status}
          onChange={handleChange}
          disabled={isLoading}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
          data-testid="input-status"
        >
          <option value={PostStatus.DRAFT}>Draft</option>
          <option value={PostStatus.PUBLISHED}>Published</option>
          <option value={PostStatus.ARCHIVED}>Archived</option>
        </select>
      </FormField>

      <div className="flex gap-4">
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
          data-testid="submit-button"
        >
          {isLoading ? 'Saving...' : isEditing ? 'Update Post' : 'Create Post'}
        </button>

        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 focus:ring-2 focus:ring-gray-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
            data-testid="cancel-button"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
