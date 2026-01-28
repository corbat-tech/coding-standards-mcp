import { User } from './User';
import { UserRepository } from './UserRepository';

export class InMemoryUserRepository implements UserRepository {
  private users: Map<string, User> = new Map();

  findById(id: string): User | null {
    return this.users.get(id) ?? null;
  }

  findAll(): User[] {
    return Array.from(this.users.values());
  }

  save(user: User): void {
    this.users.set(user.id, user);
  }

  existsByEmail(email: string): boolean {
    const normalizedEmail = email.toLowerCase().trim();
    return Array.from(this.users.values()).some(
      (user) => user.email === normalizedEmail
    );
  }

  clear(): void {
    this.users.clear();
  }
}
