import { Router, Request, Response, NextFunction } from 'express';
import { AuthService } from '../../application/authService.js';
import { LoginSchema } from '../../domain/user.js';

export function createAuthRoutes(authService: AuthService): Router {
  const router = Router();

  router.post('/login', async (req: Request, res: Response, next: NextFunction) => {
    try {
      const dto = LoginSchema.parse(req.body);
      const result = await authService.login(dto);
      res.json(result);
    } catch (error) {
      next(error);
    }
  });

  return router;
}
