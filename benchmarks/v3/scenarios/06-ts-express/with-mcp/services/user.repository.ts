/**
 * In-Memory User Repository Implementation
 * Infrastructure layer - implements repository interface
 */

import { IUserRepository } from '../types/service.interfaces';
import { IUser } from '../types/user.types';

export class InMemoryUserRepository implements IUserRepository {
  private users: Map<string, IUser> = new Map();

  async create(user: IUser): Promise<IUser> {
    this.users.set(user.id, user);
    return user;
  }

  async findById(id: string): Promise<IUser | null> {
    return this.users.get(id) || null;
  }

  async findByEmail(email: string): Promise<IUser | null> {
    for (const user of this.users.values()) {
      if (user.email === email) {
        return user;
      }
    }
    return null;
  }

  async findAll(): Promise<IUser[]> {
    return Array.from(this.users.values());
  }

  async update(id: string, data: Partial<IUser>): Promise<IUser | null> {
    const user = this.users.get(id);
    if (!user) return null;

    const updated = { ...user, ...data };
    this.users.set(id, updated);
    return updated;
  }

  async delete(id: string): Promise<boolean> {
    return this.users.delete(id);
  }
}
