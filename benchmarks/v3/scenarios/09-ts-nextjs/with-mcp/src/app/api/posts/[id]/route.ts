import { NextRequest, NextResponse } from 'next/server';
import { getPostById, updatePost, deletePost } from '@/lib/posts';
import { UpdatePostSchema } from '@/lib/types';
import { ZodError } from 'zod';

interface Params {
  params: { id: string };
}

export async function GET(_request: NextRequest, { params }: Params) {
  const post = await getPostById(params.id);
  if (!post) {
    return NextResponse.json({ error: 'Post not found' }, { status: 404 });
  }
  return NextResponse.json(post);
}

export async function PUT(request: NextRequest, { params }: Params) {
  try {
    const body = await request.json();
    const dto = UpdatePostSchema.parse(body);
    const post = await updatePost(params.id, dto);
    if (!post) {
      return NextResponse.json({ error: 'Post not found' }, { status: 404 });
    }
    return NextResponse.json(post);
  } catch (error) {
    if (error instanceof ZodError) {
      return NextResponse.json(
        { error: 'Validation error', details: error.errors },
        { status: 400 }
      );
    }
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function DELETE(_request: NextRequest, { params }: Params) {
  const deleted = await deletePost(params.id);
  if (!deleted) {
    return NextResponse.json({ error: 'Post not found' }, { status: 404 });
  }
  return new NextResponse(null, { status: 204 });
}
