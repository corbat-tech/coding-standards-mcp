/**
 * API Client for Posts
 * Used by client components to interact with the API
 */

import { Post, CreatePostInput, UpdatePostInput, ApiResponse, PostListResponse } from '@/types/post';

const API_BASE_URL = '/api/posts';

export class PostApiClient {
  /**
   * Fetch all posts with pagination
   */
  static async getPosts(options?: {
    page?: number;
    limit?: number;
    publishedOnly?: boolean;
  }): Promise<ApiResponse<PostListResponse>> {
    const params = new URLSearchParams();
    if (options?.page) params.set('page', options.page.toString());
    if (options?.limit) params.set('limit', options.limit.toString());
    if (options?.publishedOnly) params.set('published', 'true');

    const url = params.toString() ? `${API_BASE_URL}?${params}` : API_BASE_URL;

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    return response.json();
  }

  /**
   * Fetch a single post by ID
   */
  static async getPost(id: string): Promise<ApiResponse<Post>> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    return response.json();
  }

  /**
   * Create a new post
   */
  static async createPost(input: CreatePostInput): Promise<ApiResponse<Post>> {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(input),
    });

    return response.json();
  }

  /**
   * Update an existing post
   */
  static async updatePost(id: string, input: UpdatePostInput): Promise<ApiResponse<Post>> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(input),
    });

    return response.json();
  }

  /**
   * Delete a post
   */
  static async deletePost(id: string): Promise<ApiResponse<{ deleted: boolean }>> {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    return response.json();
  }
}
