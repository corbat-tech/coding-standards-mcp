/**
 * Post Repository Implementation
 * In-memory repository for demonstration; can be replaced with database
 */

import type {
  Post,
  PostId,
  PostRepository,
  CreatePostRequest,
  UpdatePostRequest,
} from '../types';
import { PostStatus } from '../types';

/** In-memory storage for posts */
const postsStore = new Map<PostId, Post>();

/** Generate unique ID */
function generateId(): PostId {
  return `post_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

/** In-memory repository implementation */
export class InMemoryPostRepository implements PostRepository {
  async findAll(): Promise<Post[]> {
    return Array.from(postsStore.values()).sort(
      (a, b) => b.createdAt.getTime() - a.createdAt.getTime(),
    );
  }

  async findById(id: PostId): Promise<Post | null> {
    return postsStore.get(id) ?? null;
  }

  async create(request: CreatePostRequest): Promise<Post> {
    const now = new Date();
    const post: Post = {
      id: generateId(),
      title: request.title.trim(),
      content: request.content.trim(),
      author: request.author.trim(),
      status: request.status ?? PostStatus.DRAFT,
      createdAt: now,
      updatedAt: now,
    };

    postsStore.set(post.id, post);
    return post;
  }

  async update(id: PostId, request: UpdatePostRequest): Promise<Post | null> {
    const existing = postsStore.get(id);

    if (!existing) {
      return null;
    }

    const updated: Post = {
      ...existing,
      title: request.title?.trim() ?? existing.title,
      content: request.content?.trim() ?? existing.content,
      status: request.status ?? existing.status,
      updatedAt: new Date(),
    };

    postsStore.set(id, updated);
    return updated;
  }

  async delete(id: PostId): Promise<boolean> {
    return postsStore.delete(id);
  }

  /** Clear all posts - useful for testing */
  async clear(): Promise<void> {
    postsStore.clear();
  }
}

/** Singleton repository instance */
let repositoryInstance: InMemoryPostRepository | null = null;

/** Factory function for creating repository instance */
export function createPostRepository(): PostRepository {
  if (!repositoryInstance) {
    repositoryInstance = new InMemoryPostRepository();
  }
  return repositoryInstance;
}

/** Get repository instance for testing */
export function getRepositoryInstance(): InMemoryPostRepository {
  if (!repositoryInstance) {
    repositoryInstance = new InMemoryPostRepository();
  }
  return repositoryInstance;
}
