import { Article } from '../../domain/entities/article.entity';

describe('Article Entity', () => {
  const validProps = {
    title: 'Test Article',
    content: 'This is test content',
    authorId: 'author-123',
    tags: ['test', 'unit'],
  };

  describe('constructor', () => {
    it('should create article with valid properties', () => {
      const article = new Article(validProps);

      expect(article.title).toBe(validProps.title);
      expect(article.content).toBe(validProps.content);
      expect(article.authorId).toBe(validProps.authorId);
      expect(article.tags).toEqual(validProps.tags);
      expect(article.id).toBeDefined();
      expect(article.createdAt).toBeInstanceOf(Date);
      expect(article.updatedAt).toBeInstanceOf(Date);
    });

    it('should generate unique IDs', () => {
      const article1 = new Article(validProps);
      const article2 = new Article(validProps);

      expect(article1.id).not.toBe(article2.id);
    });

    it('should use provided ID when given', () => {
      const customId = 'custom-id-123';
      const article = new Article({ ...validProps, id: customId });

      expect(article.id).toBe(customId);
    });

    it('should default tags to empty array when not provided', () => {
      const { tags, ...propsWithoutTags } = validProps;
      const article = new Article(propsWithoutTags);

      expect(article.tags).toEqual([]);
    });

    it('should use provided dates when given', () => {
      const createdAt = new Date('2024-01-01');
      const updatedAt = new Date('2024-01-02');
      const article = new Article({ ...validProps, createdAt, updatedAt });

      expect(article.createdAt).toBe(createdAt);
      expect(article.updatedAt).toBe(updatedAt);
    });
  });

  describe('update', () => {
    it('should update title and content', () => {
      const article = new Article(validProps);
      const originalUpdatedAt = article.updatedAt;

      // Wait a small amount to ensure updatedAt changes
      jest.useFakeTimers();
      jest.advanceTimersByTime(1000);

      article.update('New Title', 'New Content');

      expect(article.title).toBe('New Title');
      expect(article.content).toBe('New Content');
      expect(article.updatedAt.getTime()).toBeGreaterThan(originalUpdatedAt.getTime());

      jest.useRealTimers();
    });

    it('should update tags when provided', () => {
      const article = new Article(validProps);
      const newTags = ['updated', 'tags'];

      article.update('Title', 'Content', newTags);

      expect(article.tags).toEqual(newTags);
    });

    it('should keep existing tags when not provided', () => {
      const article = new Article(validProps);

      article.update('Title', 'Content');

      expect(article.tags).toEqual(validProps.tags);
    });
  });

  describe('toJSON', () => {
    it('should return article properties as plain object', () => {
      const article = new Article(validProps);
      const json = article.toJSON();

      expect(json).toEqual({
        id: article.id,
        title: validProps.title,
        content: validProps.content,
        authorId: validProps.authorId,
        tags: validProps.tags,
        createdAt: article.createdAt,
        updatedAt: article.updatedAt,
      });
    });

    it('should return a copy of tags array', () => {
      const article = new Article(validProps);
      const json = article.toJSON();

      json.tags?.push('modified');

      expect(article.tags).not.toContain('modified');
    });
  });

  describe('getters', () => {
    it('should return immutable tags copy', () => {
      const article = new Article(validProps);
      const tags = article.tags;

      tags.push('modified');

      expect(article.tags).not.toContain('modified');
    });
  });
});
