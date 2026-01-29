/**
 * Single Post Page - Server Component
 * Displays a single blog post
 */

import { notFound } from 'next/navigation';
import Link from 'next/link';
import { getPostById } from '@/lib/posts-store';

interface PostPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: PostPageProps) {
  const { id } = await params;
  const post = getPostById(id);

  if (!post) {
    return {
      title: 'Post Not Found',
    };
  }

  return {
    title: post.title,
    description: post.excerpt,
  };
}

function formatDate(dateString: string): string {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default async function PostPage({ params }: PostPageProps) {
  const { id } = await params;
  const post = getPostById(id);

  if (!post) {
    notFound();
  }

  return (
    <main className="post-page">
      <article className="post-article" data-testid="post-article">
        <header className="post-header">
          <Link href="/posts" className="back-link">
            &larr; Back to posts
          </Link>
          <h1 className="post-title">{post.title}</h1>
          <div className="post-meta">
            <span className="post-author">By {post.author}</span>
            <time className="post-date" dateTime={post.createdAt}>
              {formatDate(post.createdAt)}
            </time>
            {!post.published && (
              <span className="post-draft-badge" data-testid="draft-badge">
                Draft
              </span>
            )}
          </div>
          {post.updatedAt !== post.createdAt && (
            <div className="post-updated">
              Last updated: {formatDate(post.updatedAt)}
            </div>
          )}
        </header>

        <div className="post-content" data-testid="post-content">
          {post.content.split('\n').map((paragraph, index) => (
            <p key={index}>{paragraph}</p>
          ))}
        </div>

        <footer className="post-footer">
          <Link href={`/posts/${post.id}/edit`} className="btn btn-primary">
            Edit Post
          </Link>
          <Link href="/posts" className="btn btn-secondary">
            View All Posts
          </Link>
        </footer>
      </article>
    </main>
  );
}
