import { ListArticlesUseCaseImpl } from '../../application/use-cases/list-articles.use-case';
import { ArticleRepository } from '../../domain/interfaces/article.repository.interface';
import { Article } from '../../domain/entities/article.entity';

describe('ListArticlesUseCase', () => {
  let useCase: ListArticlesUseCaseImpl;
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
    useCase = new ListArticlesUseCaseImpl(mockRepository);
  });

  describe('execute', () => {
    it('should return all articles', async () => {
      const articles = [
        new Article({
          id: 'article-1',
          title: 'Article 1',
          content: 'Content 1',
          authorId: 'author-1',
        }),
        new Article({
          id: 'article-2',
          title: 'Article 2',
          content: 'Content 2',
          authorId: 'author-2',
        }),
      ];
      mockRepository.findAll.mockResolvedValue(articles);

      const result = await useCase.execute();

      expect(result).toHaveLength(2);
      expect(result[0].id).toBe('article-1');
      expect(result[1].id).toBe('article-2');
      expect(mockRepository.findAll).toHaveBeenCalledTimes(1);
    });

    it('should return empty array when no articles exist', async () => {
      mockRepository.findAll.mockResolvedValue([]);

      const result = await useCase.execute();

      expect(result).toEqual([]);
    });
  });

  describe('executeByAuthor', () => {
    it('should return articles by specific author', async () => {
      const authorId = 'author-123';
      const articles = [
        new Article({
          id: 'article-1',
          title: 'Article 1',
          content: 'Content 1',
          authorId,
        }),
        new Article({
          id: 'article-2',
          title: 'Article 2',
          content: 'Content 2',
          authorId,
        }),
      ];
      mockRepository.findByAuthorId.mockResolvedValue(articles);

      const result = await useCase.executeByAuthor(authorId);

      expect(result).toHaveLength(2);
      expect(result.every((a) => a.authorId === authorId)).toBe(true);
      expect(mockRepository.findByAuthorId).toHaveBeenCalledWith(authorId);
    });

    it('should return empty array when author has no articles', async () => {
      mockRepository.findByAuthorId.mockResolvedValue([]);

      const result = await useCase.executeByAuthor('author-with-no-articles');

      expect(result).toEqual([]);
    });
  });
});
