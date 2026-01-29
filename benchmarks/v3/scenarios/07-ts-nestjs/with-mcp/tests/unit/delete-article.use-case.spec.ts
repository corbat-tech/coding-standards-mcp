import { DeleteArticleUseCaseImpl } from '../../application/use-cases/delete-article.use-case';
import { ArticleRepository } from '../../domain/interfaces/article.repository.interface';
import { ArticleNotFoundException } from '../../domain/exceptions/article.exceptions';

describe('DeleteArticleUseCase', () => {
  let useCase: DeleteArticleUseCaseImpl;
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
    useCase = new DeleteArticleUseCaseImpl(mockRepository);
  });

  describe('execute', () => {
    it('should delete article successfully', async () => {
      mockRepository.exists.mockResolvedValue(true);
      mockRepository.delete.mockResolvedValue(true);

      await expect(useCase.execute('article-123')).resolves.not.toThrow();

      expect(mockRepository.exists).toHaveBeenCalledWith('article-123');
      expect(mockRepository.delete).toHaveBeenCalledWith('article-123');
    });

    it('should throw ArticleNotFoundException when article does not exist', async () => {
      mockRepository.exists.mockResolvedValue(false);

      await expect(useCase.execute('non-existent-id')).rejects.toThrow(
        ArticleNotFoundException,
      );
      await expect(useCase.execute('non-existent-id')).rejects.toThrow(
        "Article with id 'non-existent-id' not found",
      );
      expect(mockRepository.delete).not.toHaveBeenCalled();
    });
  });
});
