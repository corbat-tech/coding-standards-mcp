import { Injectable, Inject } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { ArticleService } from '../article.service';
import { Article, CreateArticleProps, UpdateArticleProps } from '../../domain/article.entity';
import { ArticleRepository, ARTICLE_REPOSITORY } from '../../domain/article.repository';
import { ArticleNotFoundException, ArticleAlreadyPublishedException } from '../../domain/article.exceptions';

@Injectable()
export class ArticleServiceImpl implements ArticleService {
  constructor(
    @Inject(ARTICLE_REPOSITORY) private readonly repository: ArticleRepository
  ) {}

  async create(props: CreateArticleProps): Promise<Article> {
    const now = new Date();
    const article: Article = {
      id: uuidv4(),
      title: props.title,
      content: props.content,
      author: props.author,
      tags: props.tags ?? [],
      published: false,
      createdAt: now,
      updatedAt: now,
    };
    return this.repository.save(article);
  }

  async getById(id: string): Promise<Article> {
    const article = await this.repository.findById(id);
    if (!article) {
      throw new ArticleNotFoundException(id);
    }
    return article;
  }

  async getAll(): Promise<Article[]> {
    return this.repository.findAll();
  }

  async getByAuthor(author: string): Promise<Article[]> {
    return this.repository.findByAuthor(author);
  }

  async update(id: string, props: UpdateArticleProps): Promise<Article> {
    const article = await this.getById(id);
    const updated: Article = {
      ...article,
      title: props.title ?? article.title,
      content: props.content ?? article.content,
      tags: props.tags ?? article.tags,
      published: props.published ?? article.published,
      updatedAt: new Date(),
    };
    return this.repository.save(updated);
  }

  async publish(id: string): Promise<Article> {
    const article = await this.getById(id);
    if (article.published) {
      throw new ArticleAlreadyPublishedException(id);
    }
    return this.update(id, { published: true });
  }

  async delete(id: string): Promise<void> {
    await this.getById(id);
    await this.repository.delete(id);
  }
}
