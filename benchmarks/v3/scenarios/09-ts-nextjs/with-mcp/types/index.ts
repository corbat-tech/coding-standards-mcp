/**
 * Blog Post Types and Interfaces
 * Following TypeScript best practices with strict typing
 */

/** Unique identifier for a post */
export type PostId = string;

/** Post status enumeration */
export enum PostStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
}

/** Core Post entity interface */
export interface Post {
  readonly id: PostId;
  readonly title: string;
  readonly content: string;
  readonly author: string;
  readonly status: PostStatus;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

/** Request to create a new post */
export interface CreatePostRequest {
  readonly title: string;
  readonly content: string;
  readonly author: string;
  readonly status?: PostStatus;
}

/** Request to update an existing post */
export interface UpdatePostRequest {
  readonly title?: string;
  readonly content?: string;
  readonly status?: PostStatus;
}

/** Generic API response wrapper */
export interface ApiResponse<T> {
  readonly success: boolean;
  readonly data?: T;
  readonly error?: ApiError;
}

/** API error structure */
export interface ApiError {
  readonly code: string;
  readonly message: string;
  readonly details?: ValidationError[];
}

/** Validation error for form fields */
export interface ValidationError {
  readonly field: string;
  readonly message: string;
}

/** Validation result from validator */
export interface ValidationResult {
  readonly isValid: boolean;
  readonly errors: ValidationError[];
}

/** Repository interface for data access abstraction */
export interface PostRepository {
  findAll(): Promise<Post[]>;
  findById(id: PostId): Promise<Post | null>;
  create(request: CreatePostRequest): Promise<Post>;
  update(id: PostId, request: UpdatePostRequest): Promise<Post | null>;
  delete(id: PostId): Promise<boolean>;
}

/** Service interface for business logic */
export interface PostService {
  getAllPosts(): Promise<Post[]>;
  getPostById(id: PostId): Promise<Post>;
  createPost(request: CreatePostRequest): Promise<Post>;
  updatePost(id: PostId, request: UpdatePostRequest): Promise<Post>;
  deletePost(id: PostId): Promise<void>;
}

/** Validator interface */
export interface PostValidator {
  validateCreate(request: CreatePostRequest): ValidationResult;
  validateUpdate(request: UpdatePostRequest): ValidationResult;
}
