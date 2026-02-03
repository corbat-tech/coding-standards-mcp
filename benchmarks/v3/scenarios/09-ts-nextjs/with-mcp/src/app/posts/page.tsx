import { getAllPosts } from '@/lib/posts';
import Link from 'next/link';

export default async function PostsPage() {
  const posts = await getAllPosts();

  return (
    <main>
      <h1>Blog Posts</h1>
      <Link href="/posts/new">Create New Post</Link>
      {posts.length === 0 ? (
        <p>No posts yet.</p>
      ) : (
        <ul>
          {posts.map((post) => (
            <li key={post.id}>
              <Link href={`/posts/${post.id}`}>
                <h2>{post.title}</h2>
              </Link>
              <p>By {post.author}</p>
              <p>{post.published ? 'Published' : 'Draft'}</p>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
