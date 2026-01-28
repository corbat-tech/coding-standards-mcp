import { Request, Response, Router } from 'express';
import { TaskService } from './TaskService';
import { TaskNotFoundError, InvalidTaskInputError } from './TaskErrors';

export interface CreateTaskRequest {
  title: string;
  description?: string;
}

export interface TaskResponse {
  id: string;
  title: string;
  description: string;
  completed: boolean;
  createdAt: string;
}

export class TaskController {
  public readonly router: Router;

  constructor(private readonly taskService: TaskService) {
    this.router = Router();
    this.setupRoutes();
  }

  private setupRoutes(): void {
    this.router.post('/tasks', this.create.bind(this));
    this.router.get('/tasks', this.list.bind(this));
    this.router.get('/tasks/:id', this.getById.bind(this));
    this.router.delete('/tasks/:id', this.delete.bind(this));
  }

  private create(req: Request, res: Response): void {
    try {
      const input: CreateTaskRequest = req.body;
      const task = this.taskService.createTask(input);
      res.status(201).json(this.toResponse(task));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private list(_req: Request, res: Response): void {
    const tasks = this.taskService.listAllTasks();
    res.json(tasks.map((t) => this.toResponse(t)));
  }

  private getById(req: Request, res: Response): void {
    try {
      const task = this.taskService.getTaskById(req.params.id);
      res.json(this.toResponse(task));
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private delete(req: Request, res: Response): void {
    try {
      this.taskService.deleteTask(req.params.id);
      res.status(204).send();
    } catch (error) {
      this.handleError(error, res);
    }
  }

  private toResponse(task: {
    id: string;
    title: string;
    description: string;
    completed: boolean;
    createdAt: Date;
  }): TaskResponse {
    return {
      id: task.id,
      title: task.title,
      description: task.description,
      completed: task.completed,
      createdAt: task.createdAt.toISOString(),
    };
  }

  private handleError(error: unknown, res: Response): void {
    if (error instanceof TaskNotFoundError) {
      res.status(404).json({ error: error.message });
      return;
    }
    if (error instanceof InvalidTaskInputError) {
      res.status(400).json({ error: error.message });
      return;
    }
    res.status(500).json({ error: 'Internal server error' });
  }
}
