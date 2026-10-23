import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth.middleware';
import { incidentReportService } from '../services/incident-report.service';
import { sendSuccess, sendError } from '../utils/response';
import { logger } from '../utils/logger';

const DEV_DEFAULT_ADMIN_KEY = 'samraksha_admin_secret_key';

function getAdminSecret(): string | null {
  const configured = process.env.ADMIN_API_KEY;
  if (process.env.NODE_ENV === 'production') {
    if (!configured || configured.trim() === '' || configured === DEV_DEFAULT_ADMIN_KEY) {
      logger.error('CRITICAL: ADMIN_API_KEY must be securely configured in production and cannot use development default.');
      return null;
    }
    return configured;
  }
  return configured || DEV_DEFAULT_ADMIN_KEY;
}

export class IncidentReportController {
  async create(req: AuthenticatedRequest, res: Response): Promise<void> {
    try {
      const userId = req.userId;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }

      const result = await incidentReportService.createReport(userId, req.body);

      if (result.isDuplicateWarning) {
        res.status(200).json({
          success: false,
          warning: result.warning,
          message: result.message,
          data: {
            isPotentialDuplicate: true
          },
          timestamp: new Date().toISOString()
        });
        return;
      }

      sendSuccess(
        res,
        'Incident report submitted successfully and is pending review.',
        result.report,
        201
      );
    } catch (error: any) {
      const msg = error.message || 'Failed to submit incident report';
      if (msg.startsWith('VALIDATION_ERROR:')) {
        sendError(res, msg.replace('VALIDATION_ERROR: ', ''), 400);
      } else if (msg.startsWith('RATE_LIMIT_EXCEEDED:')) {
        sendError(res, msg.replace('RATE_LIMIT_EXCEEDED: ', ''), 429);
      } else {
        logger.error(`Error creating incident report: ${msg}`);
        sendError(res, 'Internal server error while processing incident report.', 500);
      }
    }
  }

  async getMyReports(req: AuthenticatedRequest, res: Response): Promise<void> {
    try {
      const userId = req.userId;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }

      const reports = await incidentReportService.getUserReports(userId);
      sendSuccess(res, 'Incident reports retrieved successfully', reports);
    } catch (error: any) {
      logger.error(`Error retrieving incident reports for user: ${error.message}`);
      sendError(res, 'Failed to retrieve incident reports', 500);
    }
  }

  async getById(req: AuthenticatedRequest, res: Response): Promise<void> {
    try {
      const userId = req.userId;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }

      const adminHeader = req.headers['x-admin-key'];
      const activeAdminSecret = getAdminSecret();
      const isAdmin = Boolean(adminHeader && activeAdminSecret && adminHeader === activeAdminSecret);

      const report = await incidentReportService.getReportById(req.params.id, userId, isAdmin);
      if (!report) {
        sendError(res, 'Incident report not found', 404);
        return;
      }

      sendSuccess(res, 'Incident report retrieved successfully', report);
    } catch (error: any) {
      const msg = error.message || '';
      if (msg.startsWith('FORBIDDEN:')) {
        sendError(res, 'You are not authorized to view this incident report.', 403);
      } else {
        sendError(res, 'Failed to retrieve incident report', 500);
      }
    }
  }

  async updateStatus(req: AuthenticatedRequest, res: Response): Promise<void> {
    try {
      const adminHeader = req.headers['x-admin-key'];
      const activeAdminSecret = getAdminSecret();
      if (!activeAdminSecret) {
        sendError(res, 'Server configuration error: ADMIN_API_KEY must be configured in production', 500);
        return;
      }
      if (!adminHeader || adminHeader !== activeAdminSecret) {
        sendError(res, 'Unauthorized: Valid X-Admin-Key is required for moderation', 403);
        return;
      }

      const { id } = req.params;
      const { status, moderatorNotes } = req.body;

      if (!status) {
        sendError(res, 'Status is required (PENDING_REVIEW, VERIFIED, or REJECTED)', 400);
        return;
      }

      const updated = await incidentReportService.updateReportStatus(
        id,
        status,
        moderatorNotes,
        req.userId || undefined
      );

      if (!updated) {
        sendError(res, 'Incident report not found', 404);
        return;
      }

      sendSuccess(res, `Incident report status updated to ${status}`, updated);
    } catch (error: any) {
      const msg = error.message || '';
      if (msg.startsWith('VALIDATION_ERROR:')) {
        sendError(res, msg.replace('VALIDATION_ERROR: ', ''), 400);
      } else {
        logger.error(`Error updating incident report status: ${msg}`);
        sendError(res, 'Failed to update incident report status', 500);
      }
    }
  }
}

export const incidentReportController = new IncidentReportController();
