/**
 * User Routes/Controller
 * HTTP layer - handles request/response
 */

import { Router, Request, Response, NextFunction } from 'express';
import { IUserService } from '../types/service.interfaces';
import { AuthenticatedRequest, validate } from '../middleware';
import {
  createUserSchema,
  updateUserSchema,
  loginSchema,
  idParamSchema,
} from '../types/validation.schemas';

export function createUserRoutes(
  userService: IUserService,
  authMiddleware: (req: Request, res: Response, next: NextFunction) => void
): Router {
  const router = Router();

  router.post(
    '/register',
    validate(createUserSchema),
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const user = await userService.createUser(req.body);
        res.status(201).json(user);
      } catch (error) {
        next(error);
      }
    }
  );

  router.post(
    '/login',
    validate(loginSchema),
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const result = await userService.login(req.body);
        res.json(result);
      } catch (error) {
        next(error);
      }
    }
  );

  router.get(
    '/',
    authMiddleware,
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const users = await userService.getAllUsers();
        res.json(users);
      } catch (error) {
        next(error);
      }
    }
  );

  router.get(
    '/:id',
    authMiddleware,
    validate(idParamSchema, 'params'),
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const user = await userService.getUserById(req.params.id);
        res.json(user);
      } catch (error) {
        next(error);
      }
    }
  );

  router.put(
    '/:id',
    authMiddleware,
    validate(idParamSchema, 'params'),
    validate(updateUserSchema),
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const user = await userService.updateUser(req.params.id, req.body);
        res.json(user);
      } catch (error) {
        next(error);
      }
    }
  );

  router.delete(
    '/:id',
    authMiddleware,
    validate(idParamSchema, 'params'),
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        await userService.deleteUser(req.params.id);
        res.status(204).send();
      } catch (error) {
        next(error);
      }
    }
  );

  router.get(
    '/me',
    authMiddleware,
    async (req: Request, res: Response, next: NextFunction) => {
      try {
        const authReq = req as AuthenticatedRequest;
        const user = await userService.getUserById(authReq.user.userId);
        res.json(user);
      } catch (error) {
        next(error);
      }
    }
  );

  return router;
}
