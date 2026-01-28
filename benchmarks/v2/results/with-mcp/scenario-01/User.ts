export interface User {
  readonly id: string;
  readonly name: string;
  readonly email: string;
  readonly createdAt: Date;
}

export interface CreateUserInput {
  name: string;
  email: string;
}

export function createUser(
  id: string,
  input: CreateUserInput,
  createdAt: Date = new Date()
): User {
  return {
    id,
    name: input.name.trim(),
    email: input.email.toLowerCase().trim(),
    createdAt,
  };
}
