import { Router } from 'express';
import { authController } from '../controllers/auth.controller';
import { authenticate } from '../middleware/auth.middleware';
import { validate } from '../middleware/validate.middleware';
import { z } from 'zod';
import { asyncHandler } from '../utils/asyncHandler';

const router = Router();

const loginSchema = z.object({
  email: z.string().email('Invalid email format'),
});

// POST /api/auth/login - Login (get token)
router.post('/login', validate(loginSchema), asyncHandler(authController.login.bind(authController)));

// GET /api/auth/me - Get current user
router.get('/me', authenticate, asyncHandler(authController.me.bind(authController)));

export default router;
