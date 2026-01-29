import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/interfaces/article.repository.interface';
import { ArticleNotFoundException } from '../../domain/exceptions/article.exceptions';

/**
 * Use case interface for deleting articles
 */
export interface DeleteArticleUseCase {
  execute(id: string): Promise<void>;
}

export const DELETE_ARTICLE_USE_CASE = Symbol('DELETE_ARTICLE_USE_CASE');

/**
 * Implementation of the delete article use case
 */
@Injectable()
export class DeleteArticleUseCaseImpl implements DeleteArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string): Promise<void> {
    const exists = await this.articleRepository.exists(id);

    if (!exists) {
      throw new ArticleNotFoundException(id);
    }

    await this.articleRepository.delete(id);
  }
}
