import { Router } from 'express';
import { AuthController } from '../controllers/authController';
import { validateRequest } from '../middleware/validateRequest';
import { loginValidation, registerValidation } from '../validators/authValidators';

export function createAuthRoutes(authController: AuthController): Router {
  const router = Router();

  router.post(
    '/register',
    registerValidation,
    validateRequest,
    authController.register
  );

  router.post(
    '/login',
    loginValidation,
    validateRequest,
    authController.login
  );

  return router;
}
