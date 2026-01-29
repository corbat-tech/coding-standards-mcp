import { CreateArticleUseCaseImpl } from '../../application/use-cases/create-article.use-case';
import { ArticleRepository } from '../../domain/interfaces/article.repository.interface';
import { CreateArticleDto } from '../../application/dtos/create-article.dto';
import { Article } from '../../domain/entities/article.entity';
import { ArticleValidationException } from '../../domain/exceptions/article.exceptions';

describe('CreateArticleUseCase', () => {
  let useCase: CreateArticleUseCaseImpl;
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
    useCase = new CreateArticleUseCaseImpl(mockRepository);
  });

  describe('execute', () => {
    const validDto: CreateArticleDto = {
      title: 'Test Article',
      content: 'This is valid content for testing',
      authorId: 'author-123',
      tags: ['test'],
    };

    it('should create article with valid data', async () => {
      mockRepository.save.mockImplementation(async (article) => article);

      const result = await useCase.execute(validDto);

      expect(result.title).toBe(validDto.title);
      expect(result.content).toBe(validDto.content);
      expect(result.authorId).toBe(validDto.authorId);
      expect(result.tags).toEqual(validDto.tags);
      expect(mockRepository.save).toHaveBeenCalledTimes(1);
    });

    it('should trim title and content', async () => {
      const dtoWithWhitespace: CreateArticleDto = {
        ...validDto,
        title: '  Trimmed Title  ',
        content: '  Trimmed Content  ',
      };
      mockRepository.save.mockImplementation(async (article) => article);

      const result = await useCase.execute(dtoWithWhitespace);

      expect(result.title).toBe('Trimmed Title');
      expect(result.content).toBe('Trimmed Content');
    });

    it('should use empty array when tags not provided', async () => {
      const dtoWithoutTags: CreateArticleDto = {
        title: 'Test Article',
        content: 'This is valid content for testing',
        authorId: 'author-123',
      };
      mockRepository.save.mockImplementation(async (article) => article);

      const result = await useCase.execute(dtoWithoutTags);

      expect(result.tags).toEqual([]);
    });

    it('should throw ArticleValidationException when title is empty', async () => {
      const invalidDto: CreateArticleDto = {
        ...validDto,
        title: '',
      };

      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        ArticleValidationException,
      );
      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        'Title cannot be empty',
      );
    });

    it('should throw ArticleValidationException when title is only whitespace', async () => {
      const invalidDto: CreateArticleDto = {
        ...validDto,
        title: '   ',
      };

      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        ArticleValidationException,
      );
    });

    it('should throw ArticleValidationException when content is empty', async () => {
      const invalidDto: CreateArticleDto = {
        ...validDto,
        content: '',
      };

      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        ArticleValidationException,
      );
      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        'Content cannot be empty',
      );
    });

    it('should throw ArticleValidationException when authorId is empty', async () => {
      const invalidDto: CreateArticleDto = {
        ...validDto,
        authorId: '',
      };

      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        ArticleValidationException,
      );
      await expect(useCase.execute(invalidDto)).rejects.toThrow(
        'Author ID cannot be empty',
      );
    });
  });
});
