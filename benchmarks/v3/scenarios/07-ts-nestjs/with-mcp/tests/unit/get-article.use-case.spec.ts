import { GetArticleUseCaseImpl } from '../../application/use-cases/get-article.use-case';
import { ArticleRepository } from '../../domain/interfaces/article.repository.interface';
import { Article } from '../../domain/entities/article.entity';
import { ArticleNotFoundException } from '../../domain/exceptions/article.exceptions';

describe('GetArticleUseCase', () => {
  let useCase: GetArticleUseCaseImpl;
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
    useCase = new GetArticleUseCaseImpl(mockRepository);
  });

  describe('execute', () => {
    it('should return article when found', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Test Article',
        content: 'Test content',
        authorId: 'author-123',
      });
      mockRepository.findById.mockResolvedValue(article);

      const result = await useCase.execute('article-123');

      expect(result.id).toBe(article.id);
      expect(result.title).toBe(article.title);
      expect(result.content).toBe(article.content);
      expect(mockRepository.findById).toHaveBeenCalledWith('article-123');
    });

    it('should throw ArticleNotFoundException when article not found', async () => {
      mockRepository.findById.mockResolvedValue(null);

      await expect(useCase.execute('non-existent-id')).rejects.toThrow(
        ArticleNotFoundException,
      );
      await expect(useCase.execute('non-existent-id')).rejects.toThrow(
        "Article with id 'non-existent-id' not found",
      );
    });
  });
});
