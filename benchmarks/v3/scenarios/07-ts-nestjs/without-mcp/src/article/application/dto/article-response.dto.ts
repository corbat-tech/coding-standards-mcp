import { Article } from '../../domain/entity';

export class ArticleResponseDto {
  id: string;
  title: string;
  content: string;
  author: string;
  tags: string[];
  published: boolean;
  createdAt: Date;
  updatedAt: Date;

  static fromEntity(article: Article): ArticleResponseDto {
    const dto = new ArticleResponseDto();
    dto.id = article.id;
    dto.title = article.title;
    dto.content = article.content;
    dto.author = article.author;
    dto.tags = article.tags;
    dto.published = article.published;
    dto.createdAt = article.createdAt;
    dto.updatedAt = article.updatedAt;
    return dto;
  }

  static fromEntities(articles: Article[]): ArticleResponseDto[] {
    return articles.map((article) => ArticleResponseDto.fromEntity(article));
  }
}
