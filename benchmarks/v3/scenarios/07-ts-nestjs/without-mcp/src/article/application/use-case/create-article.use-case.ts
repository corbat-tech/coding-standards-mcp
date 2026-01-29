import { Inject, Injectable } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { Article } from '../../domain/entity';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/repository';
import { ArticleAlreadyExistsException } from '../../domain/exception';
import { CreateArticleDto, ArticleResponseDto } from '../dto';

@Injectable()
export class CreateArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(dto: CreateArticleDto): Promise<ArticleResponseDto> {
    const existingArticle = await this.articleRepository.findByTitle(dto.title);
    if (existingArticle) {
      throw new ArticleAlreadyExistsException(dto.title);
    }

    const article = new Article({
      id: uuidv4(),
      title: dto.title,
      content: dto.content,
      author: dto.author,
      tags: dto.tags ?? [],
      published: dto.published ?? false,
    });

    const savedArticle = await this.articleRepository.save(article);
    return ArticleResponseDto.fromEntity(savedArticle);
  }
}
