export class UserNotFoundError extends Error {
  constructor(public readonly userId: string) {
    super(`User not found: ${userId}`);
    this.name = 'UserNotFoundError';
  }
}

export class UserAlreadyExistsError extends Error {
  constructor(public readonly email: string) {
    super(`User with email already exists: ${email}`);
    this.name = 'UserAlreadyExistsError';
  }
}

export class InvalidUserInputError extends Error {
  constructor(public readonly field: string, public readonly reason: string) {
    super(`Invalid ${field}: ${reason}`);
    this.name = 'InvalidUserInputError';
  }
}
