import express from 'express';
import { UserServiceImpl } from './application/userService.js';
import { AuthServiceImpl } from './application/authService.js';
import { InMemoryUserRepository } from './infrastructure/repository/userRepository.js';
import { createUserRoutes } from './infrastructure/routes/userRoutes.js';
import { createAuthRoutes } from './infrastructure/routes/authRoutes.js';
import { errorHandler } from './infrastructure/middleware/errorHandler.js';

export function createApp() {
  const app = express();

  app.use(express.json());

  const userRepository = new InMemoryUserRepository();
  const userService = new UserServiceImpl(userRepository);
  const authService = new AuthServiceImpl(userService);

  app.use('/api/auth', createAuthRoutes(authService));
  app.use('/api/users', createUserRoutes(userService, authService));

  app.use(errorHandler);

  return app;
}
