import { Article } from '../../domain/article.entity';

export class ArticleResponseDto {
  id: string;
  title: string;
  content: string;
  author: string;
  tags: string[];
  published: boolean;
  createdAt: string;
  updatedAt: string;

  constructor(article: Article) {
    this.id = article.id;
    this.title = article.title;
    this.content = article.content;
    this.author = article.author;
    this.tags = article.tags;
    this.published = article.published;
    this.createdAt = article.createdAt.toISOString();
    this.updatedAt = article.updatedAt.toISOString();
  }
}
