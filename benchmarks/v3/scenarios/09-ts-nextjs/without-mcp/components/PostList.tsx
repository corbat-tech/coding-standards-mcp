/**
 * PostList - Server Component
 * Displays a list of blog posts with server-side data fetching
 */

import { Post } from '@/types/post';
import { getAllPosts } from '@/lib/posts-store';
import Link from 'next/link';

interface PostListProps {
  page?: number;
  limit?: number;
  publishedOnly?: boolean;
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function PostCard({ post }: { post: Post }) {
  return (
    <article className="post-card" data-testid="post-card">
      <header className="post-card-header">
        <Link href={`/posts/${post.id}`} className="post-card-title-link">
          <h2 className="post-card-title">{post.title}</h2>
        </Link>
        <div className="post-card-meta">
          <span className="post-card-author">By {post.author}</span>
          <span className="post-card-date">{formatDate(post.createdAt)}</span>
          {!post.published && (
            <span className="post-card-draft-badge" data-testid="draft-badge">
              Draft
            </span>
          )}
        </div>
      </header>
      <p className="post-card-excerpt">{post.excerpt}</p>
      <footer className="post-card-footer">
        <Link href={`/posts/${post.id}`} className="post-card-read-more">
          Read more
        </Link>
        <Link href={`/posts/${post.id}/edit`} className="post-card-edit-link">
          Edit
        </Link>
      </footer>
    </article>
  );
}

function EmptyState() {
  return (
    <div className="posts-empty-state" data-testid="empty-state">
      <h3>No posts yet</h3>
      <p>Be the first to create a blog post!</p>
      <Link href="/posts/new" className="create-post-button">
        Create your first post
      </Link>
    </div>
  );
}

function Pagination({
  currentPage,
  totalPages,
  baseUrl,
}: {
  currentPage: number;
  totalPages: number;
  baseUrl: string;
}) {
  if (totalPages <= 1) return null;

  const pages = Array.from({ length: totalPages }, (_, i) => i + 1);

  return (
    <nav className="pagination" aria-label="Posts pagination" data-testid="pagination">
      {currentPage > 1 && (
        <Link
          href={`${baseUrl}?page=${currentPage - 1}`}
          className="pagination-link pagination-prev"
          aria-label="Previous page"
        >
          Previous
        </Link>
      )}
      <div className="pagination-pages">
        {pages.map((page) => (
          <Link
            key={page}
            href={`${baseUrl}?page=${page}`}
            className={`pagination-link ${page === currentPage ? 'pagination-current' : ''}`}
            aria-label={`Page ${page}`}
            aria-current={page === currentPage ? 'page' : undefined}
          >
            {page}
          </Link>
        ))}
      </div>
      {currentPage < totalPages && (
        <Link
          href={`${baseUrl}?page=${currentPage + 1}`}
          className="pagination-link pagination-next"
          aria-label="Next page"
        >
          Next
        </Link>
      )}
    </nav>
  );
}

export default async function PostList({
  page = 1,
  limit = 10,
  publishedOnly = false,
}: PostListProps) {
  // Server-side data fetching
  const { posts, total } = getAllPosts({ page, limit, publishedOnly });
  const totalPages = Math.ceil(total / limit);

  return (
    <section className="post-list" data-testid="post-list">
      <header className="post-list-header">
        <h1 className="post-list-title">Blog Posts</h1>
        <Link href="/posts/new" className="create-post-button">
          New Post
        </Link>
      </header>

      {posts.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          <div className="post-list-count">
            Showing {posts.length} of {total} posts
          </div>
          <div className="post-list-grid">
            {posts.map((post) => (
              <PostCard key={post.id} post={post} />
            ))}
          </div>
          <Pagination currentPage={page} totalPages={totalPages} baseUrl="/posts" />
        </>
      )}
    </section>
  );
}

// Export individual components for testing
export { PostCard, EmptyState, Pagination, formatDate };
