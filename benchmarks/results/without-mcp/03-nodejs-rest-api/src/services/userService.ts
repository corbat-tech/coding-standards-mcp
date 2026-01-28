import { userRepository } from '../repositories/userRepository';
import { UpdateUserDTO, User, UserResponse } from '../types/user';

export class UserService {
  async getProfile(userId: string): Promise<UserResponse> {
    const user = await userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    return this.toUserResponse(user);
  }

  async updateProfile(userId: string, dto: UpdateUserDTO): Promise<UserResponse> {
    const user = await userRepository.findById(userId);
    if (!user) {
      throw new Error('User not found');
    }

    // Check if email is being changed and if it's already taken
    if (dto.email && dto.email.toLowerCase() !== user.email) {
      const existingUser = await userRepository.findByEmail(dto.email);
      if (existingUser) {
        throw new Error('Email already in use');
      }
    }

    const updates: Partial<User> = {};
    if (dto.email) {
      updates.email = dto.email.toLowerCase();
    }
    if (dto.name) {
      updates.name = dto.name;
    }

    const updatedUser = await userRepository.update(userId, updates);
    if (!updatedUser) {
      throw new Error('Failed to update user');
    }

    return this.toUserResponse(updatedUser);
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

export const userService = new UserService();
