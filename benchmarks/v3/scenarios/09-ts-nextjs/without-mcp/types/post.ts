/**
 * Blog Post Types
 */

export interface Post {
  id: string;
  title: string;
  content: string;
  excerpt: string;
  author: string;
  slug: string;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePostInput {
  title: string;
  content: string;
  excerpt?: string;
  author: string;
  published?: boolean;
}

export interface UpdatePostInput {
  title?: string;
  content?: string;
  excerpt?: string;
  author?: string;
  published?: boolean;
}

export interface PostValidationErrors {
  title?: string;
  content?: string;
  author?: string;
  excerpt?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  errors?: PostValidationErrors;
}

export interface PostListResponse {
  posts: Post[];
  total: number;
  page: number;
  limit: number;
}
