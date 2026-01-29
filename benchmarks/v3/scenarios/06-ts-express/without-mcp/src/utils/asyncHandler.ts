import { Request, Response, NextFunction, RequestHandler } from 'express';

type AsyncFunction = (req: Request<any, any, any, any>, res: Response, next: NextFunction) => Promise<unknown>;

export function asyncHandler(fn: AsyncFunction): RequestHandler {
  return (req: Request, res: Response, next: NextFunction): void => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}
