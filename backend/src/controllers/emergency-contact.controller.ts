import { Request, Response } from 'express';
import { emergencyContactService } from '../services/emergency-contact.service';
import { sendSuccess, sendError } from '../utils/response';
import { logger } from '../utils/logger';

export class EmergencyContactController {
  async getContacts(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const contacts = await emergencyContactService.getContacts(userId);
      sendSuccess(res, `Retrieved ${contacts.length} emergency contacts`, contacts);
    } catch (err: any) {
      logger.error('Failed to get emergency contacts:', err.message);
      sendError(res, 'Failed to get emergency contacts', 500);
    }
  }

  async getContactById(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { id } = req.params;
      const contact = await emergencyContactService.getContactById(id, userId);
      if (!contact) {
        sendError(res, 'Emergency contact not found', 404);
        return;
      }
      sendSuccess(res, 'Emergency contact retrieved', contact);
    } catch (err: any) {
      logger.error('Failed to get emergency contact:', err.message);
      sendError(res, 'Failed to get emergency contact', 500, err.message);
    }
  }

  async addContact(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { name, phoneNumber, relationship, id } = req.body;
      if (!name || !phoneNumber) {
        sendError(res, 'name and phoneNumber are required', 400);
        return;
      }
      const contact = await emergencyContactService.addContact(userId, {
        id,
        name,
        phoneNumber,
        relationship: relationship || 'Family'
      });
      sendSuccess(res, 'Emergency contact added successfully', contact, 201);
    } catch (err: any) {
      logger.error('Failed to add emergency contact:', err.message);
      sendError(res, 'Failed to add emergency contact', 500);
    }
  }

  async updateContact(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { id } = req.params;
      const updated = await emergencyContactService.updateContact(id, userId, req.body);
      if (!updated) {
        sendError(res, 'Emergency contact not found', 404);
        return;
      }
      sendSuccess(res, 'Emergency contact updated successfully', updated);
    } catch (err: any) {
      logger.error('Failed to update emergency contact:', err.message);
      sendError(res, 'Failed to update emergency contact', 500, err.message);
    }
  }

  async deleteContact(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const { id } = req.params;
      const deleted = await emergencyContactService.deleteContact(id, userId);
      if (!deleted) {
        sendError(res, 'Emergency contact not found', 404);
        return;
      }
      sendSuccess(res, 'Emergency contact deleted successfully');
    } catch (err: any) {
      logger.error('Failed to delete emergency contact:', err.message);
      sendError(res, 'Failed to delete emergency contact', 500, err.message);
    }
  }

  async syncContacts(req: Request, res: Response): Promise<void> {
    try {
      const userId = (req as any).userId as string;
      if (!userId) {
        sendError(res, 'Unauthorized', 401);
        return;
      }
      const incoming = Array.isArray(req.body) ? req.body : req.body.contacts || [];
      const synced = await emergencyContactService.syncContacts(userId, incoming);
      sendSuccess(res, `Successfully synchronized ${synced.length} emergency contacts`, synced);
    } catch (err: any) {
      logger.error('Failed to sync emergency contacts:', err.message);
      sendError(res, 'Failed to synchronize emergency contacts', 500);
    }
  }
}

export const emergencyContactController = new EmergencyContactController();
