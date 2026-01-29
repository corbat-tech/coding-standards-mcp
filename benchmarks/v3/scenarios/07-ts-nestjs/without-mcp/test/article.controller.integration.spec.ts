import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication, ValidationPipe } from '@nestjs/common';
import * as request from 'supertest';
import { ArticleModule } from '../src/article/article.module';
import { ARTICLE_REPOSITORY } from '../src/article/domain/repository';
import { InMemoryArticleRepository } from '../src/article/infrastructure/persistence';

describe('ArticleController (Integration)', () => {
  let app: INestApplication;
  let repository: InMemoryArticleRepository;

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [ArticleModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
      }),
    );
    await app.init();

    repository = moduleFixture.get<InMemoryArticleRepository>(ARTICLE_REPOSITORY);
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => {
    repository.clear();
  });

  describe('POST /articles', () => {
    it('should create an article', async () => {
      const dto = {
        title: 'Test Article',
        content: 'This is test content that is at least 10 characters long.',
        author: 'John Doe',
        tags: ['test'],
      };

      const response = await request(app.getHttpServer())
        .post('/articles')
        .send(dto)
        .expect(201);

      expect(response.body).toMatchObject({
        title: dto.title,
        content: dto.content,
        author: dto.author,
        tags: dto.tags,
        published: false,
      });
      expect(response.body.id).toBeDefined();
    });

    it('should validate required fields', async () => {
      const response = await request(app.getHttpServer())
        .post('/articles')
        .send({})
        .expect(400);

      expect(response.body.message).toContain('title should not be empty');
    });

    it('should validate title length', async () => {
      const response = await request(app.getHttpServer())
        .post('/articles')
        .send({
          title: 'ab', // Too short
          content: 'This is valid content that is long enough.',
          author: 'John Doe',
        })
        .expect(400);

      expect(response.body.message).toContain('title must be longer than or equal to 3 characters');
    });

    it('should return 409 for duplicate title', async () => {
      const dto = {
        title: 'Duplicate Title',
        content: 'This is test content that is at least 10 characters long.',
        author: 'John Doe',
      };

      await request(app.getHttpServer()).post('/articles').send(dto).expect(201);

      await request(app.getHttpServer()).post('/articles').send(dto).expect(409);
    });
  });

  describe('GET /articles', () => {
    it('should return all articles', async () => {
      const dto1 = {
        title: 'Article One',
        content: 'Content for article one.',
        author: 'John',
      };
      const dto2 = {
        title: 'Article Two',
        content: 'Content for article two.',
        author: 'Jane',
      };

      await request(app.getHttpServer()).post('/articles').send(dto1);
      await request(app.getHttpServer()).post('/articles').send(dto2);

      const response = await request(app.getHttpServer())
        .get('/articles')
        .expect(200);

      expect(response.body).toHaveLength(2);
    });

    it('should return empty array when no articles', async () => {
      const response = await request(app.getHttpServer())
        .get('/articles')
        .expect(200);

      expect(response.body).toEqual([]);
    });
  });

  describe('GET /articles/:id', () => {
    it('should return an article by id', async () => {
      const dto = {
        title: 'Test Article',
        content: 'This is test content.',
        author: 'John Doe',
      };

      const createResponse = await request(app.getHttpServer())
        .post('/articles')
        .send(dto);

      const response = await request(app.getHttpServer())
        .get(`/articles/${createResponse.body.id}`)
        .expect(200);

      expect(response.body.title).toBe(dto.title);
    });

    it('should return 404 for non-existent article', async () => {
      await request(app.getHttpServer())
        .get('/articles/non-existent-id')
        .expect(404);
    });
  });

  describe('PUT /articles/:id', () => {
    it('should update an article', async () => {
      const dto = {
        title: 'Original Title',
        content: 'Original content.',
        author: 'John Doe',
      };

      const createResponse = await request(app.getHttpServer())
        .post('/articles')
        .send(dto);

      const response = await request(app.getHttpServer())
        .put(`/articles/${createResponse.body.id}`)
        .send({ title: 'Updated Title' })
        .expect(200);

      expect(response.body.title).toBe('Updated Title');
      expect(response.body.content).toBe(dto.content);
    });

    it('should return 404 for non-existent article', async () => {
      await request(app.getHttpServer())
        .put('/articles/non-existent-id')
        .send({ title: 'New Title' })
        .expect(404);
    });
  });

  describe('DELETE /articles/:id', () => {
    it('should delete an article', async () => {
      const dto = {
        title: 'Article to Delete',
        content: 'This will be deleted.',
        author: 'John Doe',
      };

      const createResponse = await request(app.getHttpServer())
        .post('/articles')
        .send(dto);

      await request(app.getHttpServer())
        .delete(`/articles/${createResponse.body.id}`)
        .expect(204);

      await request(app.getHttpServer())
        .get(`/articles/${createResponse.body.id}`)
        .expect(404);
    });

    it('should return 404 for non-existent article', async () => {
      await request(app.getHttpServer())
        .delete('/articles/non-existent-id')
        .expect(404);
    });
  });

  describe('POST /articles/:id/publish', () => {
    it('should publish an article', async () => {
      const dto = {
        title: 'Article to Publish',
        content: 'A'.repeat(100), // Content with at least 100 characters
        author: 'John Doe',
      };

      const createResponse = await request(app.getHttpServer())
        .post('/articles')
        .send(dto);

      const response = await request(app.getHttpServer())
        .post(`/articles/${createResponse.body.id}/publish`)
        .expect(201);

      expect(response.body.published).toBe(true);
    });

    it('should return 422 if content is too short', async () => {
      const dto = {
        title: 'Short Content Article',
        content: 'Too short', // Less than 100 characters
        author: 'John Doe',
      };

      const createResponse = await request(app.getHttpServer())
        .post('/articles')
        .send(dto);

      await request(app.getHttpServer())
        .post(`/articles/${createResponse.body.id}/publish`)
        .expect(422);
    });
  });

  describe('POST /articles/:id/unpublish', () => {
    it('should unpublish an article', async () => {
      const dto = {
        title: 'Article to Unpublish',
        content: 'A'.repeat(100),
        author: 'John Doe',
      };

      const createResponse = await request(app.getHttpServer())
        .post('/articles')
        .send(dto);

      await request(app.getHttpServer())
        .post(`/articles/${createResponse.body.id}/publish`);

      const response = await request(app.getHttpServer())
        .post(`/articles/${createResponse.body.id}/unpublish`)
        .expect(201);

      expect(response.body.published).toBe(false);
    });
  });

  describe('GET /articles/published', () => {
    it('should return only published articles', async () => {
      const dto1 = {
        title: 'Published Article',
        content: 'A'.repeat(100),
        author: 'John',
      };
      const dto2 = {
        title: 'Unpublished Article',
        content: 'Not published content.',
        author: 'Jane',
      };

      const response1 = await request(app.getHttpServer())
        .post('/articles')
        .send(dto1);
      await request(app.getHttpServer()).post('/articles').send(dto2);

      await request(app.getHttpServer())
        .post(`/articles/${response1.body.id}/publish`);

      const response = await request(app.getHttpServer())
        .get('/articles/published')
        .expect(200);

      expect(response.body).toHaveLength(1);
      expect(response.body[0].title).toBe('Published Article');
    });
  });

  describe('GET /articles/by-author', () => {
    it('should return articles by author', async () => {
      const dto1 = {
        title: 'Article by John',
        content: 'Content by John.',
        author: 'John Doe',
      };
      const dto2 = {
        title: 'Article by Jane',
        content: 'Content by Jane.',
        author: 'Jane Doe',
      };

      await request(app.getHttpServer()).post('/articles').send(dto1);
      await request(app.getHttpServer()).post('/articles').send(dto2);

      const response = await request(app.getHttpServer())
        .get('/articles/by-author')
        .query({ author: 'John Doe' })
        .expect(200);

      expect(response.body).toHaveLength(1);
      expect(response.body[0].author).toBe('John Doe');
    });
  });

  describe('GET /articles/by-tag', () => {
    it('should return articles by tag', async () => {
      const dto1 = {
        title: 'Tech Article',
        content: 'Tech content here.',
        author: 'John',
        tags: ['tech', 'news'],
      };
      const dto2 = {
        title: 'Sports Article',
        content: 'Sports content here.',
        author: 'Jane',
        tags: ['sports'],
      };

      await request(app.getHttpServer()).post('/articles').send(dto1);
      await request(app.getHttpServer()).post('/articles').send(dto2);

      const response = await request(app.getHttpServer())
        .get('/articles/by-tag')
        .query({ tag: 'tech' })
        .expect(200);

      expect(response.body).toHaveLength(1);
      expect(response.body[0].title).toBe('Tech Article');
    });
  });
});
