import { Request, Response, NextFunction } from 'express';
import { authService } from '../services/auth.service';
import { sendError } from '../utils/response';

export interface AuthenticatedRequest extends Request {
  userId?: string;
  token?: string;
}

/**
 * Middleware ensuring valid session authorization via Bearer token.
 */
export const requireAuth = async (
  req: AuthenticatedRequest,
  res: Response,
  next: NextFunction
): Promise<void> => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    sendError(res, 'Authorization token missing or invalid format', 401);
    return;
  }

  const token = authHeader.substring(7).trim();
  const userId = await authService.verifyToken(token);

  if (!userId) {
    sendError(res, 'Session expired or invalid authorization token', 401);
    return;
  }

  req.userId = userId;
  req.token = token;
  next();
};
