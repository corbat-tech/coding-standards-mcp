import { Module } from '@nestjs/common';
import { ArticleController } from './infrastructure/controllers/article.controller';
import { ArticleServiceImpl } from './infrastructure/services/article.service.impl';
import { InMemoryArticleRepository } from './infrastructure/repositories/in-memory-article.repository';
import { ARTICLE_REPOSITORY } from './domain/interfaces/article.repository.interface';
import { ARTICLE_SERVICE } from './domain/interfaces/article.service.interface';
import {
  CREATE_ARTICLE_USE_CASE,
  CreateArticleUseCaseImpl,
} from './application/use-cases/create-article.use-case';
import {
  GET_ARTICLE_USE_CASE,
  GetArticleUseCaseImpl,
} from './application/use-cases/get-article.use-case';
import {
  LIST_ARTICLES_USE_CASE,
  ListArticlesUseCaseImpl,
} from './application/use-cases/list-articles.use-case';
import {
  UPDATE_ARTICLE_USE_CASE,
  UpdateArticleUseCaseImpl,
} from './application/use-cases/update-article.use-case';
import {
  DELETE_ARTICLE_USE_CASE,
  DeleteArticleUseCaseImpl,
} from './application/use-cases/delete-article.use-case';

/**
 * NestJS Module for Article management
 * Wires together all layers using dependency injection
 */
@Module({
  controllers: [ArticleController],
  providers: [
    // Repository - Infrastructure layer
    {
      provide: ARTICLE_REPOSITORY,
      useClass: InMemoryArticleRepository,
    },
    // Service - Application layer facade
    {
      provide: ARTICLE_SERVICE,
      useClass: ArticleServiceImpl,
    },
    // Use Cases - Application layer
    {
      provide: CREATE_ARTICLE_USE_CASE,
      useClass: CreateArticleUseCaseImpl,
    },
    {
      provide: GET_ARTICLE_USE_CASE,
      useClass: GetArticleUseCaseImpl,
    },
    {
      provide: LIST_ARTICLES_USE_CASE,
      useClass: ListArticlesUseCaseImpl,
    },
    {
      provide: UPDATE_ARTICLE_USE_CASE,
      useClass: UpdateArticleUseCaseImpl,
    },
    {
      provide: DELETE_ARTICLE_USE_CASE,
      useClass: DeleteArticleUseCaseImpl,
    },
  ],
  exports: [
    ARTICLE_SERVICE,
    ARTICLE_REPOSITORY,
    CREATE_ARTICLE_USE_CASE,
    GET_ARTICLE_USE_CASE,
    LIST_ARTICLES_USE_CASE,
    UPDATE_ARTICLE_USE_CASE,
    DELETE_ARTICLE_USE_CASE,
  ],
})
export class ArticleModule {}
