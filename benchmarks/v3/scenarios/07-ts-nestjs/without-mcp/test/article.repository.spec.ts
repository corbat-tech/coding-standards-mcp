import { InMemoryArticleRepository } from '../src/article/infrastructure/persistence';
import { Article } from '../src/article/domain/entity';

describe('InMemoryArticleRepository', () => {
  let repository: InMemoryArticleRepository;

  const createTestArticle = (overrides?: Partial<ConstructorParameters<typeof Article>[0]>) => {
    return new Article({
      id: 'test-id-' + Math.random().toString(36).substring(7),
      title: 'Test Article',
      content: 'This is a test article content that is long enough for testing.',
      author: 'Test Author',
      tags: ['test', 'article'],
      published: false,
      ...overrides,
    });
  };

  beforeEach(() => {
    repository = new InMemoryArticleRepository();
  });

  describe('save', () => {
    it('should save an article', async () => {
      const article = createTestArticle({ id: 'save-test-id' });
      const saved = await repository.save(article);

      expect(saved).toEqual(article);
    });
  });

  describe('findById', () => {
    it('should find an article by id', async () => {
      const article = createTestArticle({ id: 'find-by-id-test' });
      await repository.save(article);

      const found = await repository.findById(article.id);

      expect(found).toEqual(article);
    });

    it('should return null if article not found', async () => {
      const found = await repository.findById('non-existent');

      expect(found).toBeNull();
    });
  });

  describe('findByTitle', () => {
    it('should find an article by title', async () => {
      const article = createTestArticle({ title: 'Unique Title' });
      await repository.save(article);

      const found = await repository.findByTitle('Unique Title');

      expect(found).toEqual(article);
    });

    it('should return null if article not found', async () => {
      const found = await repository.findByTitle('Non-existent Title');

      expect(found).toBeNull();
    });
  });

  describe('findAll', () => {
    it('should return all articles', async () => {
      const article1 = createTestArticle({ id: 'id-1', title: 'Article 1' });
      const article2 = createTestArticle({ id: 'id-2', title: 'Article 2' });
      await repository.save(article1);
      await repository.save(article2);

      const articles = await repository.findAll();

      expect(articles).toHaveLength(2);
      expect(articles).toContainEqual(article1);
      expect(articles).toContainEqual(article2);
    });

    it('should return empty array if no articles', async () => {
      const articles = await repository.findAll();

      expect(articles).toEqual([]);
    });
  });

  describe('findByAuthor', () => {
    it('should find articles by author', async () => {
      const article1 = createTestArticle({ id: 'id-1', author: 'John Doe', title: 'Article 1' });
      const article2 = createTestArticle({ id: 'id-2', author: 'John Doe', title: 'Article 2' });
      const article3 = createTestArticle({ id: 'id-3', author: 'Jane Doe', title: 'Article 3' });
      await repository.save(article1);
      await repository.save(article2);
      await repository.save(article3);

      const articles = await repository.findByAuthor('John Doe');

      expect(articles).toHaveLength(2);
      expect(articles).toContainEqual(article1);
      expect(articles).toContainEqual(article2);
    });
  });

  describe('findByTag', () => {
    it('should find articles by tag', async () => {
      const article1 = createTestArticle({ id: 'id-1', tags: ['tech', 'news'], title: 'Article 1' });
      const article2 = createTestArticle({ id: 'id-2', tags: ['tech'], title: 'Article 2' });
      const article3 = createTestArticle({ id: 'id-3', tags: ['sports'], title: 'Article 3' });
      await repository.save(article1);
      await repository.save(article2);
      await repository.save(article3);

      const articles = await repository.findByTag('tech');

      expect(articles).toHaveLength(2);
      expect(articles).toContainEqual(article1);
      expect(articles).toContainEqual(article2);
    });
  });

  describe('findPublished', () => {
    it('should find only published articles', async () => {
      const article1 = createTestArticle({ id: 'id-1', published: true, title: 'Article 1' });
      const article2 = createTestArticle({ id: 'id-2', published: false, title: 'Article 2' });
      const article3 = createTestArticle({ id: 'id-3', published: true, title: 'Article 3' });
      await repository.save(article1);
      await repository.save(article2);
      await repository.save(article3);

      const articles = await repository.findPublished();

      expect(articles).toHaveLength(2);
      expect(articles).toContainEqual(article1);
      expect(articles).toContainEqual(article3);
    });
  });

  describe('update', () => {
    it('should update an article', async () => {
      const article = createTestArticle({ id: 'update-test' });
      await repository.save(article);

      const updatedArticle = article.update({ title: 'Updated Title' });
      const saved = await repository.update(updatedArticle);

      expect(saved.title).toBe('Updated Title');

      const found = await repository.findById(article.id);
      expect(found?.title).toBe('Updated Title');
    });
  });

  describe('delete', () => {
    it('should delete an article', async () => {
      const article = createTestArticle({ id: 'delete-test' });
      await repository.save(article);

      await repository.delete(article.id);

      const found = await repository.findById(article.id);
      expect(found).toBeNull();
    });
  });

  describe('existsById', () => {
    it('should return true if article exists', async () => {
      const article = createTestArticle({ id: 'exists-test' });
      await repository.save(article);

      const exists = await repository.existsById(article.id);

      expect(exists).toBe(true);
    });

    it('should return false if article does not exist', async () => {
      const exists = await repository.existsById('non-existent');

      expect(exists).toBe(false);
    });
  });

  describe('existsByTitle', () => {
    it('should return true if article with title exists', async () => {
      const article = createTestArticle({ title: 'Existing Title' });
      await repository.save(article);

      const exists = await repository.existsByTitle('Existing Title');

      expect(exists).toBe(true);
    });

    it('should return false if article with title does not exist', async () => {
      const exists = await repository.existsByTitle('Non-existent Title');

      expect(exists).toBe(false);
    });
  });

  describe('clear', () => {
    it('should remove all articles', async () => {
      const article1 = createTestArticle({ id: 'id-1', title: 'Article 1' });
      const article2 = createTestArticle({ id: 'id-2', title: 'Article 2' });
      await repository.save(article1);
      await repository.save(article2);

      repository.clear();

      const articles = await repository.findAll();
      expect(articles).toEqual([]);
    });
  });
});
