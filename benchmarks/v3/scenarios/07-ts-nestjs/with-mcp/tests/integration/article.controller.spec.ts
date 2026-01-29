import { Test, TestingModule } from '@nestjs/testing';
import { ArticleController } from '../../infrastructure/controllers/article.controller';
import { ArticleServiceImpl } from '../../infrastructure/services/article.service.impl';
import { InMemoryArticleRepository } from '../../infrastructure/repositories/in-memory-article.repository';
import { ARTICLE_REPOSITORY } from '../../domain/interfaces/article.repository.interface';
import { ARTICLE_SERVICE } from '../../domain/interfaces/article.service.interface';
import { CreateArticleDto } from '../../application/dtos/create-article.dto';
import { UpdateArticleDto } from '../../application/dtos/update-article.dto';
import { ArticleNotFoundException } from '../../domain/exceptions/article.exceptions';

describe('ArticleController (Integration)', () => {
  let controller: ArticleController;
  let module: TestingModule;

  beforeEach(async () => {
    module = await Test.createTestingModule({
      controllers: [ArticleController],
      providers: [
        {
          provide: ARTICLE_REPOSITORY,
          useClass: InMemoryArticleRepository,
        },
        {
          provide: ARTICLE_SERVICE,
          useClass: ArticleServiceImpl,
        },
      ],
    }).compile();

    controller = module.get<ArticleController>(ArticleController);
  });

  afterEach(async () => {
    await module.close();
  });

  describe('create', () => {
    it('should create and return article', async () => {
      const dto: CreateArticleDto = {
        title: 'Test Article',
        content: 'This is test content for integration test',
        authorId: 'author-123',
        tags: ['test', 'integration'],
      };

      const result = await controller.create(dto);

      expect(result.title).toBe(dto.title);
      expect(result.content).toBe(dto.content);
      expect(result.authorId).toBe(dto.authorId);
      expect(result.tags).toEqual(dto.tags);
      expect(result.id).toBeDefined();
    });
  });

  describe('getById', () => {
    it('should return article by id', async () => {
      const dto: CreateArticleDto = {
        title: 'Test Article',
        content: 'This is test content for integration test',
        authorId: 'author-123',
      };
      const created = await controller.create(dto);

      const result = await controller.getById(created.id);

      expect(result.id).toBe(created.id);
      expect(result.title).toBe(dto.title);
    });

    it('should throw ArticleNotFoundException for non-existent id', async () => {
      await expect(controller.getById('non-existent')).rejects.toThrow(
        ArticleNotFoundException,
      );
    });
  });

  describe('getAll', () => {
    it('should return all articles', async () => {
      await controller.create({
        title: 'Article 1',
        content: 'Content for article 1',
        authorId: 'author-1',
      });
      await controller.create({
        title: 'Article 2',
        content: 'Content for article 2',
        authorId: 'author-2',
      });

      const result = await controller.getAll();

      expect(result).toHaveLength(2);
    });

    it('should return empty array when no articles', async () => {
      const result = await controller.getAll();

      expect(result).toEqual([]);
    });
  });

  describe('getByAuthor', () => {
    it('should return articles by author', async () => {
      const authorId = 'author-123';
      await controller.create({
        title: 'Article 1',
        content: 'Content for article 1',
        authorId,
      });
      await controller.create({
        title: 'Article 2',
        content: 'Content for article 2',
        authorId,
      });
      await controller.create({
        title: 'Article 3',
        content: 'Content for article 3',
        authorId: 'other-author',
      });

      const result = await controller.getByAuthor(authorId);

      expect(result).toHaveLength(2);
      expect(result.every((a) => a.authorId === authorId)).toBe(true);
    });
  });

  describe('update', () => {
    it('should update and return article', async () => {
      const created = await controller.create({
        title: 'Original Title',
        content: 'Original content for test',
        authorId: 'author-123',
      });
      const updateDto: UpdateArticleDto = {
        title: 'Updated Title',
        content: 'Updated content for test',
      };

      const result = await controller.update(created.id, updateDto);

      expect(result.title).toBe('Updated Title');
      expect(result.content).toBe('Updated content for test');
      expect(result.id).toBe(created.id);
    });

    it('should throw ArticleNotFoundException for non-existent id', async () => {
      await expect(
        controller.update('non-existent', { title: 'New Title' }),
      ).rejects.toThrow(ArticleNotFoundException);
    });
  });

  describe('delete', () => {
    it('should delete article successfully', async () => {
      const created = await controller.create({
        title: 'To Delete',
        content: 'Content to be deleted',
        authorId: 'author-123',
      });

      await expect(controller.delete(created.id)).resolves.not.toThrow();
      await expect(controller.getById(created.id)).rejects.toThrow(
        ArticleNotFoundException,
      );
    });

    it('should throw ArticleNotFoundException for non-existent id', async () => {
      await expect(controller.delete('non-existent')).rejects.toThrow(
        ArticleNotFoundException,
      );
    });
  });
});
