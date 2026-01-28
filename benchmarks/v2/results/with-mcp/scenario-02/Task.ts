export interface Task {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly completed: boolean;
  readonly createdAt: Date;
}

export interface CreateTaskInput {
  title: string;
  description?: string;
}

export function createTask(
  id: string,
  input: CreateTaskInput,
  createdAt: Date = new Date()
): Task {
  return {
    id,
    title: input.title.trim(),
    description: input.description?.trim() ?? '',
    completed: false,
    createdAt,
  };
}
