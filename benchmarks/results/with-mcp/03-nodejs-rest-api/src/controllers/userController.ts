import { Request, Response } from 'express';
import { UnauthorizedError } from '../domain/errors';
import { UserService } from '../services/userService';
import { UpdateUserDto, UserResponse } from '../types/user';

export class UserController {
  constructor(private readonly userService: UserService) {}

  getProfile = async (
    req: Request,
    res: Response<UserResponse>
  ): Promise<void> => {
    const userId = this.getUserId(req);
    const user = await this.userService.getProfile(userId);
    res.status(200).json(user);
  };

  updateProfile = async (
    req: Request<unknown, UserResponse, UpdateUserDto>,
    res: Response<UserResponse>
  ): Promise<void> => {
    const userId = this.getUserId(req);
    const user = await this.userService.updateProfile(userId, req.body);
    res.status(200).json(user);
  };

  private getUserId(req: Request): string {
    if (!req.user?.userId) {
      throw new UnauthorizedError('User not authenticated');
    }
    return req.user.userId;
  }
}
