import { Article } from './article.entity';

export interface ArticleRepository {
  save(article: Article): Promise<Article>;
  findById(id: string): Promise<Article | null>;
  findAll(): Promise<Article[]>;
  findByAuthor(author: string): Promise<Article[]>;
  delete(id: string): Promise<void>;
}

export const ARTICLE_REPOSITORY = 'ARTICLE_REPOSITORY';
