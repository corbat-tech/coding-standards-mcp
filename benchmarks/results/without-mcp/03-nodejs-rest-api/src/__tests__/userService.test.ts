import { userService } from '../services/userService';
import { authService } from '../services/authService';
import { userRepository } from '../repositories/userRepository';

describe('UserService', () => {
  let userId: string;

  beforeEach(async () => {
    userRepository.clear();
    const result = await authService.register({
      email: 'test@example.com',
      password: 'Password123',
      name: 'Test User',
    });
    userId = result.user.id;
  });

  describe('getProfile', () => {
    it('should return user profile', async () => {
      const profile = await userService.getProfile(userId);

      expect(profile.email).toBe('test@example.com');
      expect(profile.name).toBe('Test User');
      expect(profile.id).toBe(userId);
    });

    it('should throw error for non-existent user', async () => {
      await expect(userService.getProfile('non-existent-id')).rejects.toThrow('User not found');
    });
  });

  describe('updateProfile', () => {
    it('should update user name', async () => {
      const updated = await userService.updateProfile(userId, {
        name: 'Updated Name',
      });

      expect(updated.name).toBe('Updated Name');
      expect(updated.email).toBe('test@example.com');
    });

    it('should update user email', async () => {
      const updated = await userService.updateProfile(userId, {
        email: 'newemail@example.com',
      });

      expect(updated.email).toBe('newemail@example.com');
    });

    it('should update both name and email', async () => {
      const updated = await userService.updateProfile(userId, {
        name: 'New Name',
        email: 'newemail@example.com',
      });

      expect(updated.name).toBe('New Name');
      expect(updated.email).toBe('newemail@example.com');
    });

    it('should throw error when email is already in use', async () => {
      await authService.register({
        email: 'other@example.com',
        password: 'Password123',
        name: 'Other User',
      });

      await expect(
        userService.updateProfile(userId, {
          email: 'other@example.com',
        })
      ).rejects.toThrow('Email already in use');
    });

    it('should throw error for non-existent user', async () => {
      await expect(
        userService.updateProfile('non-existent-id', {
          name: 'New Name',
        })
      ).rejects.toThrow('User not found');
    });

    it('should allow updating to same email', async () => {
      const updated = await userService.updateProfile(userId, {
        email: 'test@example.com',
      });

      expect(updated.email).toBe('test@example.com');
    });
  });
});
