import { Task } from './Task';

export interface TaskRepository {
  findById(id: string): Task | null;
  findAll(): Task[];
  save(task: Task): void;
  delete(id: string): boolean;
}
