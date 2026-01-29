import { z } from 'zod';

export const userRoleSchema = z.enum(['admin', 'user', 'moderator']);

export const createUserSchema = z.object({
  email: z.string().email('Invalid email format'),
  name: z.string().min(1, 'Name is required').max(100, 'Name must be less than 100 characters'),
  role: userRoleSchema.optional().default('user'),
});

export const updateUserSchema = z.object({
  email: z.string().email('Invalid email format').optional(),
  name: z.string().min(1, 'Name is required').max(100, 'Name must be less than 100 characters').optional(),
  role: userRoleSchema.optional(),
}).refine(data => Object.keys(data).length > 0, {
  message: 'At least one field must be provided for update',
});

export const userIdSchema = z.object({
  id: z.string().uuid('Invalid user ID format'),
});

export type CreateUserInput = z.infer<typeof createUserSchema>;
export type UpdateUserInput = z.infer<typeof updateUserSchema>;
