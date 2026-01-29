/**
 * User Service Implementation
 * Application layer - business logic
 */

import { randomUUID } from 'crypto';
import {
  IUserService,
  IUserRepository,
  IJwtService,
  IPasswordService,
} from '../types/service.interfaces';
import {
  IUser,
  IUserPublic,
  ICreateUserDto,
  IUpdateUserDto,
  ILoginDto,
  IAuthResponse,
} from '../types/user.types';
import { NotFoundError, ConflictError, UnauthorizedError } from '../types/errors';

export class UserServiceImpl implements IUserService {
  constructor(
    private readonly userRepository: IUserRepository,
    private readonly jwtService: IJwtService,
    private readonly passwordService: IPasswordService
  ) {}

  async createUser(dto: ICreateUserDto): Promise<IUserPublic> {
    const existingUser = await this.userRepository.findByEmail(dto.email);
    if (existingUser) {
      throw new ConflictError('Email already in use');
    }

    const hashedPassword = await this.passwordService.hash(dto.password);
    const user: IUser = {
      id: randomUUID(),
      email: dto.email,
      name: dto.name,
      password: hashedPassword,
      role: dto.role || 'user',
      createdAt: new Date(),
    };

    await this.userRepository.create(user);
    return this.toPublicUser(user);
  }

  async getUserById(id: string): Promise<IUserPublic> {
    const user = await this.userRepository.findById(id);
    if (!user) {
      throw new NotFoundError('User');
    }
    return this.toPublicUser(user);
  }

  async getUserByEmail(email: string): Promise<IUserPublic> {
    const user = await this.userRepository.findByEmail(email);
    if (!user) {
      throw new NotFoundError('User');
    }
    return this.toPublicUser(user);
  }

  async getAllUsers(): Promise<IUserPublic[]> {
    const users = await this.userRepository.findAll();
    return users.map(user => this.toPublicUser(user));
  }

  async updateUser(id: string, dto: IUpdateUserDto): Promise<IUserPublic> {
    const existingUser = await this.userRepository.findById(id);
    if (!existingUser) {
      throw new NotFoundError('User');
    }

    if (dto.email && dto.email !== existingUser.email) {
      const emailTaken = await this.userRepository.findByEmail(dto.email);
      if (emailTaken) {
        throw new ConflictError('Email already in use');
      }
    }

    const updateData: Partial<IUser> = { ...dto };
    if (dto.password) {
      updateData.password = await this.passwordService.hash(dto.password);
    }

    const updated = await this.userRepository.update(id, updateData);
    return this.toPublicUser(updated!);
  }

  async deleteUser(id: string): Promise<void> {
    const exists = await this.userRepository.findById(id);
    if (!exists) {
      throw new NotFoundError('User');
    }
    await this.userRepository.delete(id);
  }

  async login(dto: ILoginDto): Promise<IAuthResponse> {
    const user = await this.userRepository.findByEmail(dto.email);
    if (!user) {
      throw new UnauthorizedError('Invalid credentials');
    }

    const isValidPassword = await this.passwordService.compare(dto.password, user.password);
    if (!isValidPassword) {
      throw new UnauthorizedError('Invalid credentials');
    }

    const token = this.jwtService.generateToken({
      userId: user.id,
      email: user.email,
      role: user.role,
    });

    return {
      user: this.toPublicUser(user),
      token,
    };
  }

  private toPublicUser(user: IUser): IUserPublic {
    return {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
      createdAt: user.createdAt,
    };
  }
}
