import { describe, it, expect, beforeEach, beforeAll } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app';
import { userService } from '../src/services/user.service';
import { generateToken } from '../src/utils/jwt';
import { Express } from 'express';

describe('Auth API', () => {
  let app: Express;
  let testUserId: string;

  beforeAll(() => {
    app = createApp();
  });

  beforeEach(async () => {
    // Clear all users before each test
    userService.clearAll();

    // Create test user
    const testUser = await userService.create({
      email: 'test@example.com',
      name: 'Test User',
      role: 'user',
    });

    testUserId = testUser.id;
  });

  describe('POST /api/auth/login', () => {
    it('should login successfully with valid email', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'test@example.com' });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('success');
      expect(res.body.data.token).toBeDefined();
      expect(res.body.data.user).toBeDefined();
      expect(res.body.data.user.email).toBe('test@example.com');
    });

    it('should return 404 for non-existent user', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'nonexistent@example.com' });

      expect(res.status).toBe(404);
      expect(res.body.status).toBe('error');
    });

    it('should return 400 for invalid email format', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'invalid-email' });

      expect(res.status).toBe(400);
      expect(res.body.errors).toBeDefined();
    });

    it('should return 400 when email is missing', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({});

      expect(res.status).toBe(400);
    });
  });

  describe('GET /api/auth/me', () => {
    it('should return current user when authenticated', async () => {
      const token = generateToken({
        userId: testUserId,
        email: 'test@example.com',
        role: 'user',
      });

      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', `Bearer ${token}`);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('success');
      expect(res.body.data.id).toBe(testUserId);
      expect(res.body.data.email).toBe('test@example.com');
    });

    it('should return 401 when not authenticated', async () => {
      const res = await request(app).get('/api/auth/me');

      expect(res.status).toBe(401);
      expect(res.body.status).toBe('error');
    });

    it('should return 401 with invalid token', async () => {
      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'Bearer invalid-token');

      expect(res.status).toBe(401);
    });

    it('should return 401 with malformed authorization header', async () => {
      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', 'invalid-header');

      expect(res.status).toBe(401);
    });
  });
});
