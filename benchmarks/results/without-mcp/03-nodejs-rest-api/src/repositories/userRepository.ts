import { User } from '../types/user';

// In-memory storage for demonstration
const users: Map<string, User> = new Map();

export class UserRepository {
  async findById(id: string): Promise<User | null> {
    return users.get(id) || null;
  }

  async findByEmail(email: string): Promise<User | null> {
    for (const user of users.values()) {
      if (user.email.toLowerCase() === email.toLowerCase()) {
        return user;
      }
    }
    return null;
  }

  async existsByEmail(email: string): Promise<boolean> {
    const user = await this.findByEmail(email);
    return user !== null;
  }

  async save(user: User): Promise<User> {
    users.set(user.id, user);
    return user;
  }

  async update(id: string, updates: Partial<User>): Promise<User | null> {
    const user = users.get(id);
    if (!user) {
      return null;
    }

    const updatedUser = {
      ...user,
      ...updates,
      updatedAt: new Date(),
    };

    users.set(id, updatedUser);
    return updatedUser;
  }

  async delete(id: string): Promise<boolean> {
    return users.delete(id);
  }

  // For testing purposes
  clear(): void {
    users.clear();
  }
}

export const userRepository = new UserRepository();
