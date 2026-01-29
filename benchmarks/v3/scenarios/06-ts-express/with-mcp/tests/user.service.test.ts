/**
 * User Service Tests - Written BEFORE implementation (TDD)
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { UserServiceImpl } from '../services/user.service';
import { InMemoryUserRepository } from '../services/user.repository';
import { JwtServiceImpl } from '../services/jwt.service';
import { PasswordServiceImpl } from '../services/password.service';
import { NotFoundError, ConflictError, UnauthorizedError } from '../types/errors';
import type { IUserRepository, IJwtService, IPasswordService } from '../types';

describe('UserService', () => {
  let userService: UserServiceImpl;
  let userRepository: IUserRepository;
  let jwtService: IJwtService;
  let passwordService: IPasswordService;

  beforeEach(() => {
    userRepository = new InMemoryUserRepository();
    jwtService = new JwtServiceImpl('test-secret-key-at-least-32-chars');
    passwordService = new PasswordServiceImpl();
    userService = new UserServiceImpl(userRepository, jwtService, passwordService);
  });

  describe('createUser', () => {
    it('should create user with valid data', async () => {
      const dto = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
        role: 'user' as const,
      };

      const result = await userService.createUser(dto);

      expect(result.email).toBe(dto.email);
      expect(result.name).toBe(dto.name);
      expect(result.role).toBe(dto.role);
      expect(result.id).toBeDefined();
      expect(result.createdAt).toBeInstanceOf(Date);
      expect((result as Record<string, unknown>).password).toBeUndefined();
    });

    it('should fail create when email already exists', async () => {
      const dto = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      };

      await userService.createUser(dto);

      await expect(userService.createUser(dto)).rejects.toThrow(ConflictError);
    });

    it('should assign default role when not provided', async () => {
      const dto = {
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      };

      const result = await userService.createUser(dto);

      expect(result.role).toBe('user');
    });
  });

  describe('getUserById', () => {
    it('should get user by id', async () => {
      const created = await userService.createUser({
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      });

      const result = await userService.getUserById(created.id);

      expect(result.id).toBe(created.id);
      expect(result.email).toBe(created.email);
    });

    it('should fail get when user not found', async () => {
      await expect(
        userService.getUserById('550e8400-e29b-41d4-a716-446655440000')
      ).rejects.toThrow(NotFoundError);
    });
  });

  describe('updateUser', () => {
    it('should update user successfully', async () => {
      const created = await userService.createUser({
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      });

      const result = await userService.updateUser(created.id, {
        name: 'Updated Name',
      });

      expect(result.name).toBe('Updated Name');
      expect(result.email).toBe(created.email);
    });

    it('should fail update when user not found', async () => {
      await expect(
        userService.updateUser('550e8400-e29b-41d4-a716-446655440000', { name: 'New Name' })
      ).rejects.toThrow(NotFoundError);
    });

    it('should fail update when email already taken by another user', async () => {
      await userService.createUser({
        email: 'first@example.com',
        name: 'First User',
        password: 'password123',
      });

      const second = await userService.createUser({
        email: 'second@example.com',
        name: 'Second User',
        password: 'password123',
      });

      await expect(
        userService.updateUser(second.id, { email: 'first@example.com' })
      ).rejects.toThrow(ConflictError);
    });
  });

  describe('deleteUser', () => {
    it('should delete user successfully', async () => {
      const created = await userService.createUser({
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      });

      await userService.deleteUser(created.id);

      await expect(userService.getUserById(created.id)).rejects.toThrow(NotFoundError);
    });

    it('should fail delete when user not found', async () => {
      await expect(
        userService.deleteUser('550e8400-e29b-41d4-a716-446655440000')
      ).rejects.toThrow(NotFoundError);
    });
  });

  describe('getAllUsers', () => {
    it('should list all users', async () => {
      await userService.createUser({
        email: 'user1@example.com',
        name: 'User 1',
        password: 'password123',
      });

      await userService.createUser({
        email: 'user2@example.com',
        name: 'User 2',
        password: 'password123',
      });

      const result = await userService.getAllUsers();

      expect(result).toHaveLength(2);
    });

    it('should return empty array when no users', async () => {
      const result = await userService.getAllUsers();

      expect(result).toHaveLength(0);
    });
  });

  describe('login', () => {
    it('should authenticate with valid credentials', async () => {
      await userService.createUser({
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      });

      const result = await userService.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(result.user.email).toBe('test@example.com');
      expect(result.token).toBeDefined();
      expect(typeof result.token).toBe('string');
    });

    it('should fail auth with invalid email', async () => {
      await expect(
        userService.login({
          email: 'nonexistent@example.com',
          password: 'password123',
        })
      ).rejects.toThrow(UnauthorizedError);
    });

    it('should fail auth with invalid password', async () => {
      await userService.createUser({
        email: 'test@example.com',
        name: 'Test User',
        password: 'password123',
      });

      await expect(
        userService.login({
          email: 'test@example.com',
          password: 'wrongpassword',
        })
      ).rejects.toThrow(UnauthorizedError);
    });
  });
});
