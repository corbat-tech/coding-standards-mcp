import request from 'supertest';
import app from './app';

describe('Tasks API', () => {
  describe('POST /tasks', () => {
    it('should create a task', async () => {
      const response = await request(app)
        .post('/tasks')
        .send({ title: 'Test Task', description: 'Test description' });

      expect(response.status).toBe(201);
      expect(response.body.title).toBe('Test Task');
      expect(response.body.id).toBeDefined();
    });

    it('should return 400 when title is missing', async () => {
      const response = await request(app)
        .post('/tasks')
        .send({ description: 'No title' });

      expect(response.status).toBe(400);
      expect(response.body.error).toBe('Title is required');
    });
  });

  describe('GET /tasks', () => {
    it('should return all tasks', async () => {
      const response = await request(app).get('/tasks');

      expect(response.status).toBe(200);
      expect(Array.isArray(response.body)).toBe(true);
    });
  });

  describe('GET /tasks/:id', () => {
    it('should return task by id', async () => {
      const createResponse = await request(app)
        .post('/tasks')
        .send({ title: 'Find me' });

      const response = await request(app).get(`/tasks/${createResponse.body.id}`);

      expect(response.status).toBe(200);
      expect(response.body.title).toBe('Find me');
    });

    it('should return 404 for non-existent task', async () => {
      const response = await request(app).get('/tasks/non-existent-id');

      expect(response.status).toBe(404);
    });
  });

  describe('DELETE /tasks/:id', () => {
    it('should delete task', async () => {
      const createResponse = await request(app)
        .post('/tasks')
        .send({ title: 'Delete me' });

      const response = await request(app).delete(`/tasks/${createResponse.body.id}`);

      expect(response.status).toBe(204);
    });

    it('should return 404 for non-existent task', async () => {
      const response = await request(app).delete('/tasks/non-existent-id');

      expect(response.status).toBe(404);
    });
  });
});
