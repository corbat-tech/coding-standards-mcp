/**
 * Post Repository Tests
 * Tests data access layer for CRUD operations
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { InMemoryPostRepository } from '../lib/repository';
import { PostStatus } from '../types';
import type { CreatePostRequest, UpdatePostRequest } from '../types';

describe('InMemoryPostRepository', () => {
  let repository: InMemoryPostRepository;

  beforeEach(async () => {
    repository = new InMemoryPostRepository();
    await repository.clear();
  });

  describe('create', () => {
    it('should create a post with generated id', async () => {
      const request: CreatePostRequest = {
        title: 'Test Post',
        content: 'Test content for the post',
        author: 'Test Author',
      };

      const post = await repository.create(request);

      expect(post.id).toBeDefined();
      expect(post.id).toMatch(/^post_/);
      expect(post.title).toBe('Test Post');
      expect(post.content).toBe('Test content for the post');
      expect(post.author).toBe('Test Author');
      expect(post.status).toBe(PostStatus.DRAFT);
    });

    it('should create a post with specified status', async () => {
      const request: CreatePostRequest = {
        title: 'Published Post',
        content: 'Published content',
        author: 'Author',
        status: PostStatus.PUBLISHED,
      };

      const post = await repository.create(request);

      expect(post.status).toBe(PostStatus.PUBLISHED);
    });

    it('should set createdAt and updatedAt timestamps', async () => {
      const request: CreatePostRequest = {
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      };

      const before = new Date();
      const post = await repository.create(request);
      const after = new Date();

      expect(post.createdAt.getTime()).toBeGreaterThanOrEqual(before.getTime());
      expect(post.createdAt.getTime()).toBeLessThanOrEqual(after.getTime());
      expect(post.updatedAt).toEqual(post.createdAt);
    });

    it('should trim whitespace from fields', async () => {
      const request: CreatePostRequest = {
        title: '  Test Post  ',
        content: '  Test content  ',
        author: '  Author  ',
      };

      const post = await repository.create(request);

      expect(post.title).toBe('Test Post');
      expect(post.content).toBe('Test content');
      expect(post.author).toBe('Author');
    });
  });

  describe('findAll', () => {
    it('should return empty array when no posts exist', async () => {
      const posts = await repository.findAll();

      expect(posts).toEqual([]);
    });

    it('should return all posts', async () => {
      await repository.create({
        title: 'Post 1',
        content: 'Content 1',
        author: 'Author 1',
      });
      await repository.create({
        title: 'Post 2',
        content: 'Content 2',
        author: 'Author 2',
      });

      const posts = await repository.findAll();

      expect(posts).toHaveLength(2);
    });

    it('should return posts sorted by createdAt descending', async () => {
      const post1 = await repository.create({
        title: 'First Post',
        content: 'Content 1',
        author: 'Author',
      });

      // Small delay to ensure different timestamps
      await new Promise((resolve) => setTimeout(resolve, 10));

      const post2 = await repository.create({
        title: 'Second Post',
        content: 'Content 2',
        author: 'Author',
      });

      const posts = await repository.findAll();

      expect(posts[0].id).toBe(post2.id);
      expect(posts[1].id).toBe(post1.id);
    });
  });

  describe('findById', () => {
    it('should return post when found', async () => {
      const created = await repository.create({
        title: 'Test Post',
        content: 'Test content',
        author: 'Author',
      });

      const found = await repository.findById(created.id);

      expect(found).not.toBeNull();
      expect(found?.id).toBe(created.id);
      expect(found?.title).toBe('Test Post');
    });

    it('should return null when post not found', async () => {
      const found = await repository.findById('nonexistent-id');

      expect(found).toBeNull();
    });
  });

  describe('update', () => {
    it('should update post title', async () => {
      const created = await repository.create({
        title: 'Original Title',
        content: 'Content',
        author: 'Author',
      });

      const request: UpdatePostRequest = {
        title: 'Updated Title',
      };

      const updated = await repository.update(created.id, request);

      expect(updated).not.toBeNull();
      expect(updated?.title).toBe('Updated Title');
      expect(updated?.content).toBe('Content');
    });

    it('should update post content', async () => {
      const created = await repository.create({
        title: 'Title',
        content: 'Original Content',
        author: 'Author',
      });

      const request: UpdatePostRequest = {
        content: 'Updated Content',
      };

      const updated = await repository.update(created.id, request);

      expect(updated).not.toBeNull();
      expect(updated?.content).toBe('Updated Content');
      expect(updated?.title).toBe('Title');
    });

    it('should update post status', async () => {
      const created = await repository.create({
        title: 'Title',
        content: 'Content',
        author: 'Author',
      });

      const request: UpdatePostRequest = {
        status: PostStatus.PUBLISHED,
      };

      const updated = await repository.update(created.id, request);

      expect(updated).not.toBeNull();
      expect(updated?.status).toBe(PostStatus.PUBLISHED);
    });

    it('should update updatedAt timestamp', async () => {
      const created = await repository.create({
        title: 'Title',
        content: 'Content',
        author: 'Author',
      });

      await new Promise((resolve) => setTimeout(resolve, 10));

      const updated = await repository.update(created.id, { title: 'New' });

      expect(updated?.updatedAt.getTime()).toBeGreaterThan(
        created.updatedAt.getTime(),
      );
    });

    it('should return null when post not found', async () => {
      const updated = await repository.update('nonexistent', { title: 'New' });

      expect(updated).toBeNull();
    });
  });

  describe('delete', () => {
    it('should delete existing post', async () => {
      const created = await repository.create({
        title: 'Title',
        content: 'Content',
        author: 'Author',
      });

      const deleted = await repository.delete(created.id);

      expect(deleted).toBe(true);

      const found = await repository.findById(created.id);
      expect(found).toBeNull();
    });

    it('should return false when post not found', async () => {
      const deleted = await repository.delete('nonexistent');

      expect(deleted).toBe(false);
    });
  });

  describe('clear', () => {
    it('should remove all posts', async () => {
      await repository.create({
        title: 'Post 1',
        content: 'Content 1',
        author: 'Author',
      });
      await repository.create({
        title: 'Post 2',
        content: 'Content 2',
        author: 'Author',
      });

      await repository.clear();

      const posts = await repository.findAll();
      expect(posts).toHaveLength(0);
    });
  });
});
