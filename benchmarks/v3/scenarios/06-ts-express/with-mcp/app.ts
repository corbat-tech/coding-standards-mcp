/**
 * Express Application Setup
 * Wires up all dependencies following Dependency Injection
 */

import express, { Application } from 'express';
import { createUserRoutes } from './routes';
import { createAuthMiddleware, errorHandler } from './middleware';
import {
  UserServiceImpl,
  InMemoryUserRepository,
  JwtServiceImpl,
  PasswordServiceImpl,
} from './services';

export interface AppConfig {
  jwtSecret: string;
}

export function createApp(config: AppConfig): Application {
  const app = express();

  // Middleware
  app.use(express.json());

  // Initialize services with dependency injection
  const userRepository = new InMemoryUserRepository();
  const jwtService = new JwtServiceImpl(config.jwtSecret);
  const passwordService = new PasswordServiceImpl();
  const userService = new UserServiceImpl(userRepository, jwtService, passwordService);

  // Auth middleware
  const authMiddleware = createAuthMiddleware(jwtService);

  // Routes
  app.use('/api/users', createUserRoutes(userService, authMiddleware));

  // Health check
  app.get('/health', (req, res) => {
    res.json({ status: 'ok' });
  });

  // Error handler - must be last
  app.use(errorHandler);

  return app;
}

// Main entry point
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'default-secret-key-change-in-production';

if (require.main === module) {
  const app = createApp({ jwtSecret: JWT_SECRET });
  app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
  });
}
