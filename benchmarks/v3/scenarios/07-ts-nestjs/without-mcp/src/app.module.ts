import { Module } from '@nestjs/common';
import { ArticleModule } from './article';

@Module({
  imports: [ArticleModule],
})
export class AppModule {}
