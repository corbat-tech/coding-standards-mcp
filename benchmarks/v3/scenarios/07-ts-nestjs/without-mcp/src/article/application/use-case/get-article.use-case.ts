import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/repository';
import { ArticleNotFoundException } from '../../domain/exception';
import { ArticleResponseDto } from '../dto';

@Injectable()
export class GetArticleUseCase {
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

@Injectable()
export class GetAllArticlesUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findAll();
    return ArticleResponseDto.fromEntities(articles);
  }
}

@Injectable()
export class GetArticlesByAuthorUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(author: string): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findByAuthor(author);
    return ArticleResponseDto.fromEntities(articles);
  }
}

@Injectable()
export class GetArticlesByTagUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(tag: string): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findByTag(tag);
    return ArticleResponseDto.fromEntities(articles);
  }
}

@Injectable()
export class GetPublishedArticlesUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(): Promise<ArticleResponseDto[]> {
    const articles = await this.articleRepository.findPublished();
    return ArticleResponseDto.fromEntities(articles);
  }
}
