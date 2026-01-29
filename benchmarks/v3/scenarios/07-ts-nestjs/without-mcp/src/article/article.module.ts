import { Module } from '@nestjs/common';
import { ArticleController } from './article.controller';
import { ARTICLE_SERVICE } from './application/port';
import { ARTICLE_REPOSITORY } from './domain/repository';
import { ArticleService } from './application/service';
import { InMemoryArticleRepository } from './infrastructure/persistence';
import {
  CreateArticleUseCase,
  UpdateArticleUseCase,
  DeleteArticleUseCase,
  GetArticleUseCase,
  GetAllArticlesUseCase,
  GetArticlesByAuthorUseCase,
  GetArticlesByTagUseCase,
  GetPublishedArticlesUseCase,
  PublishArticleUseCase,
  UnpublishArticleUseCase,
} from './application/use-case';

@Module({
  controllers: [ArticleController],
  providers: [
    {
      provide: ARTICLE_REPOSITORY,
      useClass: InMemoryArticleRepository,
    },
    {
      provide: ARTICLE_SERVICE,
      useClass: ArticleService,
    },
    CreateArticleUseCase,
    UpdateArticleUseCase,
    DeleteArticleUseCase,
    GetArticleUseCase,
    GetAllArticlesUseCase,
    GetArticlesByAuthorUseCase,
    GetArticlesByTagUseCase,
    GetPublishedArticlesUseCase,
    PublishArticleUseCase,
    UnpublishArticleUseCase,
  ],
  exports: [ARTICLE_SERVICE],
})
export class ArticleModule {}
