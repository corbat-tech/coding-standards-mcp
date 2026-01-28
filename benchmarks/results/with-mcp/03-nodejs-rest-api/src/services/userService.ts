import { ConflictError, NotFoundError } from '../domain/errors';
import { UserRepository } from '../repositories/userRepository';
import { UpdateUserDto, UserResponse } from '../types/user';

export class UserService {
  constructor(private readonly userRepository: UserRepository) {}

  async getProfile(userId: string): Promise<UserResponse> {
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }
    return this.toUserResponse(user);
  }

  async updateProfile(userId: string, dto: UpdateUserDto): Promise<UserResponse> {
    const user = await this.userRepository.findById(userId);
    if (!user) {
      throw new NotFoundError('User');
    }

    if (dto.email && dto.email !== user.email) {
      await this.ensureEmailNotTaken(dto.email);
    }

    const updated = await this.userRepository.update(userId, {
      ...(dto.name && { name: dto.name }),
      ...(dto.email && { email: dto.email.toLowerCase() }),
    });

    if (!updated) {
      throw new NotFoundError('User');
    }

    return this.toUserResponse(updated);
  }

  private async ensureEmailNotTaken(email: string): Promise<void> {
    const exists = await this.userRepository.existsByEmail(email);
    if (exists) {
      throw new ConflictError('Email already in use');
    }
  }

  private toUserResponse(user: { id: string; email: string; name: string; createdAt: Date; updatedAt: Date }): UserResponse {
    return {
      id: user.id,
      email: user.email,
      name: user.name,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    };
  }
}
