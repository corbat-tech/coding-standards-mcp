import { Request, Response } from 'express';
import { userService } from '../services/user.service';
import { generateToken } from '../utils/jwt';
import { NotFoundError } from '../utils/errors';

export class AuthController {
  async login(req: Request, res: Response): Promise<void> {
    const { email } = req.body;

    const user = await userService.findByEmail(email);
    if (!user) {
      throw new NotFoundError('User not found');
    }

    const token = generateToken({
      userId: user.id,
      email: user.email,
      role: user.role,
    });

    res.json({
      status: 'success',
      data: {
        token,
        user,
      },
    });
  }

  async me(req: Request, res: Response): Promise<void> {
    if (!req.user) {
      throw new NotFoundError('User not found');
    }

    const user = await userService.findById(req.user.userId);
    res.json({
      status: 'success',
      data: user,
    });
  }
}

export const authController = new AuthController();
