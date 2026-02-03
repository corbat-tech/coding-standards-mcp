import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Inject,
  HttpCode,
  HttpStatus,
  Query,
} from '@nestjs/common';
import { ArticleService, ARTICLE_SERVICE } from '../../application/article.service';
import { CreateArticleDto } from '../../application/dto/create-article.dto';
import { UpdateArticleDto } from '../../application/dto/update-article.dto';
import { ArticleResponseDto } from '../../application/dto/article-response.dto';

@Controller('articles')
export class ArticleController {
  constructor(
    @Inject(ARTICLE_SERVICE) private readonly articleService: ArticleService
  ) {}

  @Post()
  async create(@Body() dto: CreateArticleDto): Promise<ArticleResponseDto> {
    const article = await this.articleService.create(dto);
    return new ArticleResponseDto(article);
  }

  @Get()
  async getAll(@Query('author') author?: string): Promise<ArticleResponseDto[]> {
    const articles = author
      ? await this.articleService.getByAuthor(author)
      : await this.articleService.getAll();
    return articles.map((a) => new ArticleResponseDto(a));
  }

  @Get(':id')
  async getById(@Param('id') id: string): Promise<ArticleResponseDto> {
    const article = await this.articleService.getById(id);
    return new ArticleResponseDto(article);
  }

  @Put(':id')
  async update(@Param('id') id: string, @Body() dto: UpdateArticleDto): Promise<ArticleResponseDto> {
    const article = await this.articleService.update(id, dto);
    return new ArticleResponseDto(article);
  }

  @Post(':id/publish')
  async publish(@Param('id') id: string): Promise<ArticleResponseDto> {
    const article = await this.articleService.publish(id);
    return new ArticleResponseDto(article);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async delete(@Param('id') id: string): Promise<void> {
    await this.articleService.delete(id);
  }
}
