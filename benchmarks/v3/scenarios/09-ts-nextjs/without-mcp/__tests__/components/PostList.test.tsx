/**
 * PostList Component Tests
 * Tests for the Server Component that displays blog posts
 */

import { render, screen } from '@testing-library/react';
import { PostCard, EmptyState, Pagination, formatDate } from '@/components/PostList';
import { Post } from '@/types/post';

// Mock next/link
jest.mock('next/link', () => {
  return function MockLink({ children, href }: { children: React.ReactNode; href: string }) {
    return <a href={href}>{children}</a>;
  };
});

const mockPost: Post = {
  id: 'test-id-1',
  title: 'Test Post Title',
  content: 'This is the full content of the test post.',
  excerpt: 'This is the excerpt of the test post.',
  author: 'John Doe',
  slug: 'test-post-title',
  published: true,
  createdAt: '2024-01-15T10:00:00.000Z',
  updatedAt: '2024-01-15T10:00:00.000Z',
};

const mockDraftPost: Post = {
  ...mockPost,
  id: 'test-id-2',
  title: 'Draft Post',
  published: false,
};

describe('PostCard Component', () => {
  it('should render post title', () => {
    render(<PostCard post={mockPost} />);

    expect(screen.getByText('Test Post Title')).toBeInTheDocument();
  });

  it('should render post author', () => {
    render(<PostCard post={mockPost} />);

    expect(screen.getByText('By John Doe')).toBeInTheDocument();
  });

  it('should render post excerpt', () => {
    render(<PostCard post={mockPost} />);

    expect(screen.getByText('This is the excerpt of the test post.')).toBeInTheDocument();
  });

  it('should render formatted date', () => {
    render(<PostCard post={mockPost} />);

    // The date format depends on locale, but should contain key parts
    expect(screen.getByText(/January 15, 2024/)).toBeInTheDocument();
  });

  it('should render link to post detail page', () => {
    render(<PostCard post={mockPost} />);

    const links = screen.getAllByRole('link');
    const postLink = links.find((link) => link.getAttribute('href') === '/posts/test-id-1');
    expect(postLink).toBeInTheDocument();
  });

  it('should render edit link', () => {
    render(<PostCard post={mockPost} />);

    const editLink = screen.getByText('Edit');
    expect(editLink).toHaveAttribute('href', '/posts/test-id-1/edit');
  });

  it('should show draft badge for unpublished posts', () => {
    render(<PostCard post={mockDraftPost} />);

    expect(screen.getByTestId('draft-badge')).toBeInTheDocument();
    expect(screen.getByText('Draft')).toBeInTheDocument();
  });

  it('should not show draft badge for published posts', () => {
    render(<PostCard post={mockPost} />);

    expect(screen.queryByTestId('draft-badge')).not.toBeInTheDocument();
  });
});

describe('EmptyState Component', () => {
  it('should render empty state message', () => {
    render(<EmptyState />);

    expect(screen.getByText('No posts yet')).toBeInTheDocument();
    expect(screen.getByText('Be the first to create a blog post!')).toBeInTheDocument();
  });

  it('should render create post link', () => {
    render(<EmptyState />);

    const createLink = screen.getByText('Create your first post');
    expect(createLink).toHaveAttribute('href', '/posts/new');
  });
});

describe('Pagination Component', () => {
  it('should not render when totalPages is 1', () => {
    const { container } = render(
      <Pagination currentPage={1} totalPages={1} baseUrl="/posts" />
    );

    expect(container.firstChild).toBeNull();
  });

  it('should render page links for multiple pages', () => {
    render(<Pagination currentPage={1} totalPages={3} baseUrl="/posts" />);

    expect(screen.getByText('1')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('should not render previous link on first page', () => {
    render(<Pagination currentPage={1} totalPages={3} baseUrl="/posts" />);

    expect(screen.queryByText('Previous')).not.toBeInTheDocument();
    expect(screen.getByText('Next')).toBeInTheDocument();
  });

  it('should not render next link on last page', () => {
    render(<Pagination currentPage={3} totalPages={3} baseUrl="/posts" />);

    expect(screen.getByText('Previous')).toBeInTheDocument();
    expect(screen.queryByText('Next')).not.toBeInTheDocument();
  });

  it('should render both previous and next links on middle pages', () => {
    render(<Pagination currentPage={2} totalPages={3} baseUrl="/posts" />);

    expect(screen.getByText('Previous')).toBeInTheDocument();
    expect(screen.getByText('Next')).toBeInTheDocument();
  });

  it('should have correct href for pagination links', () => {
    render(<Pagination currentPage={2} totalPages={3} baseUrl="/posts" />);

    const prevLink = screen.getByText('Previous');
    const nextLink = screen.getByText('Next');

    expect(prevLink).toHaveAttribute('href', '/posts?page=1');
    expect(nextLink).toHaveAttribute('href', '/posts?page=3');
  });

  it('should mark current page with aria-current', () => {
    render(<Pagination currentPage={2} totalPages={3} baseUrl="/posts" />);

    const currentPageLink = screen.getByText('2');
    expect(currentPageLink).toHaveAttribute('aria-current', 'page');
  });
});

describe('formatDate utility', () => {
  it('should format date correctly', () => {
    const dateString = '2024-01-15T10:30:00.000Z';
    const formatted = formatDate(dateString);

    // Check for key parts of the formatted date
    expect(formatted).toContain('January');
    expect(formatted).toContain('15');
    expect(formatted).toContain('2024');
  });

  it('should handle different date formats', () => {
    const dateString = '2023-12-25T00:00:00.000Z';
    const formatted = formatDate(dateString);

    expect(formatted).toContain('December');
    expect(formatted).toContain('25');
    expect(formatted).toContain('2023');
  });
});
