/**
 * API Route Tests
 * Tests HTTP endpoints for post CRUD operations
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { NextRequest } from 'next/server';
import { GET, POST } from '../app/api/posts/route';
import {
  GET as GET_BY_ID,
  PUT,
  DELETE,
} from '../app/api/posts/[id]/route';
import { getRepositoryInstance } from '../lib/repository';
import { PostStatus } from '../types';

// Mock Next.js server components
vi.mock('next/server', async () => {
  const actual = await vi.importActual('next/server');
  return {
    ...actual,
    NextResponse: {
      json: (data: unknown, init?: ResponseInit) => {
        return {
          json: () => Promise.resolve(data),
          status: init?.status ?? 200,
          _data: data,
        };
      },
    },
  };
});

describe('Posts API Routes', () => {
  beforeEach(async () => {
    const repo = getRepositoryInstance();
    await repo.clear();
  });

  describe('GET /api/posts', () => {
    it('should return empty array when no posts exist', async () => {
      const response = await GET();
      const data = await response.json();

      expect(data.success).toBe(true);
      expect(data.data).toEqual([]);
    });

    it('should return all posts', async () => {
      const repo = getRepositoryInstance();
      await repo.create({
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      });

      const response = await GET();
      const data = await response.json();

      expect(data.success).toBe(true);
      expect(data.data).toHaveLength(1);
      expect(data.data[0].title).toBe('Test Post');
    });
  });

  describe('POST /api/posts', () => {
    it('should create a new post', async () => {
      const request = new NextRequest('http://localhost/api/posts', {
        method: 'POST',
        body: JSON.stringify({
          title: 'New Post',
          content: 'This is the post content here.',
          author: 'Test Author',
        }),
      });

      const response = await POST(request);
      const data = await response.json();

      expect(data.success).toBe(true);
      expect(data.data.title).toBe('New Post');
      expect(data.data.id).toBeDefined();
      expect(response.status).toBe(201);
    });

    it('should return 400 for invalid data', async () => {
      const request = new NextRequest('http://localhost/api/posts', {
        method: 'POST',
        body: JSON.stringify({
          title: '',
          content: '',
          author: '',
        }),
      });

      const response = await POST(request);
      const data = await response.json();

      expect(data.success).toBe(false);
      expect(data.error.code).toBe('VALIDATION_FAILED');
      expect(response.status).toBe(400);
    });

    it('should return validation errors in response', async () => {
      const request = new NextRequest('http://localhost/api/posts', {
        method: 'POST',
        body: JSON.stringify({
          title: 'AB',
          content: 'Short',
          author: 'A',
        }),
      });

      const response = await POST(request);
      const data = await response.json();

      expect(data.success).toBe(false);
      expect(data.error.details).toBeDefined();
      expect(data.error.details.length).toBeGreaterThan(0);
    });
  });

  describe('GET /api/posts/[id]', () => {
    it('should return post by id', async () => {
      const repo = getRepositoryInstance();
      const created = await repo.create({
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      });

      const request = new NextRequest(`http://localhost/api/posts/${created.id}`);
      const response = await GET_BY_ID(request, {
        params: Promise.resolve({ id: created.id }),
      });
      const data = await response.json();

      expect(data.success).toBe(true);
      expect(data.data.id).toBe(created.id);
      expect(data.data.title).toBe('Test Post');
    });

    it('should return 404 for non-existent post', async () => {
      const request = new NextRequest('http://localhost/api/posts/nonexistent');
      const response = await GET_BY_ID(request, {
        params: Promise.resolve({ id: 'nonexistent' }),
      });
      const data = await response.json();

      expect(data.success).toBe(false);
      expect(data.error.code).toBe('POST_NOT_FOUND');
      expect(response.status).toBe(404);
    });
  });

  describe('PUT /api/posts/[id]', () => {
    it('should update existing post', async () => {
      const repo = getRepositoryInstance();
      const created = await repo.create({
        title: 'Original Title',
        content: 'Original content',
        author: 'Author',
      });

      const request = new NextRequest(`http://localhost/api/posts/${created.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: 'Updated Title',
        }),
      });

      const response = await PUT(request, {
        params: Promise.resolve({ id: created.id }),
      });
      const data = await response.json();

      expect(data.success).toBe(true);
      expect(data.data.title).toBe('Updated Title');
      expect(data.data.content).toBe('Original content');
    });

    it('should update post status', async () => {
      const repo = getRepositoryInstance();
      const created = await repo.create({
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      });

      const request = new NextRequest(`http://localhost/api/posts/${created.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          status: PostStatus.PUBLISHED,
        }),
      });

      const response = await PUT(request, {
        params: Promise.resolve({ id: created.id }),
      });
      const data = await response.json();

      expect(data.success).toBe(true);
      expect(data.data.status).toBe(PostStatus.PUBLISHED);
    });

    it('should return 404 for non-existent post', async () => {
      const request = new NextRequest('http://localhost/api/posts/nonexistent', {
        method: 'PUT',
        body: JSON.stringify({
          title: 'Updated',
        }),
      });

      const response = await PUT(request, {
        params: Promise.resolve({ id: 'nonexistent' }),
      });
      const data = await response.json();

      expect(data.success).toBe(false);
      expect(data.error.code).toBe('POST_NOT_FOUND');
      expect(response.status).toBe(404);
    });

    it('should return 400 for invalid update data', async () => {
      const repo = getRepositoryInstance();
      const created = await repo.create({
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      });

      const request = new NextRequest(`http://localhost/api/posts/${created.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          title: 'AB',
        }),
      });

      const response = await PUT(request, {
        params: Promise.resolve({ id: created.id }),
      });
      const data = await response.json();

      expect(data.success).toBe(false);
      expect(data.error.code).toBe('VALIDATION_FAILED');
      expect(response.status).toBe(400);
    });
  });

  describe('DELETE /api/posts/[id]', () => {
    it('should delete existing post', async () => {
      const repo = getRepositoryInstance();
      const created = await repo.create({
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      });

      const request = new NextRequest(`http://localhost/api/posts/${created.id}`, {
        method: 'DELETE',
      });

      const response = await DELETE(request, {
        params: Promise.resolve({ id: created.id }),
      });
      const data = await response.json();

      expect(data.success).toBe(true);

      // Verify post is deleted
      const found = await repo.findById(created.id);
      expect(found).toBeNull();
    });

    it('should return 404 for non-existent post', async () => {
      const request = new NextRequest('http://localhost/api/posts/nonexistent', {
        method: 'DELETE',
      });

      const response = await DELETE(request, {
        params: Promise.resolve({ id: 'nonexistent' }),
      });
      const data = await response.json();

      expect(data.success).toBe(false);
      expect(data.error.code).toBe('POST_NOT_FOUND');
      expect(response.status).toBe(404);
    });
  });
});
