import { Article } from '../entities/article.entity';

/**
 * ArticleRepository Interface - Domain layer port
 * Defines the contract for article persistence operations
 */
export interface ArticleRepository {
  /**
   * Save an article to the repository
   * @param article - The article to save
   * @returns The saved article
   */
  save(article: Article): Promise<Article>;

  /**
   * Find an article by its ID
   * @param id - The article ID
   * @returns The article if found, null otherwise
   */
  findById(id: string): Promise<Article | null>;

  /**
   * Find all articles
   * @returns Array of all articles
   */
  findAll(): Promise<Article[]>;

  /**
   * Find articles by author ID
   * @param authorId - The author's ID
   * @returns Array of articles by the author
   */
  findByAuthorId(authorId: string): Promise<Article[]>;

  /**
   * Update an existing article
   * @param article - The article with updated data
   * @returns The updated article
   */
  update(article: Article): Promise<Article>;

  /**
   * Delete an article by its ID
   * @param id - The article ID
   * @returns True if deleted, false if not found
   */
  delete(id: string): Promise<boolean>;

  /**
   * Check if an article exists
   * @param id - The article ID
   * @returns True if exists, false otherwise
   */
  exists(id: string): Promise<boolean>;
}

export const ARTICLE_REPOSITORY = Symbol('ARTICLE_REPOSITORY');
