/**
 * Post Service Implementation
 * Business logic layer with validation and error handling
 */

import type {
  Post,
  PostId,
  PostService,
  PostRepository,
  PostValidator,
  CreatePostRequest,
  UpdatePostRequest,
} from '../types';
import { PostNotFoundError, ValidationFailedError } from './errors';

/** Post service implementation with dependency injection */
export class PostServiceImpl implements PostService {
  constructor(
    private readonly repository: PostRepository,
    private readonly validator: PostValidator,
  ) {}

  async getAllPosts(): Promise<Post[]> {
    return this.repository.findAll();
  }

  async getPostById(id: PostId): Promise<Post> {
    const post = await this.repository.findById(id);

    if (!post) {
      throw new PostNotFoundError(id);
    }

    return post;
  }

  async createPost(request: CreatePostRequest): Promise<Post> {
    const validation = this.validator.validateCreate(request);

    if (!validation.isValid) {
      throw new ValidationFailedError(
        'Post validation failed',
        validation.errors,
      );
    }

    return this.repository.create(request);
  }

  async updatePost(id: PostId, request: UpdatePostRequest): Promise<Post> {
    const validation = this.validator.validateUpdate(request);

    if (!validation.isValid) {
      throw new ValidationFailedError(
        'Post validation failed',
        validation.errors,
      );
    }

    const updated = await this.repository.update(id, request);

    if (!updated) {
      throw new PostNotFoundError(id);
    }

    return updated;
  }

  async deletePost(id: PostId): Promise<void> {
    const deleted = await this.repository.delete(id);

    if (!deleted) {
      throw new PostNotFoundError(id);
    }
  }
}

/** Factory function for creating service instance */
export function createPostService(
  repository: PostRepository,
  validator: PostValidator,
): PostService {
  return new PostServiceImpl(repository, validator);
}
