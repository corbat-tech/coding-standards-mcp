import { Inject, Injectable } from '@nestjs/common';
import {
  ArticleRepository,
  ARTICLE_REPOSITORY,
} from '../../domain/repository';
import {
  ArticleNotFoundException,
  ArticleAlreadyExistsException,
} from '../../domain/exception';
import { UpdateArticleDto, ArticleResponseDto } from '../dto';

@Injectable()
export class UpdateArticleUseCase {
  constructor(
    @Inject(ARTICLE_REPOSITORY)
    private readonly articleRepository: ArticleRepository,
  ) {}

  async execute(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto> {
    const existingArticle = await this.articleRepository.findById(id);
    if (!existingArticle) {
      throw new ArticleNotFoundException(id);
    }

    if (dto.title && dto.title !== existingArticle.title) {
      const articleWithTitle = await this.articleRepository.findByTitle(dto.title);
      if (articleWithTitle) {
        throw new ArticleAlreadyExistsException(dto.title);
      }
    }

    const updatedArticle = existingArticle.update({
      title: dto.title,
      content: dto.content,
      author: dto.author,
      tags: dto.tags,
      published: dto.published,
    });

    const savedArticle = await this.articleRepository.update(updatedArticle);
    return ArticleResponseDto.fromEntity(savedArticle);
  }
}
