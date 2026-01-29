import { CreateArticleDto, UpdateArticleDto, ArticleResponseDto } from '../dto';

export interface ArticleServiceInterface {
  createArticle(dto: CreateArticleDto): Promise<ArticleResponseDto>;
  updateArticle(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto>;
  deleteArticle(id: string): Promise<void>;
  getArticleById(id: string): Promise<ArticleResponseDto>;
  getAllArticles(): Promise<ArticleResponseDto[]>;
  getArticlesByAuthor(author: string): Promise<ArticleResponseDto[]>;
  getArticlesByTag(tag: string): Promise<ArticleResponseDto[]>;
  getPublishedArticles(): Promise<ArticleResponseDto[]>;
  publishArticle(id: string): Promise<ArticleResponseDto>;
  unpublishArticle(id: string): Promise<ArticleResponseDto>;
}

export const ARTICLE_SERVICE = Symbol('ARTICLE_SERVICE');
