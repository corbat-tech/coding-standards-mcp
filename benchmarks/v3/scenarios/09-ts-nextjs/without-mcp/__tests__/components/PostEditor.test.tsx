/**
 * PostEditor Component Tests
 * Tests for the Client Component that handles post creation and editing
 */

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import PostEditor, { validateField, validateForm } from '@/components/PostEditor';
import { PostApiClient } from '@/lib/api-client';
import { Post } from '@/types/post';

// Mock next/navigation
const mockPush = jest.fn();
const mockRefresh = jest.fn();

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    refresh: mockRefresh,
  }),
}));

// Mock the API client
jest.mock('@/lib/api-client', () => ({
  PostApiClient: {
    createPost: jest.fn(),
    updatePost: jest.fn(),
  },
}));

const mockPost: Post = {
  id: 'test-id-1',
  title: 'Existing Post Title',
  content: 'This is the existing content of the post with enough characters.',
  excerpt: 'This is the excerpt.',
  author: 'John Doe',
  slug: 'existing-post-title',
  published: false,
  createdAt: '2024-01-15T10:00:00.000Z',
  updatedAt: '2024-01-15T10:00:00.000Z',
};

describe('PostEditor Component - Create Mode', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render form with empty fields in create mode', () => {
    render(<PostEditor mode="create" />);

    expect(screen.getByText('Create New Post')).toBeInTheDocument();
    expect(screen.getByTestId('title-input')).toHaveValue('');
    expect(screen.getByTestId('author-input')).toHaveValue('');
    expect(screen.getByTestId('content-input')).toHaveValue('');
    expect(screen.getByTestId('excerpt-input')).toHaveValue('');
    expect(screen.getByTestId('published-input')).not.toBeChecked();
  });

  it('should render submit button with correct text', () => {
    render(<PostEditor mode="create" />);

    expect(screen.getByTestId('submit-button')).toHaveTextContent('Create Post');
  });

  it('should show validation error for empty title on blur', async () => {
    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    const titleInput = screen.getByTestId('title-input');
    await user.click(titleInput);
    await user.tab(); // Blur

    expect(await screen.findByTestId('title-error')).toHaveTextContent('Title is required');
  });

  it('should show validation error for short title', async () => {
    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    const titleInput = screen.getByTestId('title-input');
    await user.type(titleInput, 'AB');
    await user.tab();

    expect(await screen.findByTestId('title-error')).toHaveTextContent(
      'Title must be at least 3 characters'
    );
  });

  it('should clear validation error when user starts typing', async () => {
    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    const titleInput = screen.getByTestId('title-input');
    await user.click(titleInput);
    await user.tab();

    expect(await screen.findByTestId('title-error')).toBeInTheDocument();

    await user.type(titleInput, 'Valid Title');

    expect(screen.queryByTestId('title-error')).not.toBeInTheDocument();
  });

  it('should show character count for title', () => {
    render(<PostEditor mode="create" />);

    expect(screen.getByText('0/200 characters')).toBeInTheDocument();
  });

  it('should update character count as user types', async () => {
    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    const titleInput = screen.getByTestId('title-input');
    await user.type(titleInput, 'Hello');

    expect(screen.getByText('5/200 characters')).toBeInTheDocument();
  });

  it('should call API and redirect on successful create', async () => {
    const mockCreatedPost = { ...mockPost, id: 'new-post-id' };
    (PostApiClient.createPost as jest.Mock).mockResolvedValue({
      success: true,
      data: mockCreatedPost,
    });

    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    await user.type(screen.getByTestId('title-input'), 'New Post Title');
    await user.type(screen.getByTestId('author-input'), 'Test Author');
    await user.type(
      screen.getByTestId('content-input'),
      'This is the content with enough characters for validation.'
    );
    await user.click(screen.getByTestId('submit-button'));

    await waitFor(() => {
      expect(PostApiClient.createPost).toHaveBeenCalledWith({
        title: 'New Post Title',
        content: 'This is the content with enough characters for validation.',
        excerpt: undefined,
        author: 'Test Author',
        published: false,
      });
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith('/posts/new-post-id');
    });
  });

  it('should show API error on failed create', async () => {
    (PostApiClient.createPost as jest.Mock).mockResolvedValue({
      success: false,
      error: 'Failed to create post',
    });

    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    await user.type(screen.getByTestId('title-input'), 'New Post Title');
    await user.type(screen.getByTestId('author-input'), 'Test Author');
    await user.type(
      screen.getByTestId('content-input'),
      'This is the content with enough characters for validation.'
    );
    await user.click(screen.getByTestId('submit-button'));

    expect(await screen.findByTestId('api-error')).toHaveTextContent('Failed to create post');
  });

  it('should disable submit button while submitting', async () => {
    (PostApiClient.createPost as jest.Mock).mockImplementation(
      () => new Promise((resolve) => setTimeout(resolve, 100))
    );

    render(<PostEditor mode="create" />);
    const user = userEvent.setup();

    await user.type(screen.getByTestId('title-input'), 'New Post Title');
    await user.type(screen.getByTestId('author-input'), 'Test Author');
    await user.type(
      screen.getByTestId('content-input'),
      'This is the content with enough characters for validation.'
    );

    const submitButton = screen.getByTestId('submit-button');
    await user.click(submitButton);

    expect(submitButton).toHaveTextContent('Saving...');
    expect(submitButton).toBeDisabled();
  });
});

describe('PostEditor Component - Edit Mode', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render form with existing post data', () => {
    render(<PostEditor mode="edit" post={mockPost} />);

    expect(screen.getByText('Edit Post')).toBeInTheDocument();
    expect(screen.getByTestId('title-input')).toHaveValue('Existing Post Title');
    expect(screen.getByTestId('author-input')).toHaveValue('John Doe');
    expect(screen.getByTestId('content-input')).toHaveValue(
      'This is the existing content of the post with enough characters.'
    );
    expect(screen.getByTestId('excerpt-input')).toHaveValue('This is the excerpt.');
    expect(screen.getByTestId('published-input')).not.toBeChecked();
  });

  it('should render submit button with correct text', () => {
    render(<PostEditor mode="edit" post={mockPost} />);

    expect(screen.getByTestId('submit-button')).toHaveTextContent('Save Changes');
  });

  it('should call API with only changed fields', async () => {
    (PostApiClient.updatePost as jest.Mock).mockResolvedValue({
      success: true,
      data: { ...mockPost, title: 'Updated Title' },
    });

    render(<PostEditor mode="edit" post={mockPost} />);
    const user = userEvent.setup();

    const titleInput = screen.getByTestId('title-input');
    await user.clear(titleInput);
    await user.type(titleInput, 'Updated Title');
    await user.click(screen.getByTestId('submit-button'));

    await waitFor(() => {
      expect(PostApiClient.updatePost).toHaveBeenCalledWith('test-id-1', {
        title: 'Updated Title',
      });
    });
  });

  it('should show error when no changes made', async () => {
    render(<PostEditor mode="edit" post={mockPost} />);
    const user = userEvent.setup();

    await user.click(screen.getByTestId('submit-button'));

    expect(await screen.findByTestId('api-error')).toHaveTextContent('No changes to save');
  });

  it('should navigate back to post on cancel', async () => {
    render(<PostEditor mode="edit" post={mockPost} />);
    const user = userEvent.setup();

    await user.click(screen.getByTestId('cancel-button'));

    expect(mockPush).toHaveBeenCalledWith('/posts/test-id-1');
  });
});

describe('validateField utility', () => {
  it('should return error for empty title', () => {
    expect(validateField('title', '')).toBe('Title is required');
    expect(validateField('title', '   ')).toBe('Title is required');
  });

  it('should return error for short title', () => {
    expect(validateField('title', 'AB')).toBe('Title must be at least 3 characters');
  });

  it('should return undefined for valid title', () => {
    expect(validateField('title', 'Valid Title')).toBeUndefined();
  });

  it('should return error for empty content', () => {
    expect(validateField('content', '')).toBe('Content is required');
  });

  it('should return error for short content', () => {
    expect(validateField('content', 'Short')).toBe('Content must be at least 10 characters');
  });

  it('should return error for empty author', () => {
    expect(validateField('author', '')).toBe('Author is required');
  });

  it('should return error for short author name', () => {
    expect(validateField('author', 'A')).toBe('Author name must be at least 2 characters');
  });
});

describe('validateForm utility', () => {
  it('should return all errors for completely empty form', () => {
    const errors = validateForm({
      title: '',
      content: '',
      excerpt: '',
      author: '',
      published: false,
    });

    expect(errors.title).toBe('Title is required');
    expect(errors.content).toBe('Content is required');
    expect(errors.author).toBe('Author is required');
    expect(errors.excerpt).toBeUndefined();
  });

  it('should return empty object for valid form', () => {
    const errors = validateForm({
      title: 'Valid Title',
      content: 'Valid content with enough characters',
      excerpt: 'Optional excerpt',
      author: 'Valid Author',
      published: true,
    });

    expect(Object.keys(errors)).toHaveLength(0);
  });
});
