import { HttpException, HttpStatus } from '@nestjs/common';

export class ArticleException extends HttpException {
  constructor(message: string, status: HttpStatus) {
    super(message, status);
  }
}

export class ArticleNotFoundException extends ArticleException {
  constructor(id: string) {
    super(`Article with id '${id}' not found`, HttpStatus.NOT_FOUND);
  }
}

export class ArticleAlreadyExistsException extends ArticleException {
  constructor(title: string) {
    super(`Article with title '${title}' already exists`, HttpStatus.CONFLICT);
  }
}

export class ArticleValidationException extends ArticleException {
  constructor(message: string) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}

export class ArticlePublishException extends ArticleException {
  constructor(id: string, reason: string) {
    super(`Cannot publish article '${id}': ${reason}`, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
