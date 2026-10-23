import { Request, Response, NextFunction } from 'express';
import { authService } from '../services/auth.service';
import { AuthenticatedRequest } from '../middleware/auth.middleware';
import { sendSuccess, sendError } from '../utils/response';
import { logger } from '../utils/logger';

export class AuthController {
  /**
   * POST /api/auth/signup
   */
  public async signup(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { fullName, email, phone, password } = req.body;

      // Basic input validation
      if (!fullName || !email || !phone || !password) {
        sendError(res, 'fullName, email, phone, and password are all required fields', 400);
        return;
      }

      if (password.length < 6) {
        sendError(res, 'Password must be at least 6 characters long', 400);
        return;
      }

      const result = await authService.signup({
        fullName,
        email,
        phone,
        password
      });

      logger.info(`User signed up successfully: ${result.user.email} (${result.user.id})`);
      sendSuccess(res, 'Account created successfully', result, 201);
    } catch (error: any) {
      logger.warn(`Signup failed: ${error.message}`);
      if (error.statusCode === 400 || error.message.includes('already exists')) {
        sendError(res, error.message, 400);
      } else {
        sendError(res, 'Database unavailable or authentication service error', 500, error.message);
      }
    }
  }

  /**
   * POST /api/auth/login
   */
  public async login(req: Request, res: Response, next: NextFunction): Promise<void> {
    try {
      const { identifier, password } = req.body;

      if (!identifier || !password) {
        sendError(res, 'Both identifier (email/phone) and password are required', 400);
        return;
      }

      const result = await authService.login({
        identifier,
        password
      });

      logger.info(`User logged in successfully: ${result.user.email} (${result.user.id})`);
      sendSuccess(res, 'Authentication successful', result, 200);
    } catch (error: any) {
      logger.warn(`Login failed: ${error.message}`);
      if (error.statusCode === 401 || error.message === 'Invalid email/phone or password') {
        sendError(res, 'Invalid email/phone or password', 401);
      } else {
        sendError(res, 'Database unavailable or authentication service error', 500, error.message);
      }
    }
  }

  /**
   * POST /api/auth/logout
   */
  public async logout(req: AuthenticatedRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const token = req.token;
      if (token) {
        await authService.logout(token);
      }

      logger.info(`User logged out session: ${req.userId}`);
      sendSuccess(res, 'Logged out successfully', null, 200);
    } catch (error: any) {
      next(error);
    }
  }

  /**
   * GET /api/auth/profile
   */
  public async getProfile(req: AuthenticatedRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.userId;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const profile = await authService.getProfile(userId);
      if (!profile) {
        sendError(res, 'User profile not found', 404);
        return;
      }

      sendSuccess(res, 'Profile retrieved successfully', profile, 200);
    } catch (error: any) {
      next(error);
    }
  }

  /**
   * PUT /api/auth/profile
   */
  public async updateProfile(req: AuthenticatedRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.userId;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const updated = await authService.updateProfile(userId, req.body);
      if (!updated) {
        sendError(res, 'User profile not found', 404);
        return;
      }

      logger.info(`Profile updated for user: ${userId}`);
      sendSuccess(res, 'Profile updated successfully', updated, 200);
    } catch (error: any) {
      next(error);
    }
  }

  /**
   * POST /api/auth/sync
   * Batch synchronize user profile state from offline client.
   */
  public async syncProfile(req: AuthenticatedRequest, res: Response, next: NextFunction): Promise<void> {
    try {
      const userId = req.userId;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      let profile = await authService.getProfile(userId);
      if (req.body && (req.body.fullName || req.body.phone || req.body.medicalInfo)) {
        const updated = await authService.updateProfile(userId, req.body);
        if (updated) {
          profile = updated;
        }
      }
      if (!profile) {
        sendError(res, 'User profile not found', 404);
        return;
      }
      sendSuccess(res, 'User profile synchronized successfully', profile, 200);
    } catch (error: any) {
      next(error);
    }
  }
}

export const authController = new AuthController();
