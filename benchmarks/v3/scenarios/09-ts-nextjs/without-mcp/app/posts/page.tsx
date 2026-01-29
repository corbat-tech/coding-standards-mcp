/**
 * Posts List Page - Server Component
 * Displays all blog posts with pagination
 */

import PostList from '@/components/PostList';

interface PostsPageProps {
  searchParams: Promise<{ page?: string; published?: string }>;
}

export const metadata = {
  title: 'Blog Posts',
  description: 'Browse all blog posts',
};

export default async function PostsPage({ searchParams }: PostsPageProps) {
  const params = await searchParams;
  const page = parseInt(params.page || '1', 10);
  const publishedOnly = params.published === 'true';

  return (
    <main className="posts-page">
      <PostList page={page} limit={10} publishedOnly={publishedOnly} />
    </main>
  );
}
