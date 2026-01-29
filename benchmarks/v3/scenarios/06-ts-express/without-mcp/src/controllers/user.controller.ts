import { Request, Response } from 'express';
import { userService } from '../services/user.service';
import { CreateUserInput, UpdateUserInput } from '../validators/user.validator';

export class UserController {
  async getAll(_req: Request, res: Response): Promise<void> {
    const users = await userService.findAll();
    res.json({
      status: 'success',
      data: users,
    });
  }

  async getById(req: Request, res: Response): Promise<void> {
    const { id } = req.params;
    const user = await userService.findById(id);
    res.json({
      status: 'success',
      data: user,
    });
  }

  async create(req: Request, res: Response): Promise<void> {
    const data = req.body as CreateUserInput;
    const user = await userService.create(data);
    res.status(201).json({
      status: 'success',
      data: user,
    });
  }

  async update(req: Request, res: Response): Promise<void> {
    const { id } = req.params;
    const data = req.body as UpdateUserInput;
    const user = await userService.update(id, data);
    res.json({
      status: 'success',
      data: user,
    });
  }

  async delete(req: Request, res: Response): Promise<void> {
    const { id } = req.params;
    await userService.delete(id);
    res.status(204).send();
  }
}

export const userController = new UserController();
