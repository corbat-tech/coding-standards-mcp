import { Article } from '../../domain/entities/article.entity';

/**
 * DTO for article responses
 */
export class ArticleResponseDto {
  id: string;
  title: string;
  content: string;
  authorId: string;
  tags: string[];
  createdAt: Date;
  updatedAt: Date;

  constructor(article: Article) {
    this.id = article.id;
    this.title = article.title;
    this.content = article.content;
    this.authorId = article.authorId;
    this.tags = article.tags;
    this.createdAt = article.createdAt;
    this.updatedAt = article.updatedAt;
  }

  static fromEntity(article: Article): ArticleResponseDto {
    return new ArticleResponseDto(article);
  }

  static fromEntities(articles: Article[]): ArticleResponseDto[] {
    return articles.map((article) => ArticleResponseDto.fromEntity(article));
  }
}
