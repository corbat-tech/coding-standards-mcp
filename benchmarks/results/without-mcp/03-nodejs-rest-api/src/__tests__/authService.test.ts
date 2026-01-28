import { authService } from '../services/authService';
import { userRepository } from '../repositories/userRepository';

describe('AuthService', () => {
  beforeEach(() => {
    userRepository.clear();
  });

  describe('register', () => {
    it('should register a new user successfully', async () => {
      const result = await authService.register({
        email: 'test@example.com',
        password: 'Password123',
        name: 'Test User',
      });

      expect(result.token).toBeDefined();
      expect(result.user.email).toBe('test@example.com');
      expect(result.user.name).toBe('Test User');
      expect(result.user.id).toBeDefined();
    });

    it('should throw error if email already exists', async () => {
      await authService.register({
        email: 'test@example.com',
        password: 'Password123',
        name: 'Test User',
      });

      await expect(
        authService.register({
          email: 'test@example.com',
          password: 'Password456',
          name: 'Another User',
        })
      ).rejects.toThrow('Email already registered');
    });

    it('should normalize email to lowercase', async () => {
      const result = await authService.register({
        email: 'TEST@EXAMPLE.COM',
        password: 'Password123',
        name: 'Test User',
      });

      expect(result.user.email).toBe('test@example.com');
    });
  });

  describe('login', () => {
    beforeEach(async () => {
      await authService.register({
        email: 'test@example.com',
        password: 'Password123',
        name: 'Test User',
      });
    });

    it('should login successfully with correct credentials', async () => {
      const result = await authService.login({
        email: 'test@example.com',
        password: 'Password123',
      });

      expect(result.token).toBeDefined();
      expect(result.user.email).toBe('test@example.com');
    });

    it('should throw error with incorrect password', async () => {
      await expect(
        authService.login({
          email: 'test@example.com',
          password: 'WrongPassword',
        })
      ).rejects.toThrow('Invalid credentials');
    });

    it('should throw error with non-existent email', async () => {
      await expect(
        authService.login({
          email: 'nonexistent@example.com',
          password: 'Password123',
        })
      ).rejects.toThrow('Invalid credentials');
    });

    it('should login with case-insensitive email', async () => {
      const result = await authService.login({
        email: 'TEST@EXAMPLE.COM',
        password: 'Password123',
      });

      expect(result.token).toBeDefined();
    });
  });

  describe('verifyToken', () => {
    it('should verify a valid token', async () => {
      const registerResult = await authService.register({
        email: 'test@example.com',
        password: 'Password123',
        name: 'Test User',
      });

      const decoded = authService.verifyToken(registerResult.token);
      expect(decoded.userId).toBe(registerResult.user.id);
    });

    it('should throw error for invalid token', () => {
      expect(() => authService.verifyToken('invalid-token')).toThrow('Invalid token');
    });
  });
});
