/**
 * Post Validator Tests
 * Tests validation logic for post creation and updates
 */

import { describe, it, expect } from 'vitest';
import { PostValidatorImpl } from '../lib/validator';
import type { CreatePostRequest, UpdatePostRequest } from '../types';

describe('PostValidatorImpl', () => {
  const validator = new PostValidatorImpl();

  describe('validateCreate', () => {
    it('should pass validation with valid data', () => {
      const request: CreatePostRequest = {
        title: 'Valid Title',
        content: 'This is valid content that is long enough.',
        author: 'John Doe',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should fail when title is empty', () => {
      const request: CreatePostRequest = {
        title: '',
        content: 'This is valid content that is long enough.',
        author: 'John Doe',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'title',
        message: 'Title is required',
      });
    });

    it('should fail when title is too short', () => {
      const request: CreatePostRequest = {
        title: 'AB',
        content: 'This is valid content that is long enough.',
        author: 'John Doe',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'title',
        message: 'Title must be at least 3 characters',
      });
    });

    it('should fail when title exceeds max length', () => {
      const request: CreatePostRequest = {
        title: 'A'.repeat(201),
        content: 'This is valid content that is long enough.',
        author: 'John Doe',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'title',
        message: 'Title must not exceed 200 characters',
      });
    });

    it('should fail when content is empty', () => {
      const request: CreatePostRequest = {
        title: 'Valid Title',
        content: '',
        author: 'John Doe',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'content',
        message: 'Content is required',
      });
    });

    it('should fail when content is too short', () => {
      const request: CreatePostRequest = {
        title: 'Valid Title',
        content: 'Short',
        author: 'John Doe',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'content',
        message: 'Content must be at least 10 characters',
      });
    });

    it('should fail when author is empty', () => {
      const request: CreatePostRequest = {
        title: 'Valid Title',
        content: 'This is valid content that is long enough.',
        author: '',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'author',
        message: 'Author is required',
      });
    });

    it('should fail when author is too short', () => {
      const request: CreatePostRequest = {
        title: 'Valid Title',
        content: 'This is valid content that is long enough.',
        author: 'A',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'author',
        message: 'Author must be at least 2 characters',
      });
    });

    it('should collect multiple validation errors', () => {
      const request: CreatePostRequest = {
        title: '',
        content: '',
        author: '',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBeGreaterThanOrEqual(3);
    });

    it('should trim whitespace when validating', () => {
      const request: CreatePostRequest = {
        title: '   ',
        content: '   ',
        author: '   ',
      };

      const result = validator.validateCreate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'title',
        message: 'Title is required',
      });
    });
  });

  describe('validateUpdate', () => {
    it('should pass validation with valid partial update', () => {
      const request: UpdatePostRequest = {
        title: 'Updated Title',
      };

      const result = validator.validateUpdate(request);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should pass validation with empty update', () => {
      const request: UpdatePostRequest = {};

      const result = validator.validateUpdate(request);

      expect(result.isValid).toBe(true);
      expect(result.errors).toHaveLength(0);
    });

    it('should fail when updated title is too short', () => {
      const request: UpdatePostRequest = {
        title: 'AB',
      };

      const result = validator.validateUpdate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'title',
        message: 'Title must be at least 3 characters',
      });
    });

    it('should fail when updated content is too short', () => {
      const request: UpdatePostRequest = {
        content: 'Short',
      };

      const result = validator.validateUpdate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors).toContainEqual({
        field: 'content',
        message: 'Content must be at least 10 characters',
      });
    });

    it('should validate all provided fields', () => {
      const request: UpdatePostRequest = {
        title: 'AB',
        content: 'Short',
      };

      const result = validator.validateUpdate(request);

      expect(result.isValid).toBe(false);
      expect(result.errors.length).toBe(2);
    });
  });
});
