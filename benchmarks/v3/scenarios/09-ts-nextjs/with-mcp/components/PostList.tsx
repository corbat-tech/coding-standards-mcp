/**
 * PostList Server Component
 * Displays a list of blog posts fetched from the server
 */

import type { Post } from '../types';
import { PostStatus } from '../types';

/** Props for PostList component */
interface PostListProps {
  posts: Post[];
  onSelectPost?: (post: Post) => void;
}

/** Format date for display */
function formatDate(date: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date));
}

/** Get status badge styling */
function getStatusBadge(status: PostStatus): {
  bg: string;
  text: string;
  label: string;
} {
  const statusStyles = {
    [PostStatus.DRAFT]: {
      bg: 'bg-yellow-100',
      text: 'text-yellow-800',
      label: 'Draft',
    },
    [PostStatus.PUBLISHED]: {
      bg: 'bg-green-100',
      text: 'text-green-800',
      label: 'Published',
    },
    [PostStatus.ARCHIVED]: {
      bg: 'bg-gray-100',
      text: 'text-gray-800',
      label: 'Archived',
    },
  };

  return statusStyles[status];
}

/** Empty state component */
function EmptyState(): JSX.Element {
  return (
    <div className="text-center py-12">
      <svg
        className="mx-auto h-12 w-12 text-gray-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        aria-hidden="true"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
        />
      </svg>
      <h3 className="mt-2 text-sm font-semibold text-gray-900">No posts</h3>
      <p className="mt-1 text-sm text-gray-500">
        Get started by creating a new post.
      </p>
    </div>
  );
}

/** Post card component */
function PostCard({
  post,
  onSelect,
}: {
  post: Post;
  onSelect?: (post: Post) => void;
}): JSX.Element {
  const statusBadge = getStatusBadge(post.status);

  return (
    <article
      className="border rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
      onClick={() => onSelect?.(post)}
      data-testid={`post-card-${post.id}`}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <h2 className="text-lg font-semibold text-gray-900 mb-1">
            {post.title}
          </h2>
          <p className="text-sm text-gray-600 line-clamp-2 mb-2">
            {post.content}
          </p>
          <div className="flex items-center gap-4 text-xs text-gray-500">
            <span>By {post.author}</span>
            <span>{formatDate(post.createdAt)}</span>
          </div>
        </div>
        <span
          className={`px-2 py-1 text-xs font-medium rounded-full ${statusBadge.bg} ${statusBadge.text}`}
        >
          {statusBadge.label}
        </span>
      </div>
    </article>
  );
}

/** Main PostList component - Server Component */
export default function PostList({
  posts,
  onSelectPost,
}: PostListProps): JSX.Element {
  if (posts.length === 0) {
    return <EmptyState />;
  }

  return (
    <div className="space-y-4" data-testid="post-list">
      <h1 className="text-2xl font-bold text-gray-900">Blog Posts</h1>
      <div className="grid gap-4">
        {posts.map((post) => (
          <PostCard key={post.id} post={post} onSelect={onSelectPost} />
        ))}
      </div>
    </div>
  );
}
