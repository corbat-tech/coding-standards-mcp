import { v4 as uuidv4 } from 'uuid';
import { User, CreateUserDto, UpdateUserDto } from '../domain/user.js';
import { NotFoundError, ConflictError } from '../domain/errors.js';
import { UserRepository } from '../infrastructure/repository/userRepository.js';

export interface UserService {
  create(dto: CreateUserDto): Promise<User>;
  getById(id: string): Promise<User>;
  getAll(): Promise<User[]>;
  update(id: string, dto: UpdateUserDto): Promise<User>;
  delete(id: string): Promise<void>;
  findByEmail(email: string): Promise<User | undefined>;
}

export class UserServiceImpl implements UserService {
  constructor(private readonly repository: UserRepository) {}

  async create(dto: CreateUserDto): Promise<User> {
    const existing = await this.repository.findByEmail(dto.email);
    if (existing) {
      throw new ConflictError('Email already registered');
    }

    const user: User = {
      id: uuidv4(),
      email: dto.email,
      name: dto.name,
      role: dto.role,
      passwordHash: this.hashPassword(dto.password),
      createdAt: new Date(),
    };

    return this.repository.save(user);
  }

  async getById(id: string): Promise<User> {
    const user = await this.repository.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }
    return user;
  }

  async getAll(): Promise<User[]> {
    return this.repository.findAll();
  }

  async update(id: string, dto: UpdateUserDto): Promise<User> {
    const user = await this.getById(id);

    if (dto.email && dto.email !== user.email) {
      const existing = await this.repository.findByEmail(dto.email);
      if (existing) {
        throw new ConflictError('Email already registered');
      }
    }

    const updated: User = {
      ...user,
      email: dto.email ?? user.email,
      name: dto.name ?? user.name,
      role: dto.role ?? user.role,
    };

    return this.repository.save(updated);
  }

  async delete(id: string): Promise<void> {
    await this.getById(id);
    await this.repository.delete(id);
  }

  async findByEmail(email: string): Promise<User | undefined> {
    return this.repository.findByEmail(email);
  }

  private hashPassword(password: string): string {
    return Buffer.from(password).toString('base64');
  }
}
