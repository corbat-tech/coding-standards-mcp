import { describe, it, expect, beforeEach, beforeAll, afterAll } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app';
import { userService } from '../src/services/user.service';
import { generateToken } from '../src/utils/jwt';
import { Express } from 'express';

describe('User API', () => {
  let app: Express;
  let adminToken: string;
  let userToken: string;
  let testUserId: string;

  beforeAll(() => {
    app = createApp();
  });

  beforeEach(async () => {
    // Clear all users before each test
    userService.clearAll();

    // Create admin user
    const adminUser = await userService.create({
      email: 'admin@example.com',
      name: 'Admin User',
      role: 'admin',
    });

    // Create regular user
    const regularUser = await userService.create({
      email: 'user@example.com',
      name: 'Regular User',
      role: 'user',
    });

    testUserId = regularUser.id;

    // Generate tokens
    adminToken = generateToken({
      userId: adminUser.id,
      email: adminUser.email,
      role: adminUser.role,
    });

    userToken = generateToken({
      userId: regularUser.id,
      email: regularUser.email,
      role: regularUser.role,
    });
  });

  describe('GET /api/users', () => {
    it('should return all users when authenticated', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('success');
      expect(res.body.data).toHaveLength(2);
    });

    it('should return 401 when not authenticated', async () => {
      const res = await request(app).get('/api/users');

      expect(res.status).toBe(401);
      expect(res.body.status).toBe('error');
    });

    it('should return 401 with invalid token', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', 'Bearer invalid-token');

      expect(res.status).toBe(401);
    });
  });

  describe('GET /api/users/:id', () => {
    it('should return a user by ID', async () => {
      const res = await request(app)
        .get(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('success');
      expect(res.body.data.id).toBe(testUserId);
      expect(res.body.data.email).toBe('user@example.com');
    });

    it('should return 404 for non-existent user', async () => {
      const res = await request(app)
        .get('/api/users/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(404);
      expect(res.body.status).toBe('error');
    });

    it('should return 400 for invalid UUID', async () => {
      const res = await request(app)
        .get('/api/users/invalid-id')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(400);
    });
  });

  describe('POST /api/users', () => {
    it('should create a new user when admin', async () => {
      const newUser = {
        email: 'newuser@example.com',
        name: 'New User',
        role: 'user',
      };

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send(newUser);

      expect(res.status).toBe(201);
      expect(res.body.status).toBe('success');
      expect(res.body.data.email).toBe(newUser.email);
      expect(res.body.data.name).toBe(newUser.name);
      expect(res.body.data.role).toBe(newUser.role);
      expect(res.body.data.id).toBeDefined();
      expect(res.body.data.createdAt).toBeDefined();
    });

    it('should return 403 when non-admin tries to create user', async () => {
      const newUser = {
        email: 'newuser@example.com',
        name: 'New User',
      };

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${userToken}`)
        .send(newUser);

      expect(res.status).toBe(403);
    });

    it('should return 400 for invalid email', async () => {
      const newUser = {
        email: 'invalid-email',
        name: 'New User',
      };

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send(newUser);

      expect(res.status).toBe(400);
      expect(res.body.errors).toBeDefined();
    });

    it('should return 400 for missing name', async () => {
      const newUser = {
        email: 'newuser@example.com',
      };

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send(newUser);

      expect(res.status).toBe(400);
    });

    it('should return 409 for duplicate email', async () => {
      const newUser = {
        email: 'user@example.com', // Already exists
        name: 'Duplicate User',
      };

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send(newUser);

      expect(res.status).toBe(409);
    });

    it('should default role to user when not provided', async () => {
      const newUser = {
        email: 'newuser@example.com',
        name: 'New User',
      };

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send(newUser);

      expect(res.status).toBe(201);
      expect(res.body.data.role).toBe('user');
    });
  });

  describe('PATCH /api/users/:id', () => {
    it('should update a user when admin', async () => {
      const updateData = {
        name: 'Updated Name',
      };

      const res = await request(app)
        .patch(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send(updateData);

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('success');
      expect(res.body.data.name).toBe('Updated Name');
      expect(res.body.data.email).toBe('user@example.com'); // Unchanged
    });

    it('should return 403 when non-admin tries to update', async () => {
      const updateData = {
        name: 'Updated Name',
      };

      const res = await request(app)
        .patch(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${userToken}`)
        .send(updateData);

      expect(res.status).toBe(403);
    });

    it('should return 404 for non-existent user', async () => {
      const res = await request(app)
        .patch('/api/users/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ name: 'Updated Name' });

      expect(res.status).toBe(404);
    });

    it('should return 400 for empty update body', async () => {
      const res = await request(app)
        .patch(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({});

      expect(res.status).toBe(400);
    });

    it('should return 409 when updating to duplicate email', async () => {
      const res = await request(app)
        .patch(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ email: 'admin@example.com' });

      expect(res.status).toBe(409);
    });
  });

  describe('DELETE /api/users/:id', () => {
    it('should delete a user when admin', async () => {
      const res = await request(app)
        .delete(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(204);

      // Verify user is deleted
      const getRes = await request(app)
        .get(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${adminToken}`);

      expect(getRes.status).toBe(404);
    });

    it('should return 403 when non-admin tries to delete', async () => {
      const res = await request(app)
        .delete(`/api/users/${testUserId}`)
        .set('Authorization', `Bearer ${userToken}`);

      expect(res.status).toBe(403);
    });

    it('should return 404 for non-existent user', async () => {
      const res = await request(app)
        .delete('/api/users/00000000-0000-0000-0000-000000000000')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(404);
    });
  });
});
