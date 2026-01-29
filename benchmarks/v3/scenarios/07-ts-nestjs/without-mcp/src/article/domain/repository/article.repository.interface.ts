import { Article } from '../entity';

export interface ArticleRepository {
  findById(id: string): Promise<Article | null>;
  findByTitle(title: string): Promise<Article | null>;
  findAll(): Promise<Article[]>;
  findByAuthor(author: string): Promise<Article[]>;
  findByTag(tag: string): Promise<Article[]>;
  findPublished(): Promise<Article[]>;
  save(article: Article): Promise<Article>;
  update(article: Article): Promise<Article>;
  delete(id: string): Promise<void>;
  existsById(id: string): Promise<boolean>;
  existsByTitle(title: string): Promise<boolean>;
}

export const ARTICLE_REPOSITORY = Symbol('ARTICLE_REPOSITORY');
