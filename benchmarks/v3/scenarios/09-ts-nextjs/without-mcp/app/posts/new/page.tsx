/**
 * New Post Page - Server Component wrapper for Client Component
 * Renders the PostEditor in create mode
 */

import PostEditor from '@/components/PostEditor';

export const metadata = {
  title: 'Create New Post',
  description: 'Create a new blog post',
};

export default function NewPostPage() {
  return (
    <main className="new-post-page">
      <PostEditor mode="create" />
    </main>
  );
}
