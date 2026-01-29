/**
 * Integration Tests for User Routes
 */

import { describe, it, expect, beforeEach } from 'vitest';
import request from 'supertest';
import { createApp } from '../app';
import type { Application } from 'express';

describe('User Routes Integration', () => {
  let app: Application;
  const testSecret = 'test-secret-key-that-is-at-least-32-characters';

  beforeEach(() => {
    app = createApp({ jwtSecret: testSecret });
  });

  describe('POST /api/users/register', () => {
    it('should register a new user', async () => {
      const response = await request(app)
        .post('/api/users/register')
        .send({
          email: 'test@example.com',
          name: 'Test User',
          password: 'password123',
        });

      expect(response.status).toBe(201);
      expect(response.body.email).toBe('test@example.com');
      expect(response.body.name).toBe('Test User');
      expect(response.body.password).toBeUndefined();
    });

    it('should fail with invalid email', async () => {
      const response = await request(app)
        .post('/api/users/register')
        .send({
          email: 'invalid',
          name: 'Test User',
          password: 'password123',
        });

      expect(response.status).toBe(400);
      expect(response.body.error.code).toBe('VALIDATION_ERROR');
    });

    it('should fail with duplicate email', async () => {
      await request(app)
        .post('/api/users/register')
        .send({
          email: 'test@example.com',
          name: 'Test User',
          password: 'password123',
        });

      const response = await request(app)
        .post('/api/users/register')
        .send({
          email: 'test@example.com',
          name: 'Another User',
          password: 'password456',
        });

      expect(response.status).toBe(409);
      expect(response.body.error.code).toBe('CONFLICT');
    });
  });

  describe('POST /api/users/login', () => {
    beforeEach(async () => {
      await request(app)
        .post('/api/users/register')
        .send({
          email: 'test@example.com',
          name: 'Test User',
          password: 'password123',
        });
    });

    it('should login with valid credentials', async () => {
      const response = await request(app)
        .post('/api/users/login')
        .send({
          email: 'test@example.com',
          password: 'password123',
        });

      expect(response.status).toBe(200);
      expect(response.body.token).toBeDefined();
      expect(response.body.user.email).toBe('test@example.com');
    });

    it('should fail with wrong password', async () => {
      const response = await request(app)
        .post('/api/users/login')
        .send({
          email: 'test@example.com',
          password: 'wrongpassword',
        });

      expect(response.status).toBe(401);
      expect(response.body.error.code).toBe('UNAUTHORIZED');
    });
  });

  describe('Protected Routes', () => {
    let authToken: string;
    let userId: string;

    beforeEach(async () => {
      const registerResponse = await request(app)
        .post('/api/users/register')
        .send({
          email: 'test@example.com',
          name: 'Test User',
          password: 'password123',
        });
      userId = registerResponse.body.id;

      const loginResponse = await request(app)
        .post('/api/users/login')
        .send({
          email: 'test@example.com',
          password: 'password123',
        });
      authToken = loginResponse.body.token;
    });

    describe('GET /api/users', () => {
      it('should list users with valid token', async () => {
        const response = await request(app)
          .get('/api/users')
          .set('Authorization', `Bearer ${authToken}`);

        expect(response.status).toBe(200);
        expect(Array.isArray(response.body)).toBe(true);
        expect(response.body.length).toBeGreaterThan(0);
      });

      it('should fail without token', async () => {
        const response = await request(app).get('/api/users');

        expect(response.status).toBe(401);
      });
    });

    describe('GET /api/users/:id', () => {
      it('should get user by id', async () => {
        const response = await request(app)
          .get(`/api/users/${userId}`)
          .set('Authorization', `Bearer ${authToken}`);

        expect(response.status).toBe(200);
        expect(response.body.id).toBe(userId);
      });

      it('should fail with invalid uuid', async () => {
        const response = await request(app)
          .get('/api/users/invalid-id')
          .set('Authorization', `Bearer ${authToken}`);

        expect(response.status).toBe(400);
      });

      it('should fail when user not found', async () => {
        const response = await request(app)
          .get('/api/users/550e8400-e29b-41d4-a716-446655440000')
          .set('Authorization', `Bearer ${authToken}`);

        expect(response.status).toBe(404);
      });
    });

    describe('PUT /api/users/:id', () => {
      it('should update user', async () => {
        const response = await request(app)
          .put(`/api/users/${userId}`)
          .set('Authorization', `Bearer ${authToken}`)
          .send({ name: 'Updated Name' });

        expect(response.status).toBe(200);
        expect(response.body.name).toBe('Updated Name');
      });
    });

    describe('DELETE /api/users/:id', () => {
      it('should delete user', async () => {
        const response = await request(app)
          .delete(`/api/users/${userId}`)
          .set('Authorization', `Bearer ${authToken}`);

        expect(response.status).toBe(204);
      });
    });
  });

  describe('GET /health', () => {
    it('should return health status', async () => {
      const response = await request(app).get('/health');

      expect(response.status).toBe(200);
      expect(response.body.status).toBe('ok');
    });
  });
});
