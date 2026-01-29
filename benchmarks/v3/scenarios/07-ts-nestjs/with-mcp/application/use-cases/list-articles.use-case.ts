import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/interfaces/article.repository.interface';
import { ArticleResponseDto } from '../dtos/article-response.dto';

/**
 * Use case interface for listing articles
 */
export interface ListArticlesUseCase {
  execute(): Promise<ArticleResponseDto[]>;
  executeByAuthor(authorId: string): Promise<ArticleResponseDto[]>;
}

export const LIST_ARTICLES_USE_CASE = Symbol('LIST_ARTICLES_USE_CASE');

/**
 * Implementation of the list articles use case
 */
@Injectable()
export class ListArticlesUseCaseImpl implements ListArticlesUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findAll();
    return ArticleResponseDto.fromEntities(articles);
  }

  async executeByAuthor(authorId: string): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findByAuthorId(authorId);
    return ArticleResponseDto.fromEntities(articles);
  }
}
