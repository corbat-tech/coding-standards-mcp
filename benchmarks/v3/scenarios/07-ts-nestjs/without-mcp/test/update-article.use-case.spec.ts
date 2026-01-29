import { Test, TestingModule } from '@nestjs/testing';
import { UpdateArticleUseCase } from '../src/article/application/use-case';
import { ARTICLE_REPOSITORY } from '../src/article/domain/repository';
import {
  ArticleNotFoundException,
  ArticleAlreadyExistsException,
} from '../src/article/domain/exception';
import { InMemoryArticleRepository } from '../src/article/infrastructure/persistence';
import { Article } from '../src/article/domain/entity';

describe('UpdateArticleUseCase', () => {
  let useCase: UpdateArticleUseCase;
  let repository: InMemoryArticleRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UpdateArticleUseCase,
        {
          provide: ARTICLE_REPOSITORY,
          useClass: InMemoryArticleRepository,
        },
      ],
    }).compile();

    useCase = module.get<UpdateArticleUseCase>(UpdateArticleUseCase);
    repository = module.get<InMemoryArticleRepository>(ARTICLE_REPOSITORY);
  });

  afterEach(() => {
    repository.clear();
  });

  it('should update an article successfully', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Original Title',
      content: 'Original content that is long enough for testing.',
      author: 'John Doe',
    });
    await repository.save(article);

    const result = await useCase.execute('test-id', {
      title: 'Updated Title',
      content: 'Updated content that is long enough for testing.',
    });

    expect(result.title).toBe('Updated Title');
    expect(result.content).toBe('Updated content that is long enough for testing.');
    expect(result.author).toBe('John Doe');
  });

  it('should throw ArticleNotFoundException if article does not exist', async () => {
    await expect(
      useCase.execute('non-existent', { title: 'New Title' }),
    ).rejects.toThrow(ArticleNotFoundException);
  });

  it('should throw ArticleAlreadyExistsException if new title already exists', async () => {
    const article1 = new Article({
      id: 'id-1',
      title: 'Title One',
      content: 'Content one',
      author: 'John',
    });
    const article2 = new Article({
      id: 'id-2',
      title: 'Title Two',
      content: 'Content two',
      author: 'Jane',
    });
    await repository.save(article1);
    await repository.save(article2);

    await expect(
      useCase.execute('id-1', { title: 'Title Two' }),
    ).rejects.toThrow(ArticleAlreadyExistsException);
  });

  it('should allow updating to the same title', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Same Title',
      content: 'Original content',
      author: 'John Doe',
    });
    await repository.save(article);

    const result = await useCase.execute('test-id', {
      title: 'Same Title',
      content: 'Updated content',
    });

    expect(result.title).toBe('Same Title');
    expect(result.content).toBe('Updated content');
  });

  it('should update tags', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'Test content',
      author: 'John Doe',
      tags: ['old-tag'],
    });
    await repository.save(article);

    const result = await useCase.execute('test-id', {
      tags: ['new-tag', 'another-tag'],
    });

    expect(result.tags).toEqual(['new-tag', 'another-tag']);
  });
});
