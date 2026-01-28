import { Task, CreateTaskInput, createTask } from './Task';
import { TaskRepository } from './TaskRepository';
import { TaskNotFoundError, InvalidTaskInputError } from './TaskErrors';

export interface IdGenerator {
  generate(): string;
}

export class TaskService {
  constructor(
    private readonly repository: TaskRepository,
    private readonly idGenerator: IdGenerator
  ) {}

  createTask(input: CreateTaskInput): Task {
    this.validateInput(input);
    const task = createTask(this.idGenerator.generate(), input);
    this.repository.save(task);
    return task;
  }

  getTaskById(id: string): Task {
    const task = this.repository.findById(id);
    if (!task) {
      throw new TaskNotFoundError(id);
    }
    return task;
  }

  listAllTasks(): Task[] {
    return this.repository.findAll();
  }

  deleteTask(id: string): void {
    const deleted = this.repository.delete(id);
    if (!deleted) {
      throw new TaskNotFoundError(id);
    }
  }

  private validateInput(input: CreateTaskInput): void {
    if (!input.title || input.title.trim().length === 0) {
      throw new InvalidTaskInputError('title', 'cannot be empty');
    }
    if (input.title.trim().length > 200) {
      throw new InvalidTaskInputError('title', 'cannot exceed 200 characters');
    }
  }
}
