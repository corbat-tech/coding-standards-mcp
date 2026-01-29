/**
 * Password Service Implementation
 * Handles password hashing and comparison
 */

import bcrypt from 'bcrypt';
import { IPasswordService } from '../types/service.interfaces';

const SALT_ROUNDS = 10;

export class PasswordServiceImpl implements IPasswordService {
  async hash(password: string): Promise<string> {
    return bcrypt.hash(password, SALT_ROUNDS);
  }

  async compare(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }
}
