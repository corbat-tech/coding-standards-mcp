import express from 'express';
import { config } from './config';
import { AuthController } from './controllers/authController';
import { UserController } from './controllers/userController';
import { createAuthMiddleware } from './middleware/authMiddleware';
import { errorHandler } from './middleware/errorHandler';
import { InMemoryUserRepository } from './repositories/userRepository';
import { createAuthRoutes } from './routes/authRoutes';
import { createUserRoutes } from './routes/userRoutes';
import { AuthService } from './services/authService';
import { UserService } from './services/userService';

function createApp() {
  const app = express();

  // Middleware
  app.use(express.json());

  // Dependencies
  const userRepository = new InMemoryUserRepository();
  const authService = new AuthService(userRepository);
  const userService = new UserService(userRepository);

  // Controllers
  const authController = new AuthController(authService);
  const userController = new UserController(userService);

  // Auth middleware
  const authMiddleware = createAuthMiddleware(authService);

  // Routes
  app.use('/auth', createAuthRoutes(authController));
  app.use('/users', createUserRoutes(userController, authMiddleware));

  // Health check
  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  // Error handling
  app.use(errorHandler);

  return app;
}

const app = createApp();

app.listen(config.port, () => {
  console.log(`Server running on port ${config.port}`);
});

export { createApp };
