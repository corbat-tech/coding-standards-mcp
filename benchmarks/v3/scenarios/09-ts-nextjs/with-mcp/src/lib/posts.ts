import { v4 as uuidv4 } from 'uuid';
import { Post, CreatePostDto, UpdatePostDto } from './types';

const posts = new Map<string, Post>();

export async function getAllPosts(): Promise<Post[]> {
  return Array.from(posts.values());
}

export async function getPostById(id: string): Promise<Post | null> {
  return posts.get(id) ?? null;
}

export async function createPost(dto: CreatePostDto): Promise<Post> {
  const now = new Date();
  const post: Post = {
    id: uuidv4(),
    title: dto.title,
    content: dto.content,
    author: dto.author,
    published: false,
    createdAt: now,
    updatedAt: now,
  };
  posts.set(post.id, post);
  return post;
}

export async function updatePost(id: string, dto: UpdatePostDto): Promise<Post | null> {
  const post = posts.get(id);
  if (!post) return null;

  const updated: Post = {
    ...post,
    title: dto.title ?? post.title,
    content: dto.content ?? post.content,
    published: dto.published ?? post.published,
    updatedAt: new Date(),
  };
  posts.set(id, updated);
  return updated;
}

export async function deletePost(id: string): Promise<boolean> {
  return posts.delete(id);
}
