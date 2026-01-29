import { Test, TestingModule } from '@nestjs/testing';
import { CreateArticleUseCase } from '../src/article/application/use-case';
import { ARTICLE_REPOSITORY } from '../src/article/domain/repository';
import { ArticleAlreadyExistsException } from '../src/article/domain/exception';
import { InMemoryArticleRepository } from '../src/article/infrastructure/persistence';
import { CreateArticleDto } from '../src/article/application/dto';

describe('CreateArticleUseCase', () => {
  let useCase: CreateArticleUseCase;
  let repository: InMemoryArticleRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        CreateArticleUseCase,
        {
          provide: ARTICLE_REPOSITORY,
          useClass: InMemoryArticleRepository,
        },
      ],
    }).compile();

    useCase = module.get<CreateArticleUseCase>(CreateArticleUseCase);
    repository = module.get<InMemoryArticleRepository>(ARTICLE_REPOSITORY);
  });

  afterEach(() => {
    repository.clear();
  });

  it('should create an article successfully', async () => {
    const dto: CreateArticleDto = {
      title: 'Test Article',
      content: 'This is test content that is long enough for the article.',
      author: 'John Doe',
      tags: ['test', 'article'],
      published: false,
    };

    const result = await useCase.execute(dto);

    expect(result).toBeDefined();
    expect(result.id).toBeDefined();
    expect(result.title).toBe(dto.title);
    expect(result.content).toBe(dto.content);
    expect(result.author).toBe(dto.author);
    expect(result.tags).toEqual(dto.tags);
    expect(result.published).toBe(false);
    expect(result.createdAt).toBeInstanceOf(Date);
    expect(result.updatedAt).toBeInstanceOf(Date);
  });

  it('should create an article with default values', async () => {
    const dto: CreateArticleDto = {
      title: 'Test Article',
      content: 'This is test content that is long enough for the article.',
      author: 'John Doe',
    };

    const result = await useCase.execute(dto);

    expect(result.tags).toEqual([]);
    expect(result.published).toBe(false);
  });

  it('should throw ArticleAlreadyExistsException if title exists', async () => {
    const dto: CreateArticleDto = {
      title: 'Duplicate Title',
      content: 'This is test content that is long enough for the article.',
      author: 'John Doe',
    };

    await useCase.execute(dto);

    await expect(useCase.execute(dto)).rejects.toThrow(ArticleAlreadyExistsException);
  });

  it('should allow different titles', async () => {
    const dto1: CreateArticleDto = {
      title: 'Article One',
      content: 'Content for article one that is long enough.',
      author: 'John Doe',
    };

    const dto2: CreateArticleDto = {
      title: 'Article Two',
      content: 'Content for article two that is long enough.',
      author: 'Jane Doe',
    };

    const result1 = await useCase.execute(dto1);
    const result2 = await useCase.execute(dto2);

    expect(result1.title).toBe('Article One');
    expect(result2.title).toBe('Article Two');
  });
});
