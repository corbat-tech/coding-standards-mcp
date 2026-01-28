import express, { Request, Response } from 'express';
import { TaskService } from './TaskService';

const app = express();
app.use(express.json());

const taskService = new TaskService();

app.post('/tasks', (req: Request, res: Response) => {
  try {
    const task = taskService.create(req.body);
    res.status(201).json(task);
  } catch (error) {
    res.status(400).json({ error: (error as Error).message });
  }
});

app.get('/tasks', (req: Request, res: Response) => {
  const tasks = taskService.getAll();
  res.json(tasks);
});

app.get('/tasks/:id', (req: Request, res: Response) => {
  const task = taskService.getById(req.params.id);
  if (!task) {
    return res.status(404).json({ error: 'Task not found' });
  }
  res.json(task);
});

app.delete('/tasks/:id', (req: Request, res: Response) => {
  const deleted = taskService.delete(req.params.id);
  if (!deleted) {
    return res.status(404).json({ error: 'Task not found' });
  }
  res.status(204).send();
});

export default app;
