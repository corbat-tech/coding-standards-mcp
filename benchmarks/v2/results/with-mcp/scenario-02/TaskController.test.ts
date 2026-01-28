import { describe, it, expect, beforeEach } from 'vitest';
import express, { Express } from 'express';
import request from 'supertest';
import { TaskController } from './TaskController';
import { TaskService, IdGenerator } from './TaskService';
import { InMemoryTaskRepository } from './InMemoryTaskRepository';

class StubIdGenerator implements IdGenerator {
  private currentId = 0;

  generate(): string {
    this.currentId++;
    return `task-${this.currentId}`;
  }

  reset(): void {
    this.currentId = 0;
  }
}

describe('TaskController', () => {
  let app: Express;
  let repository: InMemoryTaskRepository;
  let idGenerator: StubIdGenerator;

  beforeEach(() => {
    repository = new InMemoryTaskRepository();
    idGenerator = new StubIdGenerator();
    const service = new TaskService(repository, idGenerator);
    const controller = new TaskController(service);

    app = express();
    app.use(express.json());
    app.use(controller.router);
  });

  describe('POST /tasks', () => {
    it('should_create_task_when_valid_input', async () => {
      // Arrange
      const input = { title: 'Test task', description: 'Description' };

      // Act
      const response = await request(app).post('/tasks').send(input);

      // Assert
      expect(response.status).toBe(201);
      expect(response.body.id).toBe('task-1');
      expect(response.body.title).toBe('Test task');
    });

    it('should_return_400_when_title_empty', async () => {
      // Arrange
      const input = { title: '' };

      // Act
      const response = await request(app).post('/tasks').send(input);

      // Assert
      expect(response.status).toBe(400);
    });
  });

  describe('GET /tasks', () => {
    it('should_return_empty_array_when_no_tasks', async () => {
      // Act
      const response = await request(app).get('/tasks');

      // Assert
      expect(response.status).toBe(200);
      expect(response.body).toEqual([]);
    });

    it('should_return_all_tasks_when_exist', async () => {
      // Arrange
      await request(app).post('/tasks').send({ title: 'Task 1' });
      await request(app).post('/tasks').send({ title: 'Task 2' });

      // Act
      const response = await request(app).get('/tasks');

      // Assert
      expect(response.status).toBe(200);
      expect(response.body).toHaveLength(2);
    });
  });

  describe('GET /tasks/:id', () => {
    it('should_return_task_when_exists', async () => {
      // Arrange
      await request(app).post('/tasks').send({ title: 'Test task' });

      // Act
      const response = await request(app).get('/tasks/task-1');

      // Assert
      expect(response.status).toBe(200);
      expect(response.body.title).toBe('Test task');
    });

    it('should_return_404_when_not_found', async () => {
      // Act
      const response = await request(app).get('/tasks/non-existent');

      // Assert
      expect(response.status).toBe(404);
    });
  });

  describe('DELETE /tasks/:id', () => {
    it('should_delete_task_when_exists', async () => {
      // Arrange
      await request(app).post('/tasks').send({ title: 'Test task' });

      // Act
      const response = await request(app).delete('/tasks/task-1');

      // Assert
      expect(response.status).toBe(204);
    });

    it('should_return_404_when_task_not_found', async () => {
      // Act
      const response = await request(app).delete('/tasks/non-existent');

      // Assert
      expect(response.status).toBe(404);
    });

    it('should_remove_task_from_list_after_delete', async () => {
      // Arrange
      await request(app).post('/tasks').send({ title: 'Test task' });
      await request(app).delete('/tasks/task-1');

      // Act
      const response = await request(app).get('/tasks');

      // Assert
      expect(response.body).toHaveLength(0);
    });
  });
});
