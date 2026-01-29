/**
 * Single Post API Route Handler
 * Handles GET (by id), PUT (update), and DELETE operations
 */

import { NextRequest, NextResponse } from 'next/server';
import type { ApiResponse, Post, UpdatePostRequest } from '../../../../types';
import {
  createPostService,
  createPostRepository,
  createPostValidator,
  PostNotFoundError,
  ValidationFailedError,
} from '../../../../lib';

/** Route params type */
interface RouteParams {
  params: Promise<{ id: string }>;
}

/** Create service instance with dependencies */
function getService() {
  const repository = createPostRepository();
  const validator = createPostValidator();
  return createPostService(repository, validator);
}

/** GET /api/posts/[id] - Get single post by ID */
export async function GET(
  _request: NextRequest,
  context: RouteParams,
): Promise<NextResponse<ApiResponse<Post>>> {
  try {
    const { id } = await context.params;
    const service = getService();
    const post = await service.getPostById(id);

    return NextResponse.json({
      success: true,
      data: post,
    });
  } catch (error) {
    return handleError(error);
  }
}

/** PUT /api/posts/[id] - Update existing post */
export async function PUT(
  request: NextRequest,
  context: RouteParams,
): Promise<NextResponse<ApiResponse<Post>>> {
  try {
    const { id } = await context.params;
    const body = (await request.json()) as UpdatePostRequest;
    const service = getService();
    const post = await service.updatePost(id, body);

    return NextResponse.json({
      success: true,
      data: post,
    });
  } catch (error) {
    return handleError(error);
  }
}

/** DELETE /api/posts/[id] - Delete post */
export async function DELETE(
  _request: NextRequest,
  context: RouteParams,
): Promise<NextResponse<ApiResponse<void>>> {
  try {
    const { id } = await context.params;
    const service = getService();
    await service.deletePost(id);

    return NextResponse.json({
      success: true,
    });
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
