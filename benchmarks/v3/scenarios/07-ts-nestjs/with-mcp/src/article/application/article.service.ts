import { Article, CreateArticleProps, UpdateArticleProps } from '../domain/article.entity';

export interface ArticleService {
  create(props: CreateArticleProps): Promise<Article>;
  getById(id: string): Promise<Article>;
  getAll(): Promise<Article[]>;
  getByAuthor(author: string): Promise<Article[]>;
  update(id: string, props: UpdateArticleProps): Promise<Article>;
  publish(id: string): Promise<Article>;
  delete(id: string): Promise<void>;
}

export const ARTICLE_SERVICE = 'ARTICLE_SERVICE';
