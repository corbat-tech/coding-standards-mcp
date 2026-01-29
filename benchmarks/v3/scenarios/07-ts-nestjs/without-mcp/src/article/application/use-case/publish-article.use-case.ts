import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/repository';
import {
  ArticleNotFoundException,
  ArticlePublishException,
} from '../../domain/exception';
import { ArticleResponseDto } from '../dto';

@Injectable()
export class PublishArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string): Promise<ArticleResponseDto> {
    const article = await this.articleRepository.findById(id);
    if (!article) {
      throw new ArticleNotFoundException(id);
    }

    if (article.published) {
      throw new ArticlePublishException(id, 'Article is already published');
    }

    if (article.content.length < 100) {
      throw new ArticlePublishException(
        id,
        'Article content must be at least 100 characters to publish',
      );
    }

    const publishedArticle = article.publish();
    const savedArticle = await this.articleRepository.update(publishedArticle);
    return ArticleResponseDto.fromEntity(savedArticle);
  }
}

@Injectable()
export class UnpublishArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string): Promise<ArticleResponseDto> {
    const article = await this.articleRepository.findById(id);
    if (!article) {
      throw new ArticleNotFoundException(id);
    }

    if (!article.published) {
      throw new ArticlePublishException(id, 'Article is not published');
    }

    const unpublishedArticle = article.unpublish();
    const savedArticle = await this.articleRepository.update(unpublishedArticle);
    return ArticleResponseDto.fromEntity(savedArticle);
  }
}
