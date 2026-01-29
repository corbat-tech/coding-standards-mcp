/**
 * JWT Service Implementation
 * Handles token generation and verification
 */

import jwt, { JwtPayload } from 'jsonwebtoken';
import { IJwtService } from '../types/service.interfaces';
import { ITokenPayload } from '../types/user.types';
import { UnauthorizedError } from '../types/errors';

const TOKEN_EXPIRY = '24h';

export class JwtServiceImpl implements IJwtService {
  constructor(private readonly secret: string) {}

  generateToken(payload: ITokenPayload): string {
    return jwt.sign(payload, this.secret, { expiresIn: TOKEN_EXPIRY });
  }

  verifyToken(token: string): ITokenPayload {
    try {
      const decoded = jwt.verify(token, this.secret) as JwtPayload & ITokenPayload;
      return {
        userId: decoded.userId,
        email: decoded.email,
        role: decoded.role,
      };
    } catch {
      throw new UnauthorizedError('Invalid or expired token');
    }
  }

  decodeToken(token: string): ITokenPayload | null {
    try {
      const decoded = jwt.decode(token) as JwtPayload & ITokenPayload | null;
      if (!decoded) return null;
      return {
        userId: decoded.userId,
        email: decoded.email,
        role: decoded.role,
      };
    } catch {
      return null;
    }
  }
}
