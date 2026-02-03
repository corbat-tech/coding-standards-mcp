import { Module } from '@nestjs/common';
import { ArticleController } from './infrastructure/controller/article.controller';
import { ArticleServiceImpl } from './application/use-cases/article.service.impl';
import { InMemoryArticleRepository } from './infrastructure/repository/in-memory-article.repository';
import { ARTICLE_SERVICE } from './application/article.service';
import { ARTICLE_REPOSITORY } from './domain/article.repository';

@Module({
  controllers: [ArticleController],
  providers: [
    {
      provide: ARTICLE_REPOSITORY,
      useClass: InMemoryArticleRepository,
    },
    {
      provide: ARTICLE_SERVICE,
      useClass: ArticleServiceImpl,
    },
  ],
  exports: [ARTICLE_SERVICE],
})
export class ArticleModule {}
