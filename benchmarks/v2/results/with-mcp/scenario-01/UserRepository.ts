import { User } from './User';

export interface UserRepository {
  findById(id: string): User | null;
  findAll(): User[];
  save(user: User): void;
  existsByEmail(email: string): boolean;
}
