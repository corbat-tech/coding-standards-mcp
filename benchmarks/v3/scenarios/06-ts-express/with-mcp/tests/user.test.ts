import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app.js';
import type { Express } from 'express';

describe('User API', () => {
  let app: Express;
  let authToken: string;

  beforeEach(async () => {
    app = createApp();

    await request(app)
      .post('/api/users')
      .send({ email: 'admin@test.com', name: 'Admin', password: 'password123', role: 'admin' });

    const loginRes = await request(app)
      .post('/api/auth/login')
      .send({ email: 'admin@test.com', password: 'password123' });

    authToken = loginRes.body.token;
  });

  describe('POST /api/users', () => {
    it('should create a new user', async () => {
      const res = await request(app)
        .post('/api/users')
        .send({ email: 'test@test.com', name: 'Test User', password: 'password123' });

      expect(res.status).toBe(201);
      expect(res.body.email).toBe('test@test.com');
      expect(res.body.name).toBe('Test User');
      expect(res.body.passwordHash).toBeUndefined();
    });

    it('should return 400 for invalid email', async () => {
      const res = await request(app)
        .post('/api/users')
        .send({ email: 'invalid', name: 'Test', password: 'password123' });

      expect(res.status).toBe(400);
      expect(res.body.error).toBe('ValidationError');
    });

    it('should return 409 for duplicate email', async () => {
      await request(app)
        .post('/api/users')
        .send({ email: 'dup@test.com', name: 'Test', password: 'password123' });

      const res = await request(app)
        .post('/api/users')
        .send({ email: 'dup@test.com', name: 'Another', password: 'password123' });

      expect(res.status).toBe(409);
    });
  });

  describe('GET /api/users', () => {
    it('should return all users when authenticated', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(Array.isArray(res.body)).toBe(true);
    });

    it('should return 401 without token', async () => {
      const res = await request(app).get('/api/users');
      expect(res.status).toBe(401);
    });
  });

  describe('GET /api/users/:id', () => {
    it('should return user by id', async () => {
      const createRes = await request(app)
        .post('/api/users')
        .send({ email: 'byid@test.com', name: 'By ID', password: 'password123' });

      const res = await request(app)
        .get(`/api/users/${createRes.body.id}`)
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(200);
      expect(res.body.email).toBe('byid@test.com');
    });

    it('should return 404 for non-existent user', async () => {
      const res = await request(app)
        .get('/api/users/non-existent-id')
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(404);
    });
  });

  describe('PUT /api/users/:id', () => {
    it('should update user', async () => {
      const createRes = await request(app)
        .post('/api/users')
        .send({ email: 'update@test.com', name: 'Original', password: 'password123' });

      const res = await request(app)
        .put(`/api/users/${createRes.body.id}`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({ name: 'Updated' });

      expect(res.status).toBe(200);
      expect(res.body.name).toBe('Updated');
    });
  });

  describe('DELETE /api/users/:id', () => {
    it('should delete user', async () => {
      const createRes = await request(app)
        .post('/api/users')
        .send({ email: 'delete@test.com', name: 'Delete Me', password: 'password123' });

      const res = await request(app)
        .delete(`/api/users/${createRes.body.id}`)
        .set('Authorization', `Bearer ${authToken}`);

      expect(res.status).toBe(204);
    });
  });
});

describe('Auth API', () => {
  let app: Express;

  beforeEach(() => {
    app = createApp();
  });

  describe('POST /api/auth/login', () => {
    it('should return token for valid credentials', async () => {
      await request(app)
        .post('/api/users')
        .send({ email: 'login@test.com', name: 'Login User', password: 'password123' });

      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'login@test.com', password: 'password123' });

      expect(res.status).toBe(200);
      expect(res.body.token).toBeDefined();
      expect(res.body.user.email).toBe('login@test.com');
    });

    it('should return 401 for invalid credentials', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'notexist@test.com', password: 'wrong' });

      expect(res.status).toBe(401);
    });
  });
});
