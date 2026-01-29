export type UserRole = 'admin' | 'user' | 'moderator';

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  createdAt: Date;
}

export interface CreateUserDTO {
  email: string;
  name: string;
  role?: UserRole;
}

export interface UpdateUserDTO {
  email?: string;
  name?: string;
  role?: UserRole;
}
