/**
 * User domain types and interfaces
 * Following hexagonal architecture - these are domain layer types
 */

export type UserRole = 'admin' | 'user' | 'moderator';

export interface IUser {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  password: string;
  createdAt: Date;
}

export interface IUserPublic {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  createdAt: Date;
}

export interface ICreateUserDto {
  email: string;
  name: string;
  password: string;
  role?: UserRole;
}

export interface IUpdateUserDto {
  email?: string;
  name?: string;
  password?: string;
  role?: UserRole;
}

export interface ILoginDto {
  email: string;
  password: string;
}

export interface IAuthResponse {
  user: IUserPublic;
  token: string;
}

export interface ITokenPayload {
  userId: string;
  email: string;
  role: UserRole;
}
