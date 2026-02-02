import { Router, Request, Response, NextFunction } from 'express';
import { UserService } from '../../application/userService.js';
import { CreateUserSchema, UpdateUserSchema } from '../../domain/user.js';
import { AuthenticatedRequest, createAuthMiddleware } from '../middleware/auth.js';
import { AuthService } from '../../application/authService.js';

export function createUserRoutes(userService: UserService, authService: AuthService): Router {
  const router = Router();
  const authMiddleware = createAuthMiddleware(authService);

  router.post('/', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const dto = CreateUserSchema.parse(req.body);
      const user = await userService.create(dto);
      const { passwordHash: _, ...userResponse } = user;
      res.status(201).json(userResponse);
    } catch (error) {
      next(error);
    }
  });

  router.get('/', authMiddleware, async (_req: Request, res: Response, next: NextFunction) => {
    try {
      const users = await userService.getAll();
      res.json(users.map(({ passwordHash: _, ...u }) => u));
    } catch (error) {
      next(error);
    }
  });

  router.get('/:id', authMiddleware, async (req: Request, res: Response, next: NextFunction) => {
    try {
      const user = await userService.getById(req.params.id);
      const { passwordHash: _, ...userResponse } = user;
      res.json(userResponse);
    } catch (error) {
      next(error);
    }
  });

  router.put('/:id', authMiddleware, async (req: AuthenticatedRequest, res: Response, next: NextFunction) => {
    try {
      const dto = UpdateUserSchema.parse(req.body);
      const user = await userService.update(req.params.id, dto);
      const { passwordHash: _, ...userResponse } = user;
      res.json(userResponse);
    } catch (error) {
      next(error);
    }
  });

  router.delete('/:id', authMiddleware, async (req: Request, res: Response, next: NextFunction) => {
    try {
      await userService.delete(req.params.id);
      res.status(204).send();
    } catch (error) {
      next(error);
    }
  });

  return router;
}
