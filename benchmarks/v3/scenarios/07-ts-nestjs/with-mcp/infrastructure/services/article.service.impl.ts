import { Inject, Injectable } from '@nestjs/common';
import { Article } from '../../domain/entities/article.entity';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/interfaces/article.repository.interface';
import { ArticleService } from '../../domain/interfaces/article.service.interface';
import { CreateArticleDto } from '../../application/dtos/create-article.dto';
import { UpdateArticleDto } from '../../application/dtos/update-article.dto';
import { ArticleResponseDto } from '../../application/dtos/article-response.dto';
import {
  ArticleNotFoundException,
  ArticleValidationException,
} from '../../domain/exceptions/article.exceptions';

/**
 * Implementation of ArticleService interface
 * Orchestrates article operations using the repository
 */
@Injectable()
export class ArticleServiceImpl implements ArticleService {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async create(dto: CreateArticleDto): Promise<ArticleResponseDto> {
    this.validateCreateDto(dto);

    const article = new Article({
      title: dto.title.trim(),
      content: dto.content.trim(),
      authorId: dto.authorId,
      tags: dto.tags ?? [],
    });

    const savedArticle = await this.articleRepository.save(article);
    return ArticleResponseDto.fromEntity(savedArticle);
  }

  async getById(id: string): Promise<ArticleResponseDto> {
    const article = await this.articleRepository.findById(id);

    if (!article) {
      throw new ArticleNotFoundException(id);
    }

    return ArticleResponseDto.fromEntity(article);
  }

  async getAll(): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findAll();
    return ArticleResponseDto.fromEntities(articles);
  }

  async getByAuthor(authorId: string): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findByAuthorId(authorId);
    return ArticleResponseDto.fromEntities(articles);
  }

  async update(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto> {
    const article = await this.articleRepository.findById(id);

    if (!article) {
      throw new ArticleNotFoundException(id);
    }

    this.validateUpdateDto(dto);

    const updatedTitle = dto.title?.trim() ?? article.title;
    const updatedContent = dto.content?.trim() ?? article.content;
    const updatedTags = dto.tags ?? article.tags;

    article.update(updatedTitle, updatedContent, updatedTags);

    const updatedArticle = await this.articleRepository.update(article);
    return ArticleResponseDto.fromEntity(updatedArticle);
  }

  async delete(id: string): Promise<void> {
    const exists = await this.articleRepository.exists(id);

    if (!exists) {
      throw new ArticleNotFoundException(id);
    }

    await this.articleRepository.delete(id);
  }

  private validateCreateDto(dto: CreateArticleDto): void {
    if (!dto.title?.trim()) {
      throw new ArticleValidationException('Title cannot be empty');
    }
    if (!dto.content?.trim()) {
      throw new ArticleValidationException('Content cannot be empty');
    }
    if (!dto.authorId?.trim()) {
      throw new ArticleValidationException('Author ID cannot be empty');
    }
  }

  private validateUpdateDto(dto: UpdateArticleDto): void {
    if (dto.title !== undefined && !dto.title.trim()) {
      throw new ArticleValidationException('Title cannot be empty');
    }
    if (dto.content !== undefined && !dto.content.trim()) {
      throw new ArticleValidationException('Content cannot be empty');
    }
  }
}
