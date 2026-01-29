/**
 * In-Memory Posts Store
 * In a real application, this would be replaced with a database
 */

import { Post, CreatePostInput, UpdatePostInput } from '@/types/post';
import { generateSlug, generateExcerpt } from './validation';

// In-memory store for posts
let posts: Map<string, Post> = new Map();
let idCounter = 1;

// Initialize with some sample data
function initializeSampleData(): void {
  if (posts.size === 0) {
    const samplePosts: CreatePostInput[] = [
      {
        title: 'Getting Started with Next.js 14',
        content: 'Next.js 14 introduces several exciting features including Server Components, improved routing, and better performance. In this post, we will explore these features and how to use them effectively in your projects.',
        author: 'John Doe',
        published: true,
      },
      {
        title: 'Understanding React Server Components',
        content: 'React Server Components allow you to render components on the server, reducing the JavaScript bundle size sent to the client. This results in faster page loads and better user experience.',
        author: 'Jane Smith',
        published: true,
      },
      {
        title: 'Draft: Upcoming Features',
        content: 'This is a draft post about upcoming features we are planning to release. Stay tuned for more information and detailed announcements.',
        author: 'Admin',
        published: false,
      },
    ];

    samplePosts.forEach((post) => createPost(post));
  }
}

export function generateId(): string {
  return `post_${idCounter++}_${Date.now()}`;
}

export function createPost(input: CreatePostInput): Post {
  const now = new Date().toISOString();
  const id = generateId();
  const slug = generateSlug(input.title);
  const excerpt = input.excerpt || generateExcerpt(input.content);

  const post: Post = {
    id,
    title: input.title.trim(),
    content: input.content.trim(),
    excerpt,
    author: input.author.trim(),
    slug,
    published: input.published ?? false,
    createdAt: now,
    updatedAt: now,
  };

  posts.set(id, post);
  return post;
}

export function getPostById(id: string): Post | null {
  return posts.get(id) || null;
}

export function getPostBySlug(slug: string): Post | null {
  for (const post of posts.values()) {
    if (post.slug === slug) {
      return post;
    }
  }
  return null;
}

export function getAllPosts(options?: {
  page?: number;
  limit?: number;
  publishedOnly?: boolean;
}): { posts: Post[]; total: number; page: number; limit: number } {
  const page = options?.page || 1;
  const limit = options?.limit || 10;
  const publishedOnly = options?.publishedOnly ?? false;

  let allPosts = Array.from(posts.values());

  if (publishedOnly) {
    allPosts = allPosts.filter((post) => post.published);
  }

  // Sort by createdAt descending (newest first)
  allPosts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

  const total = allPosts.length;
  const startIndex = (page - 1) * limit;
  const paginatedPosts = allPosts.slice(startIndex, startIndex + limit);

  return {
    posts: paginatedPosts,
    total,
    page,
    limit,
  };
}

export function updatePost(id: string, input: UpdatePostInput): Post | null {
  const existingPost = posts.get(id);
  if (!existingPost) {
    return null;
  }

  const updatedPost: Post = {
    ...existingPost,
    title: input.title !== undefined ? input.title.trim() : existingPost.title,
    content: input.content !== undefined ? input.content.trim() : existingPost.content,
    excerpt: input.excerpt !== undefined ? input.excerpt.trim() : existingPost.excerpt,
    author: input.author !== undefined ? input.author.trim() : existingPost.author,
    published: input.published !== undefined ? input.published : existingPost.published,
    updatedAt: new Date().toISOString(),
  };

  // Update slug if title changed
  if (input.title !== undefined) {
    updatedPost.slug = generateSlug(input.title);
  }

  // Update excerpt if content changed and no explicit excerpt provided
  if (input.content !== undefined && input.excerpt === undefined) {
    updatedPost.excerpt = generateExcerpt(input.content);
  }

  posts.set(id, updatedPost);
  return updatedPost;
}

export function deletePost(id: string): boolean {
  return posts.delete(id);
}

export function clearAllPosts(): void {
  posts.clear();
  idCounter = 1;
}

export function getPostsCount(): number {
  return posts.size;
}

// Initialize sample data on module load
initializeSampleData();

// Export for testing purposes
export function resetStore(): void {
  posts = new Map();
  idCounter = 1;
}
