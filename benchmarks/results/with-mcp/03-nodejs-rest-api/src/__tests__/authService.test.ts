import { AuthService } from '../services/authService';
import { InMemoryUserRepository, UserRepository } from '../repositories/userRepository';
import { ConflictError, UnauthorizedError, ValidationError } from '../domain/errors';

describe('AuthService', () => {
  let authService: AuthService;
  let userRepository: UserRepository;

  beforeEach(() => {
    userRepository = new InMemoryUserRepository();
    authService = new AuthService(userRepository);
  });

  describe('register', () => {
    const validDto = {
      email: 'test@example.com',
      password: 'Password123',
      name: 'Test User',
    };

    it('should_register_user_when_valid_data', async () => {
      // Arrange & Act
      const result = await authService.register(validDto);

      // Assert
      expect(result.user.email).toBe('test@example.com');
      expect(result.token).toBeDefined();
    });

    it('should_return_jwt_token_when_registered', async () => {
      // Arrange & Act
      const result = await authService.register(validDto);

      // Assert
      expect(result.token).toMatch(/^[\w-]+\.[\w-]+\.[\w-]+$/);
    });

    it('should_throw_when_email_already_exists', async () => {
      // Arrange
      await authService.register(validDto);

      // Act & Assert
      await expect(authService.register(validDto))
        .rejects.toThrow(ConflictError);
    });

    it('should_throw_when_password_too_short', async () => {
      // Arrange
      const dto = { ...validDto, password: 'Pass1' };

      // Act & Assert
      await expect(authService.register(dto))
        .rejects.toThrow(ValidationError);
    });

    it('should_throw_when_password_missing_uppercase', async () => {
      // Arrange
      const dto = { ...validDto, password: 'password123' };

      // Act & Assert
      await expect(authService.register(dto))
        .rejects.toThrow(ValidationError);
    });

    it('should_throw_when_password_missing_number', async () => {
      // Arrange
      const dto = { ...validDto, password: 'Passwordonly' };

      // Act & Assert
      await expect(authService.register(dto))
        .rejects.toThrow(ValidationError);
    });

    it('should_normalize_email_to_lowercase', async () => {
      // Arrange
      const dto = { ...validDto, email: 'TEST@EXAMPLE.COM' };

      // Act
      const result = await authService.register(dto);

      // Assert
      expect(result.user.email).toBe('test@example.com');
    });
  });

  describe('login', () => {
    const registerDto = {
      email: 'test@example.com',
      password: 'Password123',
      name: 'Test User',
    };

    beforeEach(async () => {
      await authService.register(registerDto);
    });

    it('should_return_token_when_valid_credentials', async () => {
      // Arrange
      const loginDto = { email: 'test@example.com', password: 'Password123' };

      // Act
      const result = await authService.login(loginDto);

      // Assert
      expect(result.token).toBeDefined();
    });

    it('should_throw_when_email_not_found', async () => {
      // Arrange
      const loginDto = { email: 'notfound@example.com', password: 'Password123' };

      // Act & Assert
      await expect(authService.login(loginDto))
        .rejects.toThrow(UnauthorizedError);
    });

    it('should_throw_when_password_incorrect', async () => {
      // Arrange
      const loginDto = { email: 'test@example.com', password: 'WrongPassword1' };

      // Act & Assert
      await expect(authService.login(loginDto))
        .rejects.toThrow(UnauthorizedError);
    });
  });

  describe('verifyToken', () => {
    it('should_return_payload_when_valid_token', async () => {
      // Arrange
      const { token } = await authService.register({
        email: 'test@example.com',
        password: 'Password123',
        name: 'Test',
      });

      // Act
      const payload = authService.verifyToken(token);

      // Assert
      expect(payload.email).toBe('test@example.com');
    });

    it('should_throw_when_invalid_token', () => {
      // Arrange & Act & Assert
      expect(() => authService.verifyToken('invalid-token'))
        .toThrow(UnauthorizedError);
    });
  });
});
