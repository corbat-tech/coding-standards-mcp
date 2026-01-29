/**
 * Component Tests
 * Tests for PostList and PostEditor components
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostList from '../components/PostList';
import PostEditor from '../components/PostEditor';
import { PostStatus } from '../types';
import type { Post } from '../types';

// Mock posts for testing
const mockPosts: Post[] = [
  {
    id: 'post-1',
    title: 'First Post',
    content: 'This is the first post content',
    author: 'John Doe',
    status: PostStatus.PUBLISHED,
    createdAt: new Date('2024-01-15T10:00:00'),
    updatedAt: new Date('2024-01-15T10:00:00'),
  },
  {
    id: 'post-2',
    title: 'Second Post',
    content: 'This is the second post content',
    author: 'Jane Smith',
    status: PostStatus.DRAFT,
    createdAt: new Date('2024-01-14T10:00:00'),
    updatedAt: new Date('2024-01-14T10:00:00'),
  },
];

describe('PostList', () => {
  it('should render empty state when no posts', () => {
    render(<PostList posts={[]} />);

    expect(screen.getByText('No posts')).toBeInTheDocument();
    expect(
      screen.getByText('Get started by creating a new post.'),
    ).toBeInTheDocument();
  });

  it('should render list of posts', () => {
    render(<PostList posts={mockPosts} />);

    expect(screen.getByText('First Post')).toBeInTheDocument();
    expect(screen.getByText('Second Post')).toBeInTheDocument();
  });

  it('should display post content preview', () => {
    render(<PostList posts={mockPosts} />);

    expect(
      screen.getByText('This is the first post content'),
    ).toBeInTheDocument();
  });

  it('should display author name', () => {
    render(<PostList posts={mockPosts} />);

    expect(screen.getByText('By John Doe')).toBeInTheDocument();
    expect(screen.getByText('By Jane Smith')).toBeInTheDocument();
  });

  it('should display status badges', () => {
    render(<PostList posts={mockPosts} />);

    expect(screen.getByText('Published')).toBeInTheDocument();
    expect(screen.getByText('Draft')).toBeInTheDocument();
  });

  it('should call onSelectPost when post is clicked', () => {
    const onSelectPost = vi.fn();
    render(<PostList posts={mockPosts} onSelectPost={onSelectPost} />);

    fireEvent.click(screen.getByTestId('post-card-post-1'));

    expect(onSelectPost).toHaveBeenCalledWith(mockPosts[0]);
  });

  it('should render heading', () => {
    render(<PostList posts={mockPosts} />);

    expect(screen.getByText('Blog Posts')).toBeInTheDocument();
  });
});

describe('PostEditor', () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Create Mode', () => {
    it('should render create form', () => {
      render(<PostEditor onSubmit={mockOnSubmit} />);

      expect(screen.getByText('Create New Post')).toBeInTheDocument();
      expect(screen.getByTestId('input-title')).toBeInTheDocument();
      expect(screen.getByTestId('input-content')).toBeInTheDocument();
      expect(screen.getByTestId('input-author')).toBeInTheDocument();
      expect(screen.getByTestId('input-status')).toBeInTheDocument();
    });

    it('should show validation error for empty title', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.click(screen.getByTestId('submit-button'));

      expect(screen.getByTestId('error-title')).toHaveTextContent(
        'Title is required',
      );
    });

    it('should show validation error for short title', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.type(screen.getByTestId('input-title'), 'AB');
      await user.click(screen.getByTestId('submit-button'));

      expect(screen.getByTestId('error-title')).toHaveTextContent(
        'Title must be at least 3 characters',
      );
    });

    it('should show validation error for empty content', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.click(screen.getByTestId('submit-button'));

      expect(screen.getByTestId('error-content')).toHaveTextContent(
        'Content is required',
      );
    });

    it('should show validation error for short content', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.type(screen.getByTestId('input-content'), 'Short');
      await user.click(screen.getByTestId('submit-button'));

      expect(screen.getByTestId('error-content')).toHaveTextContent(
        'Content must be at least 10 characters',
      );
    });

    it('should show validation error for empty author', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.click(screen.getByTestId('submit-button'));

      expect(screen.getByTestId('error-author')).toHaveTextContent(
        'Author is required',
      );
    });

    it('should submit valid form data', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(undefined);

      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.type(screen.getByTestId('input-title'), 'Test Title');
      await user.type(
        screen.getByTestId('input-content'),
        'This is test content that is long enough.',
      );
      await user.type(screen.getByTestId('input-author'), 'Test Author');
      await user.click(screen.getByTestId('submit-button'));

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          title: 'Test Title',
          content: 'This is test content that is long enough.',
          author: 'Test Author',
          status: PostStatus.DRAFT,
        });
      });
    });

    it('should show submit error on failure', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockRejectedValue(new Error('Server error'));

      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.type(screen.getByTestId('input-title'), 'Test Title');
      await user.type(
        screen.getByTestId('input-content'),
        'This is test content that is long enough.',
      );
      await user.type(screen.getByTestId('input-author'), 'Test Author');
      await user.click(screen.getByTestId('submit-button'));

      await waitFor(() => {
        expect(screen.getByTestId('submit-error')).toHaveTextContent(
          'Server error',
        );
      });
    });

    it('should call onCancel when cancel button clicked', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

      await user.click(screen.getByTestId('cancel-button'));

      expect(mockOnCancel).toHaveBeenCalled();
    });

    it('should disable form when loading', () => {
      render(<PostEditor onSubmit={mockOnSubmit} isLoading={true} />);

      expect(screen.getByTestId('input-title')).toBeDisabled();
      expect(screen.getByTestId('input-content')).toBeDisabled();
      expect(screen.getByTestId('input-author')).toBeDisabled();
      expect(screen.getByTestId('submit-button')).toBeDisabled();
    });

    it('should show loading text on submit button', () => {
      render(<PostEditor onSubmit={mockOnSubmit} isLoading={true} />);

      expect(screen.getByTestId('submit-button')).toHaveTextContent('Saving...');
    });
  });

  describe('Edit Mode', () => {
    const existingPost: Post = mockPosts[0];

    it('should render edit form with existing data', () => {
      render(<PostEditor post={existingPost} onSubmit={mockOnSubmit} />);

      expect(screen.getByText('Edit Post')).toBeInTheDocument();
      expect(screen.getByTestId('input-title')).toHaveValue('First Post');
      expect(screen.getByTestId('input-content')).toHaveValue(
        'This is the first post content',
      );
    });

    it('should not show author field in edit mode', () => {
      render(<PostEditor post={existingPost} onSubmit={mockOnSubmit} />);

      expect(screen.queryByTestId('input-author')).not.toBeInTheDocument();
    });

    it('should submit update data', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(undefined);

      render(<PostEditor post={existingPost} onSubmit={mockOnSubmit} />);

      await user.clear(screen.getByTestId('input-title'));
      await user.type(screen.getByTestId('input-title'), 'Updated Title');
      await user.click(screen.getByTestId('submit-button'));

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          title: 'Updated Title',
          content: 'This is the first post content',
          status: PostStatus.PUBLISHED,
        });
      });
    });

    it('should show Update Post button text', () => {
      render(<PostEditor post={existingPost} onSubmit={mockOnSubmit} />);

      expect(screen.getByTestId('submit-button')).toHaveTextContent(
        'Update Post',
      );
    });
  });

  describe('Status Selection', () => {
    it('should allow selecting different status', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.selectOptions(
        screen.getByTestId('input-status'),
        PostStatus.PUBLISHED,
      );

      expect(screen.getByTestId('input-status')).toHaveValue(
        PostStatus.PUBLISHED,
      );
    });

    it('should include status in submit data', async () => {
      const user = userEvent.setup();
      mockOnSubmit.mockResolvedValue(undefined);

      render(<PostEditor onSubmit={mockOnSubmit} />);

      await user.type(screen.getByTestId('input-title'), 'Test Title');
      await user.type(
        screen.getByTestId('input-content'),
        'This is test content that is long enough.',
      );
      await user.type(screen.getByTestId('input-author'), 'Test Author');
      await user.selectOptions(
        screen.getByTestId('input-status'),
        PostStatus.PUBLISHED,
      );
      await user.click(screen.getByTestId('submit-button'));

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith(
          expect.objectContaining({
            status: PostStatus.PUBLISHED,
          }),
        );
      });
    });
  });

  describe('Error Clearing', () => {
    it('should clear field error when user types', async () => {
      const user = userEvent.setup();
      render(<PostEditor onSubmit={mockOnSubmit} />);

      // Trigger validation error
      await user.click(screen.getByTestId('submit-button'));
      expect(screen.getByTestId('error-title')).toBeInTheDocument();

      // Type to clear error
      await user.type(screen.getByTestId('input-title'), 'New Title');

      expect(screen.queryByTestId('error-title')).not.toBeInTheDocument();
    });
  });
});
