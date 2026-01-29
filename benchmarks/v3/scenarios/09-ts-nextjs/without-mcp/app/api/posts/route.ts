/**
 * API Route: /api/posts
 * Handles GET (list) and POST (create) operations for posts
 */

import { NextRequest, NextResponse } from 'next/server';
import { CreatePostInput, ApiResponse, Post, PostListResponse } from '@/types/post';
import { validateCreatePost, hasValidationErrors } from '@/lib/validation';
import { createPost, getAllPosts } from '@/lib/posts-store';
import { ApiError, handleApiError } from '@/lib/api-error';

/**
 * GET /api/posts
 * Retrieves a paginated list of posts
 */
export async function GET(request: NextRequest): Promise<NextResponse<ApiResponse<PostListResponse>>> {
  try {
    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get('page') || '1', 10);
    const limit = parseInt(searchParams.get('limit') || '10', 10);
    const publishedOnly = searchParams.get('published') === 'true';

    // Validate pagination parameters
    if (page < 1) {
      throw ApiError.badRequest('Page must be a positive integer');
    }
    if (limit < 1 || limit > 100) {
      throw ApiError.badRequest('Limit must be between 1 and 100');
    }

    const result = getAllPosts({ page, limit, publishedOnly });

    return NextResponse.json({
      success: true,
      data: result,
    });
  } catch (error) {
    const { status, body } = handleApiError(error);
    return NextResponse.json(body as ApiResponse<PostListResponse>, { status });
  }
}

/**
 * POST /api/posts
 * Creates a new post
 */
export async function POST(request: NextRequest): Promise<NextResponse<ApiResponse<Post>>> {
  try {
    const body = await request.json();
    const input: CreatePostInput = {
      title: body.title,
      content: body.content,
      excerpt: body.excerpt,
      author: body.author,
      published: body.published,
    };

    // Validate input
    const validationErrors = validateCreatePost(input);
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

    // Create the post
    const post = createPost(input);

    return NextResponse.json(
      {
        success: true,
        data: post,
      },
      { status: 201 }
    );
  } catch (error) {
    const { status, body } = handleApiError(error);
    return NextResponse.json(body as ApiResponse<Post>, { status });
  }
}
