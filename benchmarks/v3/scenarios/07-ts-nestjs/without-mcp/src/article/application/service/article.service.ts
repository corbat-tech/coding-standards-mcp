import { Injectable } from '@nestjs/common';
import { ArticleServiceInterface } from '../port';
import { CreateArticleDto, UpdateArticleDto, ArticleResponseDto } from '../dto';
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
} from '../use-case';

@Injectable()
export class ArticleService implements ArticleServiceInterface {
  constructor(
    private readonly createArticleUseCase: CreateArticleUseCase,
    private readonly updateArticleUseCase: UpdateArticleUseCase,
    private readonly deleteArticleUseCase: DeleteArticleUseCase,
    private readonly getArticleUseCase: GetArticleUseCase,
    private readonly getAllArticlesUseCase: GetAllArticlesUseCase,
    private readonly getArticlesByAuthorUseCase: GetArticlesByAuthorUseCase,
    private readonly getArticlesByTagUseCase: GetArticlesByTagUseCase,
    private readonly getPublishedArticlesUseCase: GetPublishedArticlesUseCase,
    private readonly publishArticleUseCase: PublishArticleUseCase,
    private readonly unpublishArticleUseCase: UnpublishArticleUseCase,
  ) {}

  async createArticle(dto: CreateArticleDto): Promise<ArticleResponseDto> {
    return this.createArticleUseCase.execute(dto);
  }

  async updateArticle(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto> {
    return this.updateArticleUseCase.execute(id, dto);
  }

  async deleteArticle(id: string): Promise<void> {
    return this.deleteArticleUseCase.execute(id);
  }

  async getArticleById(id: string): Promise<ArticleResponseDto> {
    return this.getArticleUseCase.execute(id);
  }

  async getAllArticles(): Promise<ArticleResponseDto[]> {
    return this.getAllArticlesUseCase.execute();
  }

  async getArticlesByAuthor(author: string): Promise<ArticleResponseDto[]> {
    return this.getArticlesByAuthorUseCase.execute(author);
  }

  async getArticlesByTag(tag: string): Promise<ArticleResponseDto[]> {
    return this.getArticlesByTagUseCase.execute(tag);
  }

  async getPublishedArticles(): Promise<ArticleResponseDto[]> {
    return this.getPublishedArticlesUseCase.execute();
  }

  async publishArticle(id: string): Promise<ArticleResponseDto> {
    return this.publishArticleUseCase.execute(id);
  }

  async unpublishArticle(id: string): Promise<ArticleResponseDto> {
    return this.unpublishArticleUseCase.execute(id);
  }
}
