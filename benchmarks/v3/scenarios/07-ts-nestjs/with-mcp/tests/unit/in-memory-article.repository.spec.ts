import { InMemoryArticleRepository } from '../../infrastructure/repositories/in-memory-article.repository';
import { Article } from '../../domain/entities/article.entity';

describe('InMemoryArticleRepository', () => {
  let repository: InMemoryArticleRepository;

  beforeEach(() => {
    repository = new InMemoryArticleRepository();
  });

  describe('save', () => {
    it('should save and return the article', async () => {
      const article = new Article({
        title: 'Test Article',
        content: 'Test content',
        authorId: 'author-123',
      });

      const result = await repository.save(article);

      expect(result).toBe(article);
      const found = await repository.findById(article.id);
      expect(found).toBe(article);
    });
  });

  describe('findById', () => {
    it('should return article when found', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Test Article',
        content: 'Test content',
        authorId: 'author-123',
      });
      await repository.save(article);

      const result = await repository.findById('article-123');

      expect(result).toBe(article);
    });

    it('should return null when article not found', async () => {
      const result = await repository.findById('non-existent');

      expect(result).toBeNull();
    });
  });

  describe('findAll', () => {
    it('should return all articles', async () => {
      const article1 = new Article({
        id: 'article-1',
        title: 'Article 1',
        content: 'Content 1',
        authorId: 'author-1',
      });
      const article2 = new Article({
        id: 'article-2',
        title: 'Article 2',
        content: 'Content 2',
        authorId: 'author-2',
      });
      await repository.save(article1);
      await repository.save(article2);

      const result = await repository.findAll();

      expect(result).toHaveLength(2);
      expect(result).toContain(article1);
      expect(result).toContain(article2);
    });

    it('should return empty array when no articles exist', async () => {
      const result = await repository.findAll();

      expect(result).toEqual([]);
    });
  });

  describe('findByAuthorId', () => {
    it('should return articles by author', async () => {
      const authorId = 'author-123';
      const article1 = new Article({
        id: 'article-1',
        title: 'Article 1',
        content: 'Content 1',
        authorId,
      });
      const article2 = new Article({
        id: 'article-2',
        title: 'Article 2',
        content: 'Content 2',
        authorId,
      });
      const article3 = new Article({
        id: 'article-3',
        title: 'Article 3',
        content: 'Content 3',
        authorId: 'other-author',
      });
      await repository.save(article1);
      await repository.save(article2);
      await repository.save(article3);

      const result = await repository.findByAuthorId(authorId);

      expect(result).toHaveLength(2);
      expect(result).toContain(article1);
      expect(result).toContain(article2);
      expect(result).not.toContain(article3);
    });

    it('should return empty array when author has no articles', async () => {
      const result = await repository.findByAuthorId('non-existent-author');

      expect(result).toEqual([]);
    });
  });

  describe('update', () => {
    it('should update and return the article', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Original Title',
        content: 'Original Content',
        authorId: 'author-123',
      });
      await repository.save(article);

      article.update('Updated Title', 'Updated Content');
      const result = await repository.update(article);

      expect(result.title).toBe('Updated Title');
      expect(result.content).toBe('Updated Content');
      const found = await repository.findById('article-123');
      expect(found?.title).toBe('Updated Title');
    });
  });

  describe('delete', () => {
    it('should delete article and return true', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Test Article',
        content: 'Test content',
        authorId: 'author-123',
      });
      await repository.save(article);

      const result = await repository.delete('article-123');

      expect(result).toBe(true);
      const found = await repository.findById('article-123');
      expect(found).toBeNull();
    });

    it('should return false when article does not exist', async () => {
      const result = await repository.delete('non-existent');

      expect(result).toBe(false);
    });
  });

  describe('exists', () => {
    it('should return true when article exists', async () => {
      const article = new Article({
        id: 'article-123',
        title: 'Test Article',
        content: 'Test content',
        authorId: 'author-123',
      });
      await repository.save(article);

      const result = await repository.exists('article-123');

      expect(result).toBe(true);
    });

    it('should return false when article does not exist', async () => {
      const result = await repository.exists('non-existent');

      expect(result).toBe(false);
    });
  });
});
