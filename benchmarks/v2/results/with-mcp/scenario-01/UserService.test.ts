import { describe, it, expect, beforeEach } from 'vitest';
import { UserService } from './UserService';
import { InMemoryUserRepository } from './InMemoryUserRepository';
import { IdGenerator } from './IdGenerator';
import {
  UserNotFoundError,
  UserAlreadyExistsError,
  InvalidUserInputError,
} from './UserErrors';

class StubIdGenerator implements IdGenerator {
  private currentId = 0;

  generate(): string {
    this.currentId++;
    return `user-${this.currentId}`;
  }

  reset(): void {
    this.currentId = 0;
  }
}

describe('UserService', () => {
  let service: UserService;
  let repository: InMemoryUserRepository;
  let idGenerator: StubIdGenerator;

  beforeEach(() => {
    repository = new InMemoryUserRepository();
    idGenerator = new StubIdGenerator();
    service = new UserService(repository, idGenerator);
  });

  describe('createUser', () => {
    it('should_create_user_when_valid_input', () => {
      // Arrange
      const input = { name: 'John Doe', email: 'john@example.com' };

      // Act
      const user = service.createUser(input);

      // Assert
      expect(user.id).toBe('user-1');
      expect(user.name).toBe('John Doe');
      expect(user.email).toBe('john@example.com');
    });

    it('should_trim_name_when_has_whitespace', () => {
      // Arrange
      const input = { name: '  John Doe  ', email: 'john@example.com' };

      // Act
      const user = service.createUser(input);

      // Assert
      expect(user.name).toBe('John Doe');
    });

    it('should_normalize_email_when_has_uppercase', () => {
      // Arrange
      const input = { name: 'John Doe', email: 'JOHN@EXAMPLE.COM' };

      // Act
      const user = service.createUser(input);

      // Assert
      expect(user.email).toBe('john@example.com');
    });

    it('should_throw_InvalidUserInputError_when_name_empty', () => {
      // Arrange
      const input = { name: '', email: 'john@example.com' };

      // Act & Assert
      expect(() => service.createUser(input)).toThrow(InvalidUserInputError);
    });

    it('should_throw_InvalidUserInputError_when_email_invalid', () => {
      // Arrange
      const input = { name: 'John Doe', email: 'invalid-email' };

      // Act & Assert
      expect(() => service.createUser(input)).toThrow(InvalidUserInputError);
    });

    it('should_throw_UserAlreadyExistsError_when_email_taken', () => {
      // Arrange
      service.createUser({ name: 'John Doe', email: 'john@example.com' });
      const input = { name: 'Jane Doe', email: 'john@example.com' };

      // Act & Assert
      expect(() => service.createUser(input)).toThrow(UserAlreadyExistsError);
    });

    it('should_persist_user_when_created', () => {
      // Arrange
      const input = { name: 'John Doe', email: 'john@example.com' };

      // Act
      const user = service.createUser(input);
      const found = repository.findById(user.id);

      // Assert
      expect(found).toEqual(user);
    });
  });

  describe('getUserById', () => {
    it('should_return_user_when_exists', () => {
      // Arrange
      const created = service.createUser({
        name: 'John Doe',
        email: 'john@example.com',
      });

      // Act
      const user = service.getUserById(created.id);

      // Assert
      expect(user).toEqual(created);
    });

    it('should_throw_UserNotFoundError_when_not_exists', () => {
      // Act & Assert
      expect(() => service.getUserById('non-existent')).toThrow(
        UserNotFoundError
      );
    });
  });

  describe('listAllUsers', () => {
    it('should_return_empty_array_when_no_users', () => {
      // Act
      const users = service.listAllUsers();

      // Assert
      expect(users).toEqual([]);
    });

    it('should_return_all_users_when_multiple_exist', () => {
      // Arrange
      const user1 = service.createUser({
        name: 'John Doe',
        email: 'john@example.com',
      });
      const user2 = service.createUser({
        name: 'Jane Doe',
        email: 'jane@example.com',
      });

      // Act
      const users = service.listAllUsers();

      // Assert
      expect(users).toHaveLength(2);
      expect(users).toContainEqual(user1);
      expect(users).toContainEqual(user2);
    });
  });
});
