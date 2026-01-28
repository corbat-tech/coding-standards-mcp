import { UserService } from '../services/userService';
import { InMemoryUserRepository, UserRepository } from '../repositories/userRepository';
import { ConflictError, NotFoundError } from '../domain/errors';
import { User } from '../types/user';
import { v4 as uuidv4 } from 'uuid';

describe('UserService', () => {
  let userService: UserService;
  let userRepository: UserRepository;
  let testUser: User;

  beforeEach(async () => {
    userRepository = new InMemoryUserRepository();
    userService = new UserService(userRepository);

    testUser = {
      id: uuidv4(),
      email: 'test@example.com',
      passwordHash: 'hashed',
      name: 'Test User',
      createdAt: new Date(),
      updatedAt: new Date(),
    };
    await userRepository.save(testUser);
  });

  describe('getProfile', () => {
    it('should_return_user_when_exists', async () => {
      // Arrange & Act
      const result = await userService.getProfile(testUser.id);

      // Assert
      expect(result.id).toBe(testUser.id);
      expect(result.email).toBe(testUser.email);
    });

    it('should_not_return_password_hash', async () => {
      // Arrange & Act
      const result = await userService.getProfile(testUser.id);

      // Assert
      expect(result).not.toHaveProperty('passwordHash');
    });

    it('should_throw_when_user_not_found', async () => {
      // Arrange & Act & Assert
      await expect(userService.getProfile('non-existent-id'))
        .rejects.toThrow(NotFoundError);
    });
  });

  describe('updateProfile', () => {
    it('should_update_name_when_provided', async () => {
      // Arrange
      const dto = { name: 'Updated Name' };

      // Act
      const result = await userService.updateProfile(testUser.id, dto);

      // Assert
      expect(result.name).toBe('Updated Name');
    });

    it('should_update_email_when_provided', async () => {
      // Arrange
      const dto = { email: 'new@example.com' };

      // Act
      const result = await userService.updateProfile(testUser.id, dto);

      // Assert
      expect(result.email).toBe('new@example.com');
    });

    it('should_throw_when_email_already_taken', async () => {
      // Arrange
      const otherUser: User = {
        id: uuidv4(),
        email: 'other@example.com',
        passwordHash: 'hashed',
        name: 'Other User',
        createdAt: new Date(),
        updatedAt: new Date(),
      };
      await userRepository.save(otherUser);

      const dto = { email: 'other@example.com' };

      // Act & Assert
      await expect(userService.updateProfile(testUser.id, dto))
        .rejects.toThrow(ConflictError);
    });

    it('should_throw_when_user_not_found', async () => {
      // Arrange
      const dto = { name: 'New Name' };

      // Act & Assert
      await expect(userService.updateProfile('non-existent', dto))
        .rejects.toThrow(NotFoundError);
    });

    it('should_normalize_email_to_lowercase', async () => {
      // Arrange
      const dto = { email: 'UPPERCASE@EXAMPLE.COM' };

      // Act
      const result = await userService.updateProfile(testUser.id, dto);

      // Assert
      expect(result.email).toBe('uppercase@example.com');
    });

    it('should_allow_same_email_for_same_user', async () => {
      // Arrange
      const dto = { email: testUser.email };

      // Act
      const result = await userService.updateProfile(testUser.id, dto);

      // Assert
      expect(result.email).toBe(testUser.email);
    });
  });
});
