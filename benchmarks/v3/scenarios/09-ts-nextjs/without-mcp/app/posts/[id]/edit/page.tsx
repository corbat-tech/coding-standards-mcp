/**
 * Edit Post Page - Server Component wrapper for Client Component
 * Renders the PostEditor in edit mode with the existing post data
 */

import { notFound } from 'next/navigation';
import PostEditor from '@/components/PostEditor';
import { getPostById } from '@/lib/posts-store';

interface EditPostPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: EditPostPageProps) {
  const { id } = await params;
  const post = getPostById(id);

  if (!post) {
    return {
      title: 'Post Not Found',
    };
  }

  return {
    title: `Edit: ${post.title}`,
    description: `Editing post: ${post.title}`,
  };
}

export default async function EditPostPage({ params }: EditPostPageProps) {
  const { id } = await params;
  const post = getPostById(id);

  if (!post) {
    notFound();
  }

  return (
    <main className="edit-post-page">
      <PostEditor mode="edit" post={post} />
    </main>
  );
}
