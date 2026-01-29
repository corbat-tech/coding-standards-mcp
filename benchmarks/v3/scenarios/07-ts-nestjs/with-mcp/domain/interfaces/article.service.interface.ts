import { Article } from '../entities/article.entity';
import { CreateArticleDto } from '../../application/dtos/create-article.dto';
import { UpdateArticleDto } from '../../application/dtos/update-article.dto';
import { ArticleResponseDto } from '../../application/dtos/article-response.dto';

/**
 * ArticleService Interface - Application layer port
 * Defines the contract for article business operations
 */
export interface ArticleService {
  /**
   * Create a new article
   * @param dto - The article creation data
   * @returns The created article response
   */
  create(dto: CreateArticleDto): Promise<ArticleResponseDto>;

  /**
   * Get an article by ID
   * @param id - The article ID
   * @returns The article response
   * @throws ArticleNotFoundException if not found
   */
  getById(id: string): Promise<ArticleResponseDto>;

  /**
   * Get all articles
   * @returns Array of article responses
   */
  getAll(): Promise<ArticleResponseDto[]>;

  /**
   * Get articles by author
   * @param authorId - The author's ID
   * @returns Array of article responses
   */
  getByAuthor(authorId: string): Promise<ArticleResponseDto[]>;

  /**
   * Update an existing article
   * @param id - The article ID
   * @param dto - The update data
   * @returns The updated article response
   * @throws ArticleNotFoundException if not found
   */
  update(id: string, dto: UpdateArticleDto): Promise<ArticleResponseDto>;

  /**
   * Delete an article
   * @param id - The article ID
   * @throws ArticleNotFoundException if not found
   */
  delete(id: string): Promise<void>;
}

export const ARTICLE_SERVICE = Symbol('ARTICLE_SERVICE');
