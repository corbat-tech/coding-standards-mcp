import { Router } from 'express';
import { UserController } from '../controllers/userController';
import { validateRequest } from '../middleware/validateRequest';
import { updateProfileValidation } from '../validators/authValidators';

export function createUserRoutes(
  userController: UserController,
  authMiddleware: ReturnType<typeof import('../middleware/authMiddleware').createAuthMiddleware>
): Router {
  const router = Router();

  router.get(
    '/me',
    authMiddleware,
    userController.getProfile
  );

  router.put(
    '/me',
    authMiddleware,
    updateProfileValidation,
    validateRequest,
    userController.updateProfile
  );

  return router;
}
