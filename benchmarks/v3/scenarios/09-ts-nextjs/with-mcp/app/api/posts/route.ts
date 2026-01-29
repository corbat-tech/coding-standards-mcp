/**
 * Posts API Route Handler
 * Handles GET (list all) and POST (create) operations
 */

import { NextRequest, NextResponse } from 'next/server';
import type { ApiResponse, Post, CreatePostRequest } from '../../../types';
import {
  createPostService,
  createPostRepository,
  createPostValidator,
  PostNotFoundError,
  ValidationFailedError,
} from '../../../lib';

/** Create service instance with dependencies */
function getService() {
  const repository = createPostRepository();
  const validator = createPostValidator();
  return createPostService(repository, validator);
}

/** GET /api/posts - List all posts */
export async function GET(): Promise<NextResponse<ApiResponse<Post[]>>> {
  try {
    const service = getService();
    const posts = await service.getAllPosts();

    return NextResponse.json({
      success: true,
      data: posts,
    });
  } catch (error) {
    return handleError(error);
  }
}

/** POST /api/posts - Create a new post */
export async function POST(
  request: NextRequest,
): Promise<NextResponse<ApiResponse<Post>>> {
  try {
    const body = (await request.json()) as CreatePostRequest;
    const service = getService();
    const post = await service.createPost(body);

    return NextResponse.json(
      {
        success: true,
        data: post,
      },
      { status: 201 },
    );
  } catch (error) {
    return handleError(error);
  }
}

/** Centralized error handler */
function handleError(error: unknown): NextResponse<ApiResponse<never>> {
  if (error instanceof ValidationFailedError) {
    return NextResponse.json(
      {
        success: false,
        error: {
          code: error.code,
          message: error.message,
          details: error.errors,
        },
      },
      { status: 400 },
    );
  }

  if (error instanceof PostNotFoundError) {
    return NextResponse.json(
      {
        success: false,
        error: {
          code: error.code,
          message: error.message,
        },
      },
      { status: 404 },
    );
  }

  console.error('Unexpected error:', error);

  return NextResponse.json(
    {
      success: false,
      error: {
        code: 'INTERNAL_ERROR',
        message: 'An unexpected error occurred',
      },
    },
    { status: 500 },
  );
}
