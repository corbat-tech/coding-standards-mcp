import { Test, TestingModule } from '@nestjs/testing';
import {
  PublishArticleUseCase,
  UnpublishArticleUseCase,
} from '../src/article/application/use-case';
import { ARTICLE_REPOSITORY } from '../src/article/domain/repository';
import {
  ArticleNotFoundException,
  ArticlePublishException,
} from '../src/article/domain/exception';
import { InMemoryArticleRepository } from '../src/article/infrastructure/persistence';
import { Article } from '../src/article/domain/entity';

describe('PublishArticleUseCase', () => {
  let useCase: PublishArticleUseCase;
  let repository: InMemoryArticleRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        PublishArticleUseCase,
        {
          provide: ARTICLE_REPOSITORY,
          useClass: InMemoryArticleRepository,
        },
      ],
    }).compile();

    useCase = module.get<PublishArticleUseCase>(PublishArticleUseCase);
    repository = module.get<InMemoryArticleRepository>(ARTICLE_REPOSITORY);
  });

  afterEach(() => {
    repository.clear();
  });

  it('should publish an article successfully', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'A'.repeat(100), // Content with at least 100 characters
      author: 'John Doe',
      published: false,
    });
    await repository.save(article);

    const result = await useCase.execute('test-id');

    expect(result.published).toBe(true);
  });

  it('should throw ArticleNotFoundException if article does not exist', async () => {
    await expect(useCase.execute('non-existent')).rejects.toThrow(
      ArticleNotFoundException,
    );
  });

  it('should throw ArticlePublishException if article is already published', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'A'.repeat(100),
      author: 'John Doe',
      published: true,
    });
    await repository.save(article);

    await expect(useCase.execute('test-id')).rejects.toThrow(
      ArticlePublishException,
    );
  });

  it('should throw ArticlePublishException if content is too short', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'Short content', // Less than 100 characters
      author: 'John Doe',
      published: false,
    });
    await repository.save(article);

    await expect(useCase.execute('test-id')).rejects.toThrow(
      ArticlePublishException,
    );
  });
});

describe('UnpublishArticleUseCase', () => {
  let useCase: UnpublishArticleUseCase;
  let repository: InMemoryArticleRepository;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UnpublishArticleUseCase,
        {
          provide: ARTICLE_REPOSITORY,
          useClass: InMemoryArticleRepository,
        },
      ],
    }).compile();

    useCase = module.get<UnpublishArticleUseCase>(UnpublishArticleUseCase);
    repository = module.get<InMemoryArticleRepository>(ARTICLE_REPOSITORY);
  });

  afterEach(() => {
    repository.clear();
  });

  it('should unpublish an article successfully', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'Test content',
      author: 'John Doe',
      published: true,
    });
    await repository.save(article);

    const result = await useCase.execute('test-id');

    expect(result.published).toBe(false);
  });

  it('should throw ArticleNotFoundException if article does not exist', async () => {
    await expect(useCase.execute('non-existent')).rejects.toThrow(
      ArticleNotFoundException,
    );
  });

  it('should throw ArticlePublishException if article is not published', async () => {
    const article = new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'Test content',
      author: 'John Doe',
      published: false,
    });
    await repository.save(article);

    await expect(useCase.execute('test-id')).rejects.toThrow(
      ArticlePublishException,
    );
  });
});
