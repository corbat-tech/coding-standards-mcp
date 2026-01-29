import { UpdateArticleUseCaseImpl } from '../../application/use-cases/update-article.use-case';
import { ArticleRepository } from '../../domain/interfaces/article.repository.interface';
import { Article } from '../../domain/entities/article.entity';
import { UpdateArticleDto } from '../../application/dtos/update-article.dto';
import {
  ArticleNotFoundException,
  ArticleValidationException,
} from '../../domain/exceptions/article.exceptions';

describe('UpdateArticleUseCase', () => {
  let useCase: UpdateArticleUseCaseImpl;
  let mockRepository: jest.Mocked<ArticleRepository>;

  beforeEach(() => {
    mockRepository = {
      save: jest.fn(),
      findById: jest.fn(),
      findAll: jest.fn(),
      findByAuthorId: jest.fn(),
      update: jest.fn(),
      delete: jest.fn(),
      exists: jest.fn(),
    };
    useCase = new UpdateArticleUseCaseImpl(mockRepository);
  });

  describe('execute', () => {
    const existingArticle = new Article({
      id: 'article-123',
      title: 'Original Title',
      content: 'Original Content',
      authorId: 'author-123',
      tags: ['original'],
    });

    it('should update article successfully', async () => {
      const updateDto: UpdateArticleDto = {
        title: 'Updated Title',
        content: 'Updated Content',
        tags: ['updated'],
      };
      mockRepository.findById.mockResolvedValue(existingArticle);
      mockRepository.update.mockImplementation(async (article) => article);

      const result = await useCase.execute('article-123', updateDto);

      expect(result.title).toBe('Updated Title');
      expect(result.content).toBe('Updated Content');
      expect(result.tags).toEqual(['updated']);
      expect(mockRepository.update).toHaveBeenCalledTimes(1);
    });

    it('should update only title when only title provided', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Original Title',
        content: 'Original Content',
        authorId: 'author-123',
        tags: ['original'],
      });
      const updateDto: UpdateArticleDto = {
        title: 'Updated Title',
      };
      mockRepository.findById.mockResolvedValue(article);
      mockRepository.update.mockImplementation(async (a) => a);

      const result = await useCase.execute('article-123', updateDto);

      expect(result.title).toBe('Updated Title');
      expect(result.content).toBe('Original Content');
    });

    it('should update only content when only content provided', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Original Title',
        content: 'Original Content',
        authorId: 'author-123',
      });
      const updateDto: UpdateArticleDto = {
        content: 'Updated Content',
      };
      mockRepository.findById.mockResolvedValue(article);
      mockRepository.update.mockImplementation(async (a) => a);

      const result = await useCase.execute('article-123', updateDto);

      expect(result.title).toBe('Original Title');
      expect(result.content).toBe('Updated Content');
    });

    it('should throw ArticleNotFoundException when article not found', async () => {
      mockRepository.findById.mockResolvedValue(null);

      await expect(
        useCase.execute('non-existent', { title: 'New Title' }),
      ).rejects.toThrow(ArticleNotFoundException);
    });

    it('should throw ArticleValidationException when title is empty string', async () => {
      mockRepository.findById.mockResolvedValue(existingArticle);

      await expect(
        useCase.execute('article-123', { title: '' }),
      ).rejects.toThrow(ArticleValidationException);
      await expect(
        useCase.execute('article-123', { title: '' }),
      ).rejects.toThrow('Title cannot be empty');
    });

    it('should throw ArticleValidationException when content is empty string', async () => {
      mockRepository.findById.mockResolvedValue(existingArticle);

      await expect(
        useCase.execute('article-123', { content: '' }),
      ).rejects.toThrow(ArticleValidationException);
      await expect(
        useCase.execute('article-123', { content: '' }),
      ).rejects.toThrow('Content cannot be empty');
    });

    it('should trim title and content', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Original Title',
        content: 'Original Content',
        authorId: 'author-123',
      });
      mockRepository.findById.mockResolvedValue(article);
      mockRepository.update.mockImplementation(async (a) => a);

      const result = await useCase.execute('article-123', {
        title: '  Trimmed Title  ',
        content: '  Trimmed Content  ',
      });

      expect(result.title).toBe('Trimmed Title');
      expect(result.content).toBe('Trimmed Content');
    });
  });
});
