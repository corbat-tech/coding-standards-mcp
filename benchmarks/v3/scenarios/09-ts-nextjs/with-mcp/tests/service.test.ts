/**
 * Post Service Tests
 * Tests business logic layer with mocked dependencies
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { PostServiceImpl } from '../lib/service';
import { PostNotFoundError, ValidationFailedError } from '../lib/errors';
import { PostStatus } from '../types';
import type {
  Post,
  PostRepository,
  PostValidator,
  CreatePostRequest,
  UpdatePostRequest,
  ValidationResult,
} from '../types';

describe('PostServiceImpl', () => {
  let service: PostServiceImpl;
  let mockRepository: PostRepository;
  let mockValidator: PostValidator;

  const mockPost: Post = {
    id: 'post-1',
    title: 'Test Post',
    content: 'Test content',
    author: 'Test Author',
    status: PostStatus.DRAFT,
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  const validResult: ValidationResult = {
    isValid: true,
    errors: [],
  };

  const invalidResult: ValidationResult = {
    isValid: false,
    errors: [{ field: 'title', message: 'Title is required' }],
  };

  beforeEach(() => {
    mockRepository = {
      findAll: vi.fn(),
      findById: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
      delete: vi.fn(),
    };

    mockValidator = {
      validateCreate: vi.fn(),
      validateUpdate: vi.fn(),
    };

    service = new PostServiceImpl(mockRepository, mockValidator);
  });

  describe('getAllPosts', () => {
    it('should return all posts from repository', async () => {
      const posts = [mockPost, { ...mockPost, id: 'post-2' }];
      vi.mocked(mockRepository.findAll).mockResolvedValue(posts);

      const result = await service.getAllPosts();

      expect(result).toEqual(posts);
      expect(mockRepository.findAll).toHaveBeenCalledTimes(1);
    });

    it('should return empty array when no posts exist', async () => {
      vi.mocked(mockRepository.findAll).mockResolvedValue([]);

      const result = await service.getAllPosts();

      expect(result).toEqual([]);
    });
  });

  describe('getPostById', () => {
    it('should return post when found', async () => {
      vi.mocked(mockRepository.findById).mockResolvedValue(mockPost);

      const result = await service.getPostById('post-1');

      expect(result).toEqual(mockPost);
      expect(mockRepository.findById).toHaveBeenCalledWith('post-1');
    });

    it('should throw PostNotFoundError when post not found', async () => {
      vi.mocked(mockRepository.findById).mockResolvedValue(null);

      await expect(service.getPostById('nonexistent')).rejects.toThrow(
        PostNotFoundError,
      );
    });

    it('should include id in error message', async () => {
      vi.mocked(mockRepository.findById).mockResolvedValue(null);

      await expect(service.getPostById('missing-id')).rejects.toThrow(
        "Post with id 'missing-id' not found",
      );
    });
  });

  describe('createPost', () => {
    const createRequest: CreatePostRequest = {
      title: 'New Post',
      content: 'New content',
      author: 'Author',
    };

    it('should create post when validation passes', async () => {
      vi.mocked(mockValidator.validateCreate).mockReturnValue(validResult);
      vi.mocked(mockRepository.create).mockResolvedValue(mockPost);

      const result = await service.createPost(createRequest);

      expect(result).toEqual(mockPost);
      expect(mockValidator.validateCreate).toHaveBeenCalledWith(createRequest);
      expect(mockRepository.create).toHaveBeenCalledWith(createRequest);
    });

    it('should throw ValidationFailedError when validation fails', async () => {
      vi.mocked(mockValidator.validateCreate).mockReturnValue(invalidResult);

      await expect(service.createPost(createRequest)).rejects.toThrow(
        ValidationFailedError,
      );
    });

    it('should include validation errors in exception', async () => {
      vi.mocked(mockValidator.validateCreate).mockReturnValue(invalidResult);

      try {
        await service.createPost(createRequest);
        expect.fail('Should have thrown');
      } catch (error) {
        expect(error).toBeInstanceOf(ValidationFailedError);
        const validationError = error as ValidationFailedError;
        expect(validationError.errors).toEqual(invalidResult.errors);
      }
    });

    it('should not call repository when validation fails', async () => {
      vi.mocked(mockValidator.validateCreate).mockReturnValue(invalidResult);

      await expect(service.createPost(createRequest)).rejects.toThrow();

      expect(mockRepository.create).not.toHaveBeenCalled();
    });
  });

  describe('updatePost', () => {
    const updateRequest: UpdatePostRequest = {
      title: 'Updated Title',
    };

    it('should update post when validation passes', async () => {
      const updatedPost = { ...mockPost, title: 'Updated Title' };
      vi.mocked(mockValidator.validateUpdate).mockReturnValue(validResult);
      vi.mocked(mockRepository.update).mockResolvedValue(updatedPost);

      const result = await service.updatePost('post-1', updateRequest);

      expect(result).toEqual(updatedPost);
      expect(mockValidator.validateUpdate).toHaveBeenCalledWith(updateRequest);
      expect(mockRepository.update).toHaveBeenCalledWith('post-1', updateRequest);
    });

    it('should throw ValidationFailedError when validation fails', async () => {
      vi.mocked(mockValidator.validateUpdate).mockReturnValue(invalidResult);

      await expect(
        service.updatePost('post-1', updateRequest),
      ).rejects.toThrow(ValidationFailedError);
    });

    it('should throw PostNotFoundError when post not found', async () => {
      vi.mocked(mockValidator.validateUpdate).mockReturnValue(validResult);
      vi.mocked(mockRepository.update).mockResolvedValue(null);

      await expect(
        service.updatePost('nonexistent', updateRequest),
      ).rejects.toThrow(PostNotFoundError);
    });

    it('should not call repository when validation fails', async () => {
      vi.mocked(mockValidator.validateUpdate).mockReturnValue(invalidResult);

      await expect(
        service.updatePost('post-1', updateRequest),
      ).rejects.toThrow();

      expect(mockRepository.update).not.toHaveBeenCalled();
    });
  });

  describe('deletePost', () => {
    it('should delete post successfully', async () => {
      vi.mocked(mockRepository.delete).mockResolvedValue(true);

      await expect(service.deletePost('post-1')).resolves.toBeUndefined();

      expect(mockRepository.delete).toHaveBeenCalledWith('post-1');
    });

    it('should throw PostNotFoundError when post not found', async () => {
      vi.mocked(mockRepository.delete).mockResolvedValue(false);

      await expect(service.deletePost('nonexistent')).rejects.toThrow(
        PostNotFoundError,
      );
    });
  });
});
