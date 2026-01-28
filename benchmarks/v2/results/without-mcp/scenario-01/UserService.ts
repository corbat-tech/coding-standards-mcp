import { User, CreateUserInput } from './User';
import { v4 as uuidv4 } from 'uuid';

export class UserService {
  private users: Map<string, User> = new Map();

  createUser(input: CreateUserInput): User {
    if (!input.name || input.name.trim() === '') {
      throw new Error('Name is required');
    }
    if (!input.email || !this.isValidEmail(input.email)) {
      throw new Error('Valid email is required');
    }

    const user: User = {
      id: uuidv4(),
      name: input.name.trim(),
      email: input.email.toLowerCase(),
      createdAt: new Date(),
    };

    this.users.set(user.id, user);
    return user;
  }

  getById(id: string): User | undefined {
    return this.users.get(id);
  }

  listAll(): User[] {
    return Array.from(this.users.values());
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }
}
