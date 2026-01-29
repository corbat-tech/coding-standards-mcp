/**
 * API Route: /api/posts/[id]
 * Handles GET (single), PUT (update), and DELETE operations for a specific post
 */

import { NextRequest, NextResponse } from 'next/server';
import { UpdatePostInput, ApiResponse, Post } from '@/types/post';
import { validateUpdatePost, hasValidationErrors } from '@/lib/validation';
import { getPostById, updatePost, deletePost } from '@/lib/posts-store';
import { ApiError, handleApiError } from '@/lib/api-error';

interface RouteParams {
  params: Promise<{ id: string }>;
}

/**
 * GET /api/posts/[id]
 * Retrieves a single post by ID
 */
export async function GET(
  request: NextRequest,
  { params }: RouteParams
): Promise<NextResponse<ApiResponse<Post>>> {
  try {
    const { id } = await params;
    const post = getPostById(id);

    if (!post) {
      throw ApiError.notFound(`Post with ID '${id}' not found`);
    }

    return NextResponse.json({
      success: true,
      data: post,
    });
  } catch (error) {
    const { status, body } = handleApiError(error);
    return NextResponse.json(body as ApiResponse<Post>, { status });
  }
}

/**
 * PUT /api/posts/[id]
 * Updates an existing post
 */
export async function PUT(
  request: NextRequest,
  { params }: RouteParams
): Promise<NextResponse<ApiResponse<Post>>> {
  try {
    const { id } = await params;

    // Check if post exists
    const existingPost = getPostById(id);
    if (!existingPost) {
      throw ApiError.notFound(`Post with ID '${id}' not found`);
    }

    const body = await request.json();
    const input: UpdatePostInput = {
      title: body.title,
      content: body.content,
      excerpt: body.excerpt,
      author: body.author,
      published: body.published,
    };

    // Remove undefined values
    Object.keys(input).forEach((key) => {
      if (input[key as keyof UpdatePostInput] === undefined) {
        delete input[key as keyof UpdatePostInput];
      }
    });

    // Check if there's anything to update
    if (Object.keys(input).length === 0) {
      throw ApiError.badRequest('No fields provided for update');
    }

    // Validate input
    const validationErrors = validateUpdatePost(input);
    if (hasValidationErrors(validationErrors)) {
      return NextResponse.json(
        {
          success: false,
          error: 'Validation failed',
          errors: validationErrors,
        },
        { status: 422 }
      );
    }

    // Update the post
    const updatedPost = updatePost(id, input);

    if (!updatedPost) {
      throw ApiError.internalError('Failed to update post');
    }

    return NextResponse.json({
      success: true,
      data: updatedPost,
    });
  } catch (error) {
    const { status, body } = handleApiError(error);
    return NextResponse.json(body as ApiResponse<Post>, { status });
  }
}

/**
 * DELETE /api/posts/[id]
 * Deletes a post
 */
export async function DELETE(
  request: NextRequest,
  { params }: RouteParams
): Promise<NextResponse<ApiResponse<{ deleted: boolean }>>> {
  try {
    const { id } = await params;

    // Check if post exists
    const existingPost = getPostById(id);
    if (!existingPost) {
      throw ApiError.notFound(`Post with ID '${id}' not found`);
    }

    const deleted = deletePost(id);

    if (!deleted) {
      throw ApiError.internalError('Failed to delete post');
    }

    return NextResponse.json({
      success: true,
      data: { deleted: true },
    });
  } catch (error) {
    const { status, body } = handleApiError(error);
    return NextResponse.json(body as ApiResponse<{ deleted: boolean }>, { status });
  }
}

/**
 * PATCH /api/posts/[id]
 * Partially updates an existing post (same as PUT for this implementation)
 */
export async function PATCH(
  request: NextRequest,
  context: RouteParams
): Promise<NextResponse<ApiResponse<Post>>> {
  return PUT(request, context);
}
