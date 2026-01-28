import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { config } from '../config';
import { userRepository } from '../repositories/userRepository';
import { CreateUserDTO, LoginDTO, User, UserResponse, AuthResponse } from '../types/user';

export class AuthService {
  async register(dto: CreateUserDTO): Promise<AuthResponse> {
    // Check if email already exists
    const existingUser = await userRepository.findByEmail(dto.email);
    if (existingUser) {
      throw new Error('Email already registered');
    }

    // Hash password
    const hashedPassword = await bcrypt.hash(dto.password, config.bcryptRounds);

    // Create user
    const user: User = {
      id: uuidv4(),
      email: dto.email.toLowerCase(),
      password: hashedPassword,
      name: dto.name,
      createdAt: new Date(),
      updatedAt: new Date(),
    };

    await userRepository.save(user);

    // Generate token
    const token = this.generateToken(user.id);

    return {
      token,
      user: this.toUserResponse(user),
    };
  }

  async login(dto: LoginDTO): Promise<AuthResponse> {
    // Find user by email
    const user = await userRepository.findByEmail(dto.email);
    if (!user) {
      throw new Error('Invalid credentials');
    }

    // Verify password
    const isPasswordValid = await bcrypt.compare(dto.password, user.password);
    if (!isPasswordValid) {
      throw new Error('Invalid credentials');
    }

    // Generate token
    const token = this.generateToken(user.id);

    return {
      token,
      user: this.toUserResponse(user),
    };
  }

  verifyToken(token: string): { userId: string } {
    try {
      const decoded = jwt.verify(token, config.jwtSecret) as { userId: string };
      return decoded;
    } catch (error) {
      throw new Error('Invalid token');
    }
  }

  private generateToken(userId: string): string {
    return jwt.sign({ userId }, config.jwtSecret, {
      expiresIn: config.jwtExpiresIn,
    });
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

export const authService = new AuthService();
