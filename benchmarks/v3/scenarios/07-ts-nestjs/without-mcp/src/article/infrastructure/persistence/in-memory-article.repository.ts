import { Injectable } from '@nestjs/common';
import { Article } from '../../domain/entity';
import { ArticleRepository } from '../../domain/repository';

@Injectable()
export class InMemoryArticleRepository implements ArticleRepository {
  private articles: Map<string, Article> = new Map();

  async findById(id: string): Promise<Article | null> {
    return this.articles.get(id) ?? null;
  }

  async findByTitle(title: string): Promise<Article | null> {
    for (const article of this.articles.values()) {
      if (article.title === title) {
        return article;
      }
    }
    return null;
  }

  async findAll(): Promise<Article[]> {
    return Array.from(this.articles.values());
  }

  async findByAuthor(author: string): Promise<Article[]> {
    return Array.from(this.articles.values()).filter(
      (article) => article.author === author,
    );
  }

  async findByTag(tag: string): Promise<Article[]> {
    return Array.from(this.articles.values()).filter((article) =>
      article.tags.includes(tag),
    );
  }

  async findPublished(): Promise<Article[]> {
    return Array.from(this.articles.values()).filter(
      (article) => article.published,
    );
  }

  async save(article: Article): Promise<Article> {
    this.articles.set(article.id, article);
    return article;
  }

  async update(article: Article): Promise<Article> {
    this.articles.set(article.id, article);
    return article;
  }

  async delete(id: string): Promise<void> {
    this.articles.delete(id);
  }

  async existsById(id: string): Promise<boolean> {
    return this.articles.has(id);
  }

  async existsByTitle(title: string): Promise<boolean> {
    for (const article of this.articles.values()) {
      if (article.title === title) {
        return true;
      }
    }
    return false;
  }

  clear(): void {
    this.articles.clear();
  }
}
