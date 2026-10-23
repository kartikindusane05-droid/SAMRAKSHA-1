import { Request, Response } from 'express';
import { emergencyEventService } from '../services/emergency-event.service';
import { sendSuccess, sendError } from '../utils/response';
import { logger } from '../utils/logger';

export class EmergencyEventController {
  async getEvents(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const events = await emergencyEventService.getAllEvents(userId);
      sendSuccess(res, `Retrieved ${events.length} emergency events`, events);
    } catch (err: any) {
      logger.error('Failed to get emergency events:', err.message);
      sendError(res, 'Failed to get emergency events', 500);
    }
  }

  async getEventById(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { id } = req.params;
      const event = await emergencyEventService.getEventById(id, userId);
      if (!event) {
        sendError(res, 'Emergency event not found', 404);
        return;
      }
      sendSuccess(res, 'Emergency event retrieved', event);
    } catch (err: any) {
      logger.error('Failed to get emergency event:', err.message);
      sendError(res, 'Failed to get emergency event', 500, err.message);
    }
  }

  async createEvent(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { latitude, longitude, eventType, address, notes, timestamp, id } = req.body;
      if (latitude === undefined || longitude === undefined) {
        sendError(res, 'latitude and longitude are required', 400);
        return;
      }
      const event = await emergencyEventService.createEvent(userId, {
        id,
        latitude,
        longitude,
        eventType,
        address,
        notes,
        timestamp
      });
      sendSuccess(res, 'Emergency event created and dispatched', event, 201);
    } catch (err: any) {
      logger.error('Failed to create emergency event:', err.message);
      sendError(res, 'Failed to create emergency event', 500);
    }
  }

  async updateStatus(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { id } = req.params;
      const { status, resolvedAt } = req.body;
      if (!status) {
        sendError(res, 'status is required', 400);
        return;
      }
      const updated = await emergencyEventService.updateStatus(id, userId, { status, resolvedAt });
      if (!updated) {
        sendError(res, 'Emergency event not found', 404);
        return;
      }
      sendSuccess(res, `Emergency event marked as ${status}`, updated);
    } catch (err: any) {
      logger.error('Failed to update emergency event status:', err.message);
      sendError(res, 'Failed to update emergency event status', 500, err.message);
    }
  }

  async syncEvents(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const incoming = Array.isArray(req.body) ? req.body : req.body.events || [];
      const synced = await emergencyEventService.syncEvents(userId, incoming);
      sendSuccess(res, `Synchronized ${synced.length} emergency events`, synced);
    } catch (err: any) {
      logger.error('Failed to sync emergency events:', err.message);
      sendError(res, 'Failed to synchronize emergency events', 500);
    }
  }
}

export const emergencyEventController = new EmergencyEventController();
