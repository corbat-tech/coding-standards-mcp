import { Test, TestingModule } from '@nestjs/testing';
import { ArticleServiceImpl } from '../src/article/application/use-cases/article.service.impl';
import { ARTICLE_REPOSITORY } from '../src/article/domain/article.repository';
import { InMemoryArticleRepository } from '../src/article/infrastructure/repository/in-memory-article.repository';
import { ArticleNotFoundException, ArticleAlreadyPublishedException } from '../src/article/domain/article.exceptions';

describe('ArticleServiceImpl', () => {
  let service: ArticleServiceImpl;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ArticleServiceImpl,
        { provide: ARTICLE_REPOSITORY, useClass: InMemoryArticleRepository },
      ],
    }).compile();

    service = module.get<ArticleServiceImpl>(ArticleServiceImpl);
  });

  describe('create', () => {
    it('should create an article', async () => {
      const article = await service.create({
        title: 'Test Article',
        content: 'This is test content',
        author: 'John Doe',
        tags: ['test'],
      });

      expect(article.id).toBeDefined();
      expect(article.title).toBe('Test Article');
      expect(article.published).toBe(false);
    });
  });

  describe('getById', () => {
    it('should return article by id', async () => {
      const created = await service.create({
        title: 'Test',
        content: 'Content here',
        author: 'Author',
      });

      const found = await service.getById(created.id);
      expect(found.id).toBe(created.id);
    });

    it('should throw ArticleNotFoundException for non-existent id', async () => {
      await expect(service.getById('non-existent')).rejects.toThrow(ArticleNotFoundException);
    });
  });

  describe('getAll', () => {
    it('should return all articles', async () => {
      await service.create({ title: 'Article 1', content: 'Content 1', author: 'Author' });
      await service.create({ title: 'Article 2', content: 'Content 2', author: 'Author' });

      const articles = await service.getAll();
      expect(articles).toHaveLength(2);
    });
  });

  describe('getByAuthor', () => {
    it('should return articles by author', async () => {
      await service.create({ title: 'Article 1', content: 'Content', author: 'John' });
      await service.create({ title: 'Article 2', content: 'Content', author: 'Jane' });

      const articles = await service.getByAuthor('John');
      expect(articles).toHaveLength(1);
      expect(articles[0].author).toBe('John');
    });
  });

  describe('update', () => {
    it('should update article', async () => {
      const created = await service.create({
        title: 'Original',
        content: 'Original content',
        author: 'Author',
      });

      const updated = await service.update(created.id, { title: 'Updated' });
      expect(updated.title).toBe('Updated');
      expect(updated.content).toBe('Original content');
    });
  });

  describe('publish', () => {
    it('should publish an article', async () => {
      const created = await service.create({
        title: 'Draft',
        content: 'Draft content',
        author: 'Author',
      });

      const published = await service.publish(created.id);
      expect(published.published).toBe(true);
    });

    it('should throw when publishing already published article', async () => {
      const created = await service.create({
        title: 'Draft',
        content: 'Content',
        author: 'Author',
      });
      await service.publish(created.id);

      await expect(service.publish(created.id)).rejects.toThrow(ArticleAlreadyPublishedException);
    });
  });

  describe('delete', () => {
    it('should delete an article', async () => {
      const created = await service.create({
        title: 'To Delete',
        content: 'Content',
        author: 'Author',
      });

      await service.delete(created.id);
      await expect(service.getById(created.id)).rejects.toThrow(ArticleNotFoundException);
    });
  });
});
