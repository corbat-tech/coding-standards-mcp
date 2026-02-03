import { HttpException, HttpStatus } from '@nestjs/common';

export class ArticleNotFoundException extends HttpException {
  constructor(id: string) {
    super(`Article with id ${id} not found`, HttpStatus.NOT_FOUND);
  }
}

export class ArticleValidationException extends HttpException {
  constructor(message: string) {
    super(message, HttpStatus.BAD_REQUEST);
  }
}

export class ArticleAlreadyPublishedException extends HttpException {
  constructor(id: string) {
    super(`Article ${id} is already published`, HttpStatus.CONFLICT);
  }
}
