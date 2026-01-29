import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/interfaces/article.repository.interface';
import { UpdateArticleDto } from '../dtos/update-article.dto';
import { ArticleResponseDto } from '../dtos/article-response.dto';
import {
  ArticleNotFoundException,
  ArticleValidationException,
} from '../../domain/exceptions/article.exceptions';

/**
 * Use case interface for updating articles
 */
export interface UpdateArticleUseCase {
  execute(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto>;
}

export const UPDATE_ARTICLE_USE_CASE = Symbol('UPDATE_ARTICLE_USE_CASE');

/**
 * Implementation of the update article use case
 */
@Injectable()
export class UpdateArticleUseCaseImpl implements UpdateArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto> {
    const article = await this.articleRepository.findById(id);

    if (!article) {
      throw new ArticleNotFoundException(id);
    }

    this.validateDto(dto, article);

    const updatedTitle = dto.title?.trim() ?? article.title;
    const updatedContent = dto.content?.trim() ?? article.content;
    const updatedTags = dto.tags ?? article.tags;

    article.update(updatedTitle, updatedContent, updatedTags);

    const updatedArticle = await this.articleRepository.update(article);
    return ArticleResponseDto.fromEntity(updatedArticle);
  }

  private validateDto(dto: UpdateArticleDto, article: { title: string; content: string }): void {
    if (dto.title !== undefined && !dto.title.trim()) {
      throw new ArticleValidationException('Title cannot be empty');
    }
    if (dto.content !== undefined && !dto.content.trim()) {
      throw new ArticleValidationException('Content cannot be empty');
    }
  }
}
