/**
 * Post Not Found Page
 * Displayed when a post is not found
 */

import Link from 'next/link';

export default function PostNotFound() {
  return (
    <main className="not-found-page">
      <div className="not-found-content">
        <h1>Post Not Found</h1>
        <p>The post you are looking for does not exist or has been removed.</p>
        <Link href="/posts" className="btn btn-primary">
          View All Posts
        </Link>
      </div>
    </main>
  );
}
