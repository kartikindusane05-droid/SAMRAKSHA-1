import { randomUUID } from 'crypto';
import {
  Journey,
  CreateJourneyDto,
  UpdateJourneyLocationDto,
  EndJourneyDto,
  JourneyQueryParams
} from '../models/journey.model';
import { journeyRepository } from '../db/repositories/journey.repository';
import { logger } from '../utils/logger';

class JourneyService {
  private fallbackJourneys: Map<string, Journey> = new Map();

  async getAllJourneys(query: JourneyQueryParams & { userId: string }): Promise<Journey[]> {
    try {
      return await journeyRepository.findAll(query);
    } catch (err: any) {
      logger.warn(`DB getAllJourneys failed (${err.message}). Using fallback.`);
      let list = Array.from(this.fallbackJourneys.values()).filter((j) => j.userId === query.userId);
      if (query.status) {
        list = list.filter((j) => j.status === query.status);
      }
      list.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime());
      if (query.limit && query.limit > 0) {
        list = list.slice(0, query.limit);
      }
      return list;
    }
  }

  async getActiveJourney(userId: string): Promise<Journey | null> {
    try {
      return await journeyRepository.findActive(userId);
    } catch (err: any) {
      logger.warn(`DB getActiveJourney failed (${err.message}). Using fallback.`);
      const all = Array.from(this.fallbackJourneys.values());
      return all.find((j) => j.userId === userId && j.status === 'IN_PROGRESS') || null;
    }
  }

  async getJourneyById(id: string, userId: string): Promise<Journey | null> {
    try {
      return await journeyRepository.findById(id, userId);
    } catch (err: any) {
      logger.warn(`DB getJourneyById failed (${err.message}). Using fallback.`);
      const j = this.fallbackJourneys.get(id);
      return j && j.userId === userId ? j : null;
    }
  }

  async startJourney(userId: string, dto: CreateJourneyDto): Promise<Journey> {
    const id = randomUUID();
    const now = new Date().toISOString();

    const newJourney: Journey = {
      id,
      userId,
      startLocationName: dto.startLocationName,
      endLocationName: dto.endLocationName,
      startTime: now,
      expectedEndTime: dto.expectedEndTime || new Date(Date.now() + 30 * 60 * 1000).toISOString(),
      status: 'IN_PROGRESS',
      startLatitude: dto.startLatitude,
      startLongitude: dto.startLongitude,
      currentLatitude: dto.startLatitude,
      currentLongitude: dto.startLongitude,
      destinationLatitude: dto.destinationLatitude || 0,
      destinationLongitude: dto.destinationLongitude || 0,
      createdAt: now,
      updatedAt: now
    };

    try {
      return await journeyRepository.create(newJourney);
    } catch (err: any) {
      logger.warn(`DB startJourney failed (${err.message}). Using fallback.`);
      for (const [key, j] of this.fallbackJourneys) {
        if (j.userId === userId && j.status === 'IN_PROGRESS') {
          this.fallbackJourneys.set(key, {
            ...j,
            status: 'COMPLETED',
            completedTime: now,
            updatedAt: now
          });
        }
      }
      this.fallbackJourneys.set(id, newJourney);
      return newJourney;
    }
  }

  async updateLocation(
    id: string,
    userId: string,
    dto: UpdateJourneyLocationDto
  ): Promise<Journey | null> {
    try {
      return await journeyRepository.updateLocation(
        id,
        userId,
        dto.currentLatitude,
        dto.currentLongitude
      );
    } catch (err: any) {
      logger.warn(`DB updateLocation failed (${err.message}). Using fallback.`);
      const existing = this.fallbackJourneys.get(id);
      if (!existing || existing.userId !== userId || existing.status !== 'IN_PROGRESS') {
        return null;
      }
      const updated: Journey = {
        ...existing,
        currentLatitude: dto.currentLatitude,
        currentLongitude: dto.currentLongitude,
        updatedAt: new Date().toISOString()
      };
      this.fallbackJourneys.set(id, updated);
      return updated;
    }
  }

  async endJourney(
    id: string,
    userId: string,
    dto: EndJourneyDto
  ): Promise<Journey | null> {
    try {
      return await journeyRepository.endJourney(id, userId, dto.status);
    } catch (err: any) {
      logger.warn(`DB endJourney failed (${err.message}). Using fallback.`);
      const existing = this.fallbackJourneys.get(id);
      if (!existing || existing.userId !== userId) return null;

      const updated: Journey = {
        ...existing,
        status: dto.status,
        completedTime: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      this.fallbackJourneys.set(id, updated);
      return updated;
    }
  }

  async deleteJourney(id: string, userId: string): Promise<boolean> {
    try {
      return await journeyRepository.delete(id, userId);
    } catch (err: any) {
      logger.warn(`DB deleteJourney failed (${err.message}). Using fallback.`);
      const existing = this.fallbackJourneys.get(id);
      if (existing && existing.userId === userId) {
        return this.fallbackJourneys.delete(id);
      }
      return false;
    }
  }

  async syncJourneys(userId: string, incomingList: Journey[]): Promise<Journey[]> {
    try {
      return await journeyRepository.syncBatch(userId, incomingList);
    } catch (err: any) {
      logger.warn(`DB syncJourneys failed (${err.message}). Using fallback.`);
      const now = new Date().toISOString();
      for (const item of incomingList) {
        const id = item.id || randomUUID();
        this.fallbackJourneys.set(id, {
          ...item,
          id,
          userId,
          createdAt: item.createdAt || now,
          updatedAt: item.updatedAt || now
        });
      }
      return this.getAllJourneys({ userId });
    }
  }
}

export const journeyService = new JourneyService();
