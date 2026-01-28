import { Task, CreateTaskInput } from './Task';
import { v4 as uuidv4 } from 'uuid';

export class TaskService {
  private tasks: Map<string, Task> = new Map();

  create(input: CreateTaskInput): Task {
    if (!input.title || input.title.trim() === '') {
      throw new Error('Title is required');
    }

    const task: Task = {
      id: uuidv4(),
      title: input.title.trim(),
      description: input.description,
      completed: false,
      createdAt: new Date(),
    };

    this.tasks.set(task.id, task);
    return task;
  }

  getAll(): Task[] {
    return Array.from(this.tasks.values());
  }

  getById(id: string): Task | undefined {
    return this.tasks.get(id);
  }

  delete(id: string): boolean {
    return this.tasks.delete(id);
  }
}
