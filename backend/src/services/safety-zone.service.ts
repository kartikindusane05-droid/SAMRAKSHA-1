import { randomUUID } from 'crypto';
import {
  SafetyZone,
  CreateSafetyZoneDto,
  UpdateSafetyZoneDto,
  SafetyZoneQueryParams
} from '../models/safety-zone.model';
import { safetyZoneRepository } from '../db/repositories/safety-zone.repository';
import { logger } from '../utils/logger';

class SafetyZoneService {
  private fallbackZones: Map<string, SafetyZone> = new Map();

  constructor() {
    this.seedFallbackZones();
  }

  private seedFallbackZones(): void {
    const seedData: CreateSafetyZoneDto[] = [
      {
        name: 'MG Road Tech & Commercial Boulevard',
        latitude: 12.9746,
        longitude: 77.6094,
        radius: 650,
        riskLevel: 'SAFE',
        description: 'Prime metro corridor with continuous 24/7 security patrols, high foot traffic, and extensive public illumination.',
        safetyScore: 94,
        advisory: 'Safe to walk 24/7. Police beat booths active along the main walkway.',
        activePatrols: 4,
        cctvCoverage: 96
      },
      {
        name: 'Civic Hospital & University Perimeter',
        latitude: 12.9660,
        longitude: 77.5990,
        radius: 450,
        riskLevel: 'SAFE',
        description: 'Healthcare hub with 24-hour emergency vehicle access, verified safe havens, and security checkpoints.',
        safetyScore: 89,
        advisory: 'Well-monitored. Dedicated emergency response assistance available.',
        activePatrols: 3,
        cctvCoverage: 88
      },
      {
        name: 'Industrial Transit & Mixed Alleyway',
        latitude: 12.9696,
        longitude: 77.5880,
        radius: 500,
        riskLevel: 'MODERATE_RISK',
        description: 'Commercial warehouse zone with sparse pedestrian movement and reduced lighting after 9:00 PM.',
        safetyScore: 60,
        advisory: 'Caution advised during late night hours. Stick to primary lit roads.',
        activePatrols: 1,
        cctvCoverage: 40
      },
      {
        name: 'Old Railway Canal Underpass',
        latitude: 12.9830,
        longitude: 77.6010,
        radius: 380,
        riskLevel: 'UNSAFE',
        description: 'Poorly lit underpass with minimal CCTV surveillance and multiple incident reports.',
        safetyScore: 28,
        advisory: 'High risk area. Avoid after sunset or travel accompanied.',
        activePatrols: 0,
        cctvCoverage: 15
      }
    ];

    seedData.forEach((item) => {
      const id = randomUUID();
      const now = new Date().toISOString();
      this.fallbackZones.set(id, {
        id,
        ...item,
        createdAt: now,
        updatedAt: now
      });
    });
  }

  async getAllZones(query?: SafetyZoneQueryParams): Promise<SafetyZone[]> {
    try {
      const zones = await safetyZoneRepository.findAll(query);
      if (zones.length > 0) return zones;
      // If table is empty, return fallback seeds
      return Array.from(this.fallbackZones.values());
    } catch (err: any) {
      logger.warn(`DB getAllZones failed (${err.message}). Using fallback.`);
      let result = Array.from(this.fallbackZones.values());
      if (query?.riskLevel) {
        result = result.filter((zone) => zone.riskLevel === query.riskLevel);
      }
      return result;
    }
  }

  async getZoneById(id: string): Promise<SafetyZone | null> {
    try {
      return await safetyZoneRepository.findById(id);
    } catch (err: any) {
      logger.warn(`DB getZoneById failed (${err.message}). Using fallback.`);
      return this.fallbackZones.get(id) || null;
    }
  }

  async createZone(dto: CreateSafetyZoneDto): Promise<SafetyZone> {
    const id = randomUUID();
    const now = new Date().toISOString();
    const zone: SafetyZone = {
      id,
      ...dto,
      createdAt: now,
      updatedAt: now
    };

    try {
      return await safetyZoneRepository.create(zone);
    } catch (err: any) {
      logger.warn(`DB createZone failed (${err.message}). Using fallback.`);
      this.fallbackZones.set(id, zone);
      return zone;
    }
  }

  async updateZone(id: string, dto: UpdateSafetyZoneDto): Promise<SafetyZone | null> {
    try {
      return await safetyZoneRepository.update(id, dto);
    } catch (err: any) {
      logger.warn(`DB updateZone failed (${err.message}). Using fallback.`);
      const existing = this.fallbackZones.get(id);
      if (!existing) return null;
      const updated: SafetyZone = {
        ...existing,
        ...dto,
        updatedAt: new Date().toISOString()
      };
      this.fallbackZones.set(id, updated);
      return updated;
    }
  }

  async syncZones(incomingZones: SafetyZone[]): Promise<SafetyZone[]> {
    try {
      return await safetyZoneRepository.syncBatch(incomingZones);
    } catch (err: any) {
      logger.warn(`DB syncZones failed (${err.message}). Using fallback.`);
      for (const z of incomingZones) {
        const id = z.id || randomUUID();
        this.fallbackZones.set(id, {
          ...z,
          id,
          createdAt: z.createdAt || new Date().toISOString(),
          updatedAt: new Date().toISOString()
        });
      }
      return Array.from(this.fallbackZones.values());
    }
  }

  async deleteZone(id: string): Promise<boolean> {
    try {
      return await safetyZoneRepository.delete(id);
    } catch (err: any) {
      logger.warn(`DB deleteZone failed (${err.message}). Using fallback.`);
      return this.fallbackZones.delete(id);
    }
  }
}

export const safetyZoneService = new SafetyZoneService();
