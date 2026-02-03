import jwt from 'jsonwebtoken';
import { User, LoginDto } from '../domain/user.js';
import { UnauthorizedError } from '../domain/errors.js';
import { UserService } from './userService.js';

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-in-production';
const JWT_EXPIRY = '24h';

export interface AuthService {
  login(dto: LoginDto): Promise<{ token: string; user: Omit<User, 'passwordHash'> }>;
  verifyToken(token: string): { userId: string; role: string };
}

export class AuthServiceImpl implements AuthService {
  constructor(private readonly userService: UserService) {}

  async login(dto: LoginDto): Promise<{ token: string; user: Omit<User, 'passwordHash'> }> {
    const user = await this.userService.findByEmail(dto.email);
    if (!user) {
      throw new UnauthorizedError('Invalid credentials');
    }

    const passwordHash = Buffer.from(dto.password).toString('base64');
    if (user.passwordHash !== passwordHash) {
      throw new UnauthorizedError('Invalid credentials');
    }

    const token = jwt.sign({ userId: user.id, role: user.role }, JWT_SECRET, {
      expiresIn: JWT_EXPIRY,
    });

    const { passwordHash: _, ...userWithoutPassword } = user;
    return { token, user: userWithoutPassword };
  }

  verifyToken(token: string): { userId: string; role: string } {
    try {
      const decoded = jwt.verify(token, JWT_SECRET) as { userId: string; role: string };
      return decoded;
    } catch {
      throw new UnauthorizedError('Invalid token');
    }
  }
}
