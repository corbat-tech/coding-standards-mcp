import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/repository';
import { ArticleNotFoundException } from '../../domain/exception';

@Injectable()
export class DeleteArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string): Promise<void> {
    const exists = await this.articleRepository.existsById(id);
    if (!exists) {
      throw new ArticleNotFoundException(id);
    }

    await this.articleRepository.delete(id);
  }
}
