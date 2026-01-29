import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  Inject,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ArticleServiceInterface,
  ARTICLE_SERVICE,
} from './application/port';
import {
  CreateArticleDto,
  UpdateArticleDto,
  ArticleResponseDto,
} from './application/dto';

@Controller('articles')
export class ArticleController {
  constructor(
    @Inject(ARTICLE_SERVICE)
    private readonly articleService: ArticleServiceInterface,
  ) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  async create(@Body() dto: CreateArticleDto): Promise<ArticleResponseDto> {
    return this.articleService.createArticle(dto);
  }

  @Get()
  async findAll(): Promise<ArticleResponseDto[]> {
    return this.articleService.getAllArticles();
  }

  @Get('published')
  async findPublished(): Promise<ArticleResponseDto[]> {
    return this.articleService.getPublishedArticles();
  }

  @Get('by-author')
  async findByAuthor(@Query('author') author: string): Promise<ArticleResponseDto[]> {
    return this.articleService.getArticlesByAuthor(author);
  }

  @Get('by-tag')
  async findByTag(@Query('tag') tag: string): Promise<ArticleResponseDto[]> {
    return this.articleService.getArticlesByTag(tag);
  }

  @Get(':id')
  async findOne(@Param('id') id: string): Promise<ArticleResponseDto> {
    return this.articleService.getArticleById(id);
  }

  @Put(':id')
  async update(
    @Param('id') id: string,
    @Body() dto: UpdateArticleDto,
  ): Promise<ArticleResponseDto> {
    return this.articleService.updateArticle(id, dto);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async delete(@Param('id') id: string): Promise<void> {
    return this.articleService.deleteArticle(id);
  }

  @Post(':id/publish')
  async publish(@Param('id') id: string): Promise<ArticleResponseDto> {
    return this.articleService.publishArticle(id);
  }

  @Post(':id/unpublish')
  async unpublish(@Param('id') id: string): Promise<ArticleResponseDto> {
    return this.articleService.unpublishArticle(id);
  }
}
