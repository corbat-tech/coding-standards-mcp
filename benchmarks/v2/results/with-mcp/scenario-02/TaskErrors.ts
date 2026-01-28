export class TaskNotFoundError extends Error {
  constructor(public readonly taskId: string) {
    super(`Task not found: ${taskId}`);
    this.name = 'TaskNotFoundError';
  }
}

export class InvalidTaskInputError extends Error {
  constructor(public readonly field: string, public readonly reason: string) {
    super(`Invalid ${field}: ${reason}`);
    this.name = 'InvalidTaskInputError';
  }
}
