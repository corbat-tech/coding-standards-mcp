import { Injectable } from '@nestjs/common';
import { Article } from '../../domain/entities/article.entity';
import { ArticleRepository } from '../../domain/interfaces/article.repository.interface';

/**
 * In-memory implementation of ArticleRepository
 * Suitable for testing and development purposes
 */
@Injectable()
export class InMemoryArticleRepository implements ArticleRepository {
  private readonly articles: Map<string, Article> = new Map();

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

  async findByAuthorId(authorId: string): Promise<Article[]> {
    return Array.from(this.articles.values()).filter(
      (article) => article.authorId === authorId,
    );
  }

  async update(article: Article): Promise<Article> {
    this.articles.set(article.id, article);
    return article;
  }

  async delete(id: string): Promise<boolean> {
    return this.articles.delete(id);
  }

  async exists(id: string): Promise<boolean> {
    return this.articles.has(id);
  }

  /**
   * Clears all articles (useful for testing)
   */
  clear(): void {
    this.articles.clear();
  }
}
