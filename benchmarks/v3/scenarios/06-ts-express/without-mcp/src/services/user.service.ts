import { v4 as uuidv4 } from 'uuid';
import { User, CreateUserDTO, UpdateUserDTO } from '../models/user.model';
import { NotFoundError, ConflictError } from '../utils/errors';

// In-memory storage (replace with database in production)
const users: Map<string, User> = new Map();

export class UserService {
  async findAll(): Promise<User[]> {
    return Array.from(users.values());
  }

  async findById(id: string): Promise<User> {
    const user = users.get(id);
    if (!user) {
      throw new NotFoundError(`User with id ${id} not found`);
    }
    return user;
  }

  async findByEmail(email: string): Promise<User | null> {
    for (const user of users.values()) {
      if (user.email === email) {
        return user;
      }
    }
    return null;
  }

  async create(data: CreateUserDTO): Promise<User> {
    const existingUser = await this.findByEmail(data.email);
    if (existingUser) {
      throw new ConflictError(`User with email ${data.email} already exists`);
    }

    const user: User = {
      id: uuidv4(),
      email: data.email,
      name: data.name,
      role: data.role || 'user',
      createdAt: new Date(),
    };

    users.set(user.id, user);
    return user;
  }

  async update(id: string, data: UpdateUserDTO): Promise<User> {
    const user = await this.findById(id);

    if (data.email && data.email !== user.email) {
      const existingUser = await this.findByEmail(data.email);
      if (existingUser) {
        throw new ConflictError(`User with email ${data.email} already exists`);
      }
    }

    const updatedUser: User = {
      ...user,
      ...data,
    };

    users.set(id, updatedUser);
    return updatedUser;
  }

  async delete(id: string): Promise<void> {
    const user = users.get(id);
    if (!user) {
      throw new NotFoundError(`User with id ${id} not found`);
    }
    users.delete(id);
  }

  // Helper method for testing - clears all users
  clearAll(): void {
    users.clear();
  }
}

export const userService = new UserService();
