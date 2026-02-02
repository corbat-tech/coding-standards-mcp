import { Injectable } from '@nestjs/common';
import { Article } from '../../domain/article.entity';
import { ArticleRepository } from '../../domain/article.repository';

@Injectable()
export class InMemoryArticleRepository implements ArticleRepository {
  private articles = new Map<string, Article>();

  async save(article: Article): Promise<Article> {
    this.articles.set(article.id, article);
    return article;
  }

  async findById(id: string): Promise<Article | null> {
    return this.articles.get(id) ?? null;
  }

  async findAll(): Promise<Article[]> {
    return Array.from(this.articles.values());
  }

  async findByAuthor(author: string): Promise<Article[]> {
    return Array.from(this.articles.values()).filter((a) => a.author === author);
  }

  async delete(id: string): Promise<void> {
    this.articles.delete(id);
  }
}
