import { Article } from '../src/article/domain/entity';

describe('Article Entity', () => {
  const createTestArticle = (overrides?: Partial<ConstructorParameters<typeof Article>[0]>) => {
    return new Article({
      id: 'test-id',
      title: 'Test Article',
      content: 'This is a test article content that is long enough.',
      author: 'Test Author',
      tags: ['test', 'article'],
      published: false,
      ...overrides,
    });
  };

  describe('constructor', () => {
    it('should create an article with all properties', () => {
      const now = new Date();
      const article = new Article({
        id: '123',
        title: 'My Article',
        content: 'Article content',
        author: 'John Doe',
        tags: ['tech', 'news'],
        published: true,
        createdAt: now,
        updatedAt: now,
      });

      expect(article.id).toBe('123');
      expect(article.title).toBe('My Article');
      expect(article.content).toBe('Article content');
      expect(article.author).toBe('John Doe');
      expect(article.tags).toEqual(['tech', 'news']);
      expect(article.published).toBe(true);
      expect(article.createdAt).toBe(now);
      expect(article.updatedAt).toBe(now);
    });

    it('should create an article with default values', () => {
      const article = new Article({
        id: '123',
        title: 'My Article',
        content: 'Article content',
        author: 'John Doe',
      });

      expect(article.tags).toEqual([]);
      expect(article.published).toBe(false);
      expect(article.createdAt).toBeInstanceOf(Date);
      expect(article.updatedAt).toBeInstanceOf(Date);
    });
  });

  describe('update', () => {
    it('should update article properties', () => {
      const article = createTestArticle();
      const updated = article.update({
        title: 'Updated Title',
        content: 'Updated content',
      });

      expect(updated.title).toBe('Updated Title');
      expect(updated.content).toBe('Updated content');
      expect(updated.author).toBe(article.author);
      expect(updated.id).toBe(article.id);
      expect(updated.createdAt).toBe(article.createdAt);
      expect(updated.updatedAt).not.toBe(article.updatedAt);
    });

    it('should preserve original article immutability', () => {
      const article = createTestArticle();
      const originalTitle = article.title;
      article.update({ title: 'New Title' });

      expect(article.title).toBe(originalTitle);
    });

    it('should update tags', () => {
      const article = createTestArticle();
      const updated = article.update({ tags: ['new', 'tags'] });

      expect(updated.tags).toEqual(['new', 'tags']);
    });
  });

  describe('publish', () => {
    it('should set published to true', () => {
      const article = createTestArticle({ published: false });
      const published = article.publish();

      expect(published.published).toBe(true);
    });

    it('should update the updatedAt timestamp', () => {
      const article = createTestArticle();
      const published = article.publish();

      expect(published.updatedAt.getTime()).toBeGreaterThanOrEqual(
        article.updatedAt.getTime(),
      );
    });
  });

  describe('unpublish', () => {
    it('should set published to false', () => {
      const article = createTestArticle({ published: true });
      const unpublished = article.unpublish();

      expect(unpublished.published).toBe(false);
    });

    it('should update the updatedAt timestamp', () => {
      const article = createTestArticle({ published: true });
      const unpublished = article.unpublish();

      expect(unpublished.updatedAt.getTime()).toBeGreaterThanOrEqual(
        article.updatedAt.getTime(),
      );
    });
  });
});
