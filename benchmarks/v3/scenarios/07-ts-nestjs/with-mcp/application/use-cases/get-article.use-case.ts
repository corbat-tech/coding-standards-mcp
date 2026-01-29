import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/interfaces/article.repository.interface';
import { ArticleResponseDto } from '../dtos/article-response.dto';
import { ArticleNotFoundException } from '../../domain/exceptions/article.exceptions';

/**
 * Use case interface for retrieving articles
 */
export interface GetArticleUseCase {
  execute(id: string): Promise<ArticleResponseDto>;
}

export const GET_ARTICLE_USE_CASE = Symbol('GET_ARTICLE_USE_CASE');

/**
 * Implementation of the get article use case
 */
@Injectable()
export class GetArticleUseCaseImpl implements GetArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string): Promise<ArticleResponseDto> {
    const article = await this.articleRepository.findById(id);

    if (!article) {
      throw new ArticleNotFoundException(id);
    }

    return ArticleResponseDto.fromEntity(article);
  }
}
