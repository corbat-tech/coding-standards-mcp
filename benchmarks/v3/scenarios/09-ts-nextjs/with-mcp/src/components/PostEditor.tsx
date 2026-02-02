'use client';

import { useState } from 'react';
import { CreatePostSchema } from '@/lib/types';

interface PostEditorProps {
  onSubmit: (data: { title: string; content: string; author: string }) => Promise<void>;
}

export function PostEditor({ onSubmit }: PostEditorProps) {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [author, setAuthor] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    const result = CreatePostSchema.safeParse({ title, content, author });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((err) => {
        if (err.path[0]) fieldErrors[err.path[0].toString()] = err.message;
      });
      setErrors(fieldErrors);
      return;
    }

    setIsLoading(true);
    try {
      await onSubmit({ title, content, author });
      setTitle('');
      setContent('');
      setAuthor('');
    } catch {
      setErrors({ form: 'Failed to create post' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {errors.form && <p className="error">{errors.form}</p>}

      <div>
        <label htmlFor="title">Title</label>
        <input
          id="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          disabled={isLoading}
        />
        {errors.title && <span className="error">{errors.title}</span>}
      </div>

      <div>
        <label htmlFor="author">Author</label>
        <input
          id="author"
          value={author}
          onChange={(e) => setAuthor(e.target.value)}
          disabled={isLoading}
        />
        {errors.author && <span className="error">{errors.author}</span>}
      </div>

      <div>
        <label htmlFor="content">Content</label>
        <textarea
          id="content"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          disabled={isLoading}
          rows={10}
        />
        {errors.content && <span className="error">{errors.content}</span>}
      </div>

      <button type="submit" disabled={isLoading}>
        {isLoading ? 'Creating...' : 'Create Post'}
      </button>
    </form>
  );
}
