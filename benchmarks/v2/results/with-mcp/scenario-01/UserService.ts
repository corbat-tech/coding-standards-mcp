import { User, CreateUserInput, createUser } from './User';
import { UserRepository } from './UserRepository';
import { IdGenerator } from './IdGenerator';
import {
  UserNotFoundError,
  UserAlreadyExistsError,
  InvalidUserInputError,
} from './UserErrors';

export class UserService {
  constructor(
    private readonly repository: UserRepository,
    private readonly idGenerator: IdGenerator
  ) {}

  createUser(input: CreateUserInput): User {
    this.validateInput(input);
    this.ensureEmailNotTaken(input.email);

    const user = createUser(this.idGenerator.generate(), input);
    this.repository.save(user);

    return user;
  }

  getUserById(id: string): User {
    const user = this.repository.findById(id);
    if (!user) {
      throw new UserNotFoundError(id);
    }
    return user;
  }

  listAllUsers(): User[] {
    return this.repository.findAll();
  }

  private validateInput(input: CreateUserInput): void {
    if (!input.name || input.name.trim().length === 0) {
      throw new InvalidUserInputError('name', 'cannot be empty');
    }
    if (!input.email || !this.isValidEmail(input.email)) {
      throw new InvalidUserInputError('email', 'invalid email format');
    }
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  private ensureEmailNotTaken(email: string): void {
    if (this.repository.existsByEmail(email.toLowerCase().trim())) {
      throw new UserAlreadyExistsError(email);
    }
  }
}
