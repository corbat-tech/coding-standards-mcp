import { UserService } from './UserService';

describe('UserService', () => {
  let userService: UserService;

  beforeEach(() => {
    userService = new UserService();
  });

  describe('createUser', () => {
    it('should create a user with valid input', () => {
      const user = userService.createUser({
        name: 'John Doe',
        email: 'john@example.com',
      });

      expect(user.id).toBeDefined();
      expect(user.name).toBe('John Doe');
      expect(user.email).toBe('john@example.com');
      expect(user.createdAt).toBeInstanceOf(Date);
    });

    it('should throw error when name is empty', () => {
      expect(() => {
        userService.createUser({ name: '', email: 'test@example.com' });
      }).toThrow('Name is required');
    });

    it('should throw error when email is invalid', () => {
      expect(() => {
        userService.createUser({ name: 'John', email: 'invalid-email' });
      }).toThrow('Valid email is required');
    });

    it('should normalize email to lowercase', () => {
      const user = userService.createUser({
        name: 'John',
        email: 'JOHN@EXAMPLE.COM',
      });

      expect(user.email).toBe('john@example.com');
    });
  });

  describe('getById', () => {
    it('should return user by id', () => {
      const created = userService.createUser({
        name: 'John',
        email: 'john@example.com',
      });

      const found = userService.getById(created.id);

      expect(found).toEqual(created);
    });

    it('should return undefined for non-existent id', () => {
      const found = userService.getById('non-existent-id');

      expect(found).toBeUndefined();
    });
  });

  describe('listAll', () => {
    it('should return empty array when no users', () => {
      const users = userService.listAll();

      expect(users).toEqual([]);
    });

    it('should return all created users', () => {
      userService.createUser({ name: 'John', email: 'john@example.com' });
      userService.createUser({ name: 'Jane', email: 'jane@example.com' });

      const users = userService.listAll();

      expect(users.length).toBe(2);
    });
  });
});
