import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { Inject } from '@nestjs/common';
import {
  ArticleService,
  ARTICLE_SERVICE,
} from '../../domain/interfaces/article.service.interface';
import { CreateArticleDto } from '../../application/dtos/create-article.dto';
import { UpdateArticleDto } from '../../application/dtos/update-article.dto';
import { ArticleResponseDto } from '../../application/dtos/article-response.dto';

/**
 * REST Controller for Article operations
 */
@Controller('articles')
export class ArticleController {
  constructor(
    @Inject(ARTICLE_SERVICE)
    private readonly articleService: ArticleService,
  ) {}

  /**
   * Create a new article
   * POST /articles
   */
  @Post()
  @HttpCode(HttpStatus.CREATED)
  async create(@Body() dto: CreateArticleDto): Promise<ArticleResponseDto> {
    return this.articleService.create(dto);
  }

  /**
   * Get article by ID
   * GET /articles/:id
   */
  @Get(':id')
  async getById(@Param('id') id: string): Promise<ArticleResponseDto> {
    return this.articleService.getById(id);
  }

  /**
   * Get all articles
   * GET /articles
   */
  @Get()
  async getAll(): Promise<ArticleResponseDto[]> {
    return this.articleService.getAll();
  }

  /**
   * Get articles by author
   * GET /articles/author/:authorId
   */
  @Get('author/:authorId')
  async getByAuthor(
    @Param('authorId') authorId: string,
  ): Promise<ArticleResponseDto[]> {
    return this.articleService.getByAuthor(authorId);
  }

  /**
   * Update an article
   * PUT /articles/:id
   */
  @Put(':id')
  async update(
    @Param('id') id: string,
    @Body() dto: UpdateArticleDto,
  ): Promise<ArticleResponseDto> {
    return this.articleService.update(id, dto);
  }

  /**
   * Delete an article
   * DELETE /articles/:id
   */
  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async delete(@Param('id') id: string): Promise<void> {
    return this.articleService.delete(id);
  }
}
