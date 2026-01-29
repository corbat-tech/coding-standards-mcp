/**
 * Service interfaces - Application layer contracts
 * Following Dependency Inversion Principle
 */

import {
  IUser,
  IUserPublic,
  ICreateUserDto,
  IUpdateUserDto,
  ILoginDto,
  IAuthResponse,
  ITokenPayload,
} from './user.types';

export interface IUserRepository {
  create(user: IUser): Promise<IUser>;
  findById(id: string): Promise<IUser | null>;
  findByEmail(email: string): Promise<IUser | null>;
  findAll(): Promise<IUser[]>;
  update(id: string, data: Partial<IUser>): Promise<IUser | null>;
  delete(id: string): Promise<boolean>;
}

export interface IUserService {
  createUser(dto: ICreateUserDto): Promise<IUserPublic>;
  getUserById(id: string): Promise<IUserPublic>;
  getUserByEmail(email: string): Promise<IUserPublic>;
  getAllUsers(): Promise<IUserPublic[]>;
  updateUser(id: string, dto: IUpdateUserDto): Promise<IUserPublic>;
  deleteUser(id: string): Promise<void>;
  login(dto: ILoginDto): Promise<IAuthResponse>;
}

export interface IJwtService {
  generateToken(payload: ITokenPayload): string;
  verifyToken(token: string): ITokenPayload;
  decodeToken(token: string): ITokenPayload | null;
}

export interface IPasswordService {
  hash(password: string): Promise<string>;
  compare(password: string, hash: string): Promise<boolean>;
}
