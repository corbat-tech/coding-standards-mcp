import { z } from 'zod';

/**
 * Zod schemas for tool input validation.
 * Centralized schemas used by handlers.
 */

export const TaskTypeSchema = z.enum([
  'feature',
  'bugfix',
  'refactor',
  'test',
  'documentation',
  'security',
  'performance',
  'infrastructure',
]);

export const GetContextSchema = z.object({
  task: z.string().min(1, 'Task description is required'),
  project_dir: z.string().optional(),
});

export const ValidateSchema = z.object({
  code: z.string().min(1, 'Code is required'),
  task_type: TaskTypeSchema.optional(),
});

export const SearchSchema = z.object({
  query: z.string().min(1, 'Search query is required'),
});

export const InitSchema = z.object({
  project_dir: z.string().min(1, 'Project directory is required'),
});

// Type exports for use in handlers
export type GetContextInput = z.infer<typeof GetContextSchema>;
export type ValidateInput = z.infer<typeof ValidateSchema>;
export type SearchInput = z.infer<typeof SearchSchema>;
export type InitInput = z.infer<typeof InitSchema>;
export type TaskTypeInput = z.infer<typeof TaskTypeSchema>;
