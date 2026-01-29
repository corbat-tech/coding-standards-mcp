import { Router } from 'express';
import { userController } from '../controllers/user.controller';
import { authenticate, authorize } from '../middleware/auth.middleware';
import { validate } from '../middleware/validate.middleware';
import { createUserSchema, updateUserSchema, userIdSchema } from '../validators/user.validator';
import { asyncHandler } from '../utils/asyncHandler';

const router = Router();

// All routes require authentication
router.use(authenticate);

// GET /api/users - Get all users
router.get('/', asyncHandler(userController.getAll.bind(userController)));

// GET /api/users/:id - Get user by ID
router.get(
  '/:id',
  validate(userIdSchema, 'params'),
  asyncHandler(userController.getById.bind(userController))
);

// POST /api/users - Create new user (admin only)
router.post(
  '/',
  authorize('admin'),
  validate(createUserSchema),
  asyncHandler(userController.create.bind(userController))
);

// PATCH /api/users/:id - Update user (admin only)
router.patch(
  '/:id',
  authorize('admin'),
  validate(userIdSchema, 'params'),
  validate(updateUserSchema),
  asyncHandler(userController.update.bind(userController))
);

// DELETE /api/users/:id - Delete user (admin only)
router.delete(
  '/:id',
  authorize('admin'),
  validate(userIdSchema, 'params'),
  asyncHandler(userController.delete.bind(userController))
);

export default router;
