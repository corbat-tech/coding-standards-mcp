import { Request, Response } from 'express';
import { AuthService } from '../services/authService';
import { AuthResponse, CreateUserDto, LoginDto } from '../types/user';

export class AuthController {
  constructor(private readonly authService: AuthService) {}

  register = async (
    req: Request<unknown, AuthResponse, CreateUserDto>,
    res: Response<AuthResponse>
  ): Promise<void> => {
    const result = await this.authService.register(req.body);
    res.status(201).json(result);
  };

  login = async (
    req: Request<unknown, AuthResponse, LoginDto>,
    res: Response<AuthResponse>
  ): Promise<void> => {
    const result = await this.authService.login(req.body);
    res.status(200).json(result);
  };
}
