import { Inject, Injectable } from '@nestjs/common';
import { Article } from '../../domain/entities/article.entity';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/interfaces/article.repository.interface';
import { CreateArticleDto } from '../dtos/create-article.dto';
import { ArticleResponseDto } from '../dtos/article-response.dto';
import { ArticleValidationException } from '../../domain/exceptions/article.exceptions';

/**
 * Use case interface for creating articles
 */
export interface CreateArticleUseCase {
  execute(dto: CreateArticleDto): Promise<ArticleResponseDto>;
}

export const CREATE_ARTICLE_USE_CASE = Symbol('CREATE_ARTICLE_USE_CASE');

/**
 * Implementation of the create article use case
 */
@Injectable()
export class CreateArticleUseCaseImpl implements CreateArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(dto: CreateArticleDto): Promise<ArticleResponseDto> {
    this.validateDto(dto);

    const article = new Article({
      title: dto.title.trim(),
      content: dto.content.trim(),
      authorId: dto.authorId,
      tags: dto.tags ?? [],
    });

    const savedArticle = await this.articleRepository.save(article);
    return ArticleResponseDto.fromEntity(savedArticle);
  }

  private validateDto(dto: CreateArticleDto): void {
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
}
