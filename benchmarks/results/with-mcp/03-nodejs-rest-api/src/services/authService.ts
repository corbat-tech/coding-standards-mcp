import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { config } from '../config';
import { ConflictError, UnauthorizedError, ValidationError } from '../domain/errors';
import { UserRepository } from '../repositories/userRepository';
import {
  AuthResponse,
  CreateUserDto,
  JwtPayload,
  LoginDto,
  User,
  UserResponse,
} from '../types/user';

export class AuthService {
  constructor(private readonly userRepository: UserRepository) {}

  async register(dto: CreateUserDto): Promise<AuthResponse> {
    this.validatePassword(dto.password);
    await this.ensureEmailNotTaken(dto.email);

    const user = await this.createUser(dto);
    const token = this.generateToken(user);

    return { token, user: this.toUserResponse(user) };
  }

  async login(dto: LoginDto): Promise<AuthResponse> {
    const user = await this.userRepository.findByEmail(dto.email);
    if (!user) {
      throw new UnauthorizedError('Invalid credentials');
    }

    const isValidPassword = await bcrypt.compare(dto.password, user.passwordHash);
    if (!isValidPassword) {
      throw new UnauthorizedError('Invalid credentials');
    }

    const token = this.generateToken(user);
    return { token, user: this.toUserResponse(user) };
  }

  verifyToken(token: string): JwtPayload {
    try {
      return jwt.verify(token, config.jwtSecret) as JwtPayload;
    } catch {
      throw new UnauthorizedError('Invalid or expired token');
    }
  }

  private validatePassword(password: string): void {
    const minLength = 8;
    const hasUppercase = /[A-Z]/.test(password);
    const hasNumber = /[0-9]/.test(password);

    if (password.length < minLength) {
      throw new ValidationError(`Password must be at least ${minLength} characters`);
    }
    if (!hasUppercase) {
      throw new ValidationError('Password must contain at least one uppercase letter');
    }
    if (!hasNumber) {
      throw new ValidationError('Password must contain at least one number');
    }
  }

  private async ensureEmailNotTaken(email: string): Promise<void> {
    const exists = await this.userRepository.existsByEmail(email);
    if (exists) {
      throw new ConflictError('Email already registered');
    }
  }

  private async createUser(dto: CreateUserDto): Promise<User> {
    const passwordHash = await bcrypt.hash(dto.password, config.bcryptRounds);
    const now = new Date();

    const user: User = {
      id: uuidv4(),
      email: dto.email.toLowerCase(),
      passwordHash,
      name: dto.name,
      createdAt: now,
      updatedAt: now,
    };

    return this.userRepository.save(user);
  }

  private generateToken(user: User): string {
    const payload: JwtPayload = {
      userId: user.id,
      email: user.email,
    };
    return jwt.sign(payload, config.jwtSecret, { expiresIn: config.jwtExpiresIn });
  }

  private toUserResponse(user: User): UserResponse {
    return {
      id: user.id,
      email: user.email,
      name: user.name,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    };
  }
}
