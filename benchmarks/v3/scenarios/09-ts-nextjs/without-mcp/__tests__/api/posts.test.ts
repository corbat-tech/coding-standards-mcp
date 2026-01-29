/**
 * API Route Tests for Posts
 * Tests for /api/posts and /api/posts/[id] endpoints
 */

import { NextRequest } from 'next/server';
import { GET, POST } from '@/app/api/posts/route';
import {
  GET as GET_BY_ID,
  PUT,
  DELETE,
} from '@/app/api/posts/[id]/route';
import { resetStore, createPost } from '@/lib/posts-store';
import { Post } from '@/types/post';

// Helper to create a mock NextRequest
function createMockRequest(
  url: string,
  options: {
    method?: string;
    body?: object;
    headers?: Record<string, string>;
  } = {}
): NextRequest {
  const { method = 'GET', body, headers = {} } = options;

  const request = new NextRequest(new URL(url, 'http://localhost:3000'), {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  return request;
}

// Helper to parse response
async function parseResponse<T>(response: Response): Promise<T> {
  return response.json() as Promise<T>;
}

describe('POST /api/posts', () => {
  beforeEach(() => {
    resetStore();
  });

  it('should create a new post with valid data', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts', {
      method: 'POST',
      body: {
        title: 'Test Post Title',
        content: 'This is the content of the test post with enough characters.',
        author: 'Test Author',
        published: true,
      },
    });

    const response = await POST(request);
    const data = await parseResponse<{ success: boolean; data: Post }>(response);

    expect(response.status).toBe(201);
    expect(data.success).toBe(true);
    expect(data.data).toBeDefined();
    expect(data.data.title).toBe('Test Post Title');
    expect(data.data.author).toBe('Test Author');
    expect(data.data.published).toBe(true);
    expect(data.data.id).toBeDefined();
    expect(data.data.slug).toBe('test-post-title');
  });

  it('should return validation error for missing title', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts', {
      method: 'POST',
      body: {
        content: 'This is the content of the test post with enough characters.',
        author: 'Test Author',
      },
    });

    const response = await POST(request);
    const data = await parseResponse<{ success: boolean; error: string; errors: object }>(response);

    expect(response.status).toBe(422);
    expect(data.success).toBe(false);
    expect(data.errors).toHaveProperty('title');
  });

  it('should return validation error for short content', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts', {
      method: 'POST',
      body: {
        title: 'Test Post',
        content: 'Short',
        author: 'Test Author',
      },
    });

    const response = await POST(request);
    const data = await parseResponse<{ success: boolean; errors: object }>(response);

    expect(response.status).toBe(422);
    expect(data.success).toBe(false);
    expect(data.errors).toHaveProperty('content');
  });

  it('should return validation error for missing author', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts', {
      method: 'POST',
      body: {
        title: 'Test Post',
        content: 'This is the content of the test post with enough characters.',
      },
    });

    const response = await POST(request);
    const data = await parseResponse<{ success: boolean; errors: object }>(response);

    expect(response.status).toBe(422);
    expect(data.success).toBe(false);
    expect(data.errors).toHaveProperty('author');
  });

  it('should auto-generate excerpt if not provided', async () => {
    const content = 'This is a very long content that should be auto-truncated to create an excerpt.';
    const request = createMockRequest('http://localhost:3000/api/posts', {
      method: 'POST',
      body: {
        title: 'Test Post',
        content,
        author: 'Test Author',
      },
    });

    const response = await POST(request);
    const data = await parseResponse<{ success: boolean; data: Post }>(response);

    expect(response.status).toBe(201);
    expect(data.data.excerpt).toBeDefined();
    expect(data.data.excerpt.length).toBeLessThanOrEqual(153); // 150 + '...'
  });
});

describe('GET /api/posts', () => {
  beforeEach(() => {
    resetStore();
    // Create some test posts
    createPost({ title: 'Post 1', content: 'Content for post 1 with enough length', author: 'Author 1', published: true });
    createPost({ title: 'Post 2', content: 'Content for post 2 with enough length', author: 'Author 2', published: true });
    createPost({ title: 'Post 3', content: 'Content for post 3 with enough length', author: 'Author 3', published: false });
  });

  it('should return all posts with pagination', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts');

    const response = await GET(request);
    const data = await parseResponse<{ success: boolean; data: { posts: Post[]; total: number } }>(response);

    expect(response.status).toBe(200);
    expect(data.success).toBe(true);
    expect(data.data.posts).toHaveLength(3);
    expect(data.data.total).toBe(3);
  });

  it('should filter published posts only', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts?published=true');

    const response = await GET(request);
    const data = await parseResponse<{ success: boolean; data: { posts: Post[]; total: number } }>(response);

    expect(response.status).toBe(200);
    expect(data.data.posts).toHaveLength(2);
    expect(data.data.posts.every((post) => post.published)).toBe(true);
  });

  it('should handle pagination correctly', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts?page=1&limit=2');

    const response = await GET(request);
    const data = await parseResponse<{ success: boolean; data: { posts: Post[]; total: number; page: number; limit: number } }>(response);

    expect(response.status).toBe(200);
    expect(data.data.posts).toHaveLength(2);
    expect(data.data.total).toBe(3);
    expect(data.data.page).toBe(1);
    expect(data.data.limit).toBe(2);
  });

  it('should return error for invalid page parameter', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts?page=0');

    const response = await GET(request);
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(400);
    expect(data.success).toBe(false);
    expect(data.error).toContain('Page must be a positive integer');
  });

  it('should return error for invalid limit parameter', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts?limit=150');

    const response = await GET(request);
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(400);
    expect(data.success).toBe(false);
    expect(data.error).toContain('Limit must be between 1 and 100');
  });
});

describe('GET /api/posts/[id]', () => {
  let testPost: Post;

  beforeEach(() => {
    resetStore();
    testPost = createPost({
      title: 'Test Post',
      content: 'This is the test post content with enough characters.',
      author: 'Test Author',
      published: true,
    });
  });

  it('should return a single post by ID', async () => {
    const request = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`);

    const response = await GET_BY_ID(request, { params: Promise.resolve({ id: testPost.id }) });
    const data = await parseResponse<{ success: boolean; data: Post }>(response);

    expect(response.status).toBe(200);
    expect(data.success).toBe(true);
    expect(data.data.id).toBe(testPost.id);
    expect(data.data.title).toBe('Test Post');
  });

  it('should return 404 for non-existent post', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts/non-existent-id');

    const response = await GET_BY_ID(request, { params: Promise.resolve({ id: 'non-existent-id' }) });
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(404);
    expect(data.success).toBe(false);
    expect(data.error).toContain('not found');
  });
});

describe('PUT /api/posts/[id]', () => {
  let testPost: Post;

  beforeEach(() => {
    resetStore();
    testPost = createPost({
      title: 'Original Title',
      content: 'Original content with enough characters for validation.',
      author: 'Original Author',
      published: false,
    });
  });

  it('should update a post with valid data', async () => {
    const request = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`, {
      method: 'PUT',
      body: {
        title: 'Updated Title',
        published: true,
      },
    });

    const response = await PUT(request, { params: Promise.resolve({ id: testPost.id }) });
    const data = await parseResponse<{ success: boolean; data: Post }>(response);

    expect(response.status).toBe(200);
    expect(data.success).toBe(true);
    expect(data.data.title).toBe('Updated Title');
    expect(data.data.published).toBe(true);
    expect(data.data.content).toBe('Original content with enough characters for validation.');
  });

  it('should return 404 for non-existent post', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts/non-existent-id', {
      method: 'PUT',
      body: { title: 'New Title' },
    });

    const response = await PUT(request, { params: Promise.resolve({ id: 'non-existent-id' }) });
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(404);
    expect(data.success).toBe(false);
  });

  it('should return validation error for invalid update data', async () => {
    const request = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`, {
      method: 'PUT',
      body: {
        title: 'AB', // Too short
      },
    });

    const response = await PUT(request, { params: Promise.resolve({ id: testPost.id }) });
    const data = await parseResponse<{ success: boolean; errors: object }>(response);

    expect(response.status).toBe(422);
    expect(data.success).toBe(false);
    expect(data.errors).toHaveProperty('title');
  });

  it('should return error when no fields provided', async () => {
    const request = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`, {
      method: 'PUT',
      body: {},
    });

    const response = await PUT(request, { params: Promise.resolve({ id: testPost.id }) });
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(400);
    expect(data.success).toBe(false);
    expect(data.error).toContain('No fields provided');
  });

  it('should update the slug when title changes', async () => {
    const request = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`, {
      method: 'PUT',
      body: {
        title: 'New Amazing Title',
      },
    });

    const response = await PUT(request, { params: Promise.resolve({ id: testPost.id }) });
    const data = await parseResponse<{ success: boolean; data: Post }>(response);

    expect(response.status).toBe(200);
    expect(data.data.slug).toBe('new-amazing-title');
  });
});

describe('DELETE /api/posts/[id]', () => {
  let testPost: Post;

  beforeEach(() => {
    resetStore();
    testPost = createPost({
      title: 'Post to Delete',
      content: 'This post will be deleted with enough content.',
      author: 'Author',
      published: true,
    });
  });

  it('should delete a post', async () => {
    const request = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`, {
      method: 'DELETE',
    });

    const response = await DELETE(request, { params: Promise.resolve({ id: testPost.id }) });
    const data = await parseResponse<{ success: boolean; data: { deleted: boolean } }>(response);

    expect(response.status).toBe(200);
    expect(data.success).toBe(true);
    expect(data.data.deleted).toBe(true);

    // Verify post is deleted
    const getRequest = createMockRequest(`http://localhost:3000/api/posts/${testPost.id}`);
    const getResponse = await GET_BY_ID(getRequest, { params: Promise.resolve({ id: testPost.id }) });
    expect(getResponse.status).toBe(404);
  });

  it('should return 404 for non-existent post', async () => {
    const request = createMockRequest('http://localhost:3000/api/posts/non-existent-id', {
      method: 'DELETE',
    });

    const response = await DELETE(request, { params: Promise.resolve({ id: 'non-existent-id' }) });
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(404);
    expect(data.success).toBe(false);
  });
});

describe('API Error Handling', () => {
  beforeEach(() => {
    resetStore();
  });

  it('should handle malformed JSON in request body', async () => {
    const request = new NextRequest('http://localhost:3000/api/posts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{ invalid json }',
    });

    const response = await POST(request);
    const data = await parseResponse<{ success: boolean; error: string }>(response);

    expect(response.status).toBe(400);
    expect(data.success).toBe(false);
  });
});
