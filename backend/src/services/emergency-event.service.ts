import { randomUUID } from 'crypto';
import {
  EmergencyEvent,
  CreateEmergencyEventDto,
  UpdateEmergencyEventStatusDto
} from '../models/emergency-event.model';
import { emergencyEventRepository } from '../db/repositories/emergency-event.repository';
import { logger } from '../utils/logger';

class EmergencyEventService {
  private fallbackEvents: Map<string, EmergencyEvent> = new Map();

  async getAllEvents(userId: string): Promise<EmergencyEvent[]> {
    try {
      return await emergencyEventRepository.findByUserId(userId);
    } catch (err: any) {
      logger.warn(`DB getAllEvents failed (${err.message}). Using fallback.`);
      return Array.from(this.fallbackEvents.values())
        .filter((e) => e.userId === userId)
        .sort((a, b) => b.timestamp - a.timestamp);
    }
  }

  async getEventById(id: string, userId: string): Promise<EmergencyEvent | null> {
    try {
      return await emergencyEventRepository.findById(id, userId);
    } catch (err: any) {
      logger.warn(`DB getEventById failed (${err.message}). Using fallback.`);
      const ev = this.fallbackEvents.get(id);
      return ev && ev.userId === userId ? ev : null;
    }
  }

  async createEvent(userId: string, dto: CreateEmergencyEventDto): Promise<EmergencyEvent> {
    const id = dto.id || randomUUID();
    const now = new Date().toISOString();
    const event: EmergencyEvent = {
      id,
      userId,
      timestamp: dto.timestamp || Date.now(),
      latitude: dto.latitude,
      longitude: dto.longitude,
      status: dto.status || 'ACTIVE',
      eventType: dto.eventType || 'MANUAL_SOS',
      address: dto.address,
      notes: dto.notes,
      resolvedAt: dto.resolvedAt,
      createdAt: now,
      updatedAt: now
    };

    try {
      return await emergencyEventRepository.create(event);
    } catch (err: any) {
      logger.warn(`DB createEvent failed (${err.message}). Using fallback.`);
      this.fallbackEvents.set(id, event);
      return event;
    }
  }

  async updateStatus(
    id: string,
    userId: string,
    dto: UpdateEmergencyEventStatusDto
  ): Promise<EmergencyEvent | null> {
    try {
      return await emergencyEventRepository.updateStatus(id, userId, dto.status, dto.resolvedAt);
    } catch (err: any) {
      logger.warn(`DB updateStatus failed (${err.message}). Using fallback.`);
      const existing = this.fallbackEvents.get(id);
      if (!existing || existing.userId !== userId) return null;

      const updated: EmergencyEvent = {
        ...existing,
        status: dto.status,
        resolvedAt: dto.resolvedAt ?? (dto.status === 'RESOLVED' ? Date.now() : existing.resolvedAt),
        updatedAt: new Date().toISOString()
      };
      this.fallbackEvents.set(id, updated);
      return updated;
    }
  }

  async syncEvents(userId: string, incomingList: EmergencyEvent[]): Promise<EmergencyEvent[]> {
    try {
      return await emergencyEventRepository.syncBatch(userId, incomingList);
    } catch (err: any) {
      logger.warn(`DB syncEvents failed (${err.message}). Using fallback.`);
      const now = new Date().toISOString();
      for (const item of incomingList) {
        const eventId = item.id || randomUUID();
        this.fallbackEvents.set(eventId, {
          id: eventId,
          userId,
          timestamp: item.timestamp || Date.now(),
          latitude: item.latitude,
          longitude: item.longitude,
          status: item.status || 'ACTIVE',
          eventType: item.eventType || 'MANUAL_SOS',
          address: item.address,
          notes: item.notes,
          resolvedAt: item.resolvedAt,
          createdAt: item.createdAt || now,
          updatedAt: item.updatedAt || now
        });
      }
      return this.getAllEvents(userId);
    }
  }
}

export const emergencyEventService = new EmergencyEventService();
