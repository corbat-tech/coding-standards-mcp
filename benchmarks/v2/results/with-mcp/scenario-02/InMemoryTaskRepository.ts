import { Task } from './Task';
import { TaskRepository } from './TaskRepository';

export class InMemoryTaskRepository implements TaskRepository {
  private tasks: Map<string, Task> = new Map();

  findById(id: string): Task | null {
    return this.tasks.get(id) ?? null;
  }

  findAll(): Task[] {
    return Array.from(this.tasks.values());
  }

  save(task: Task): void {
    this.tasks.set(task.id, task);
  }

  delete(id: string): boolean {
    return this.tasks.delete(id);
  }

  clear(): void {
    this.tasks.clear();
  }
}
