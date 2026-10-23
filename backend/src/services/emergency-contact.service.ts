import { randomUUID } from 'crypto';
import {
  EmergencyContact,
  CreateEmergencyContactDto,
  UpdateEmergencyContactDto
} from '../models/emergency-contact.model';
import { emergencyContactRepository } from '../db/repositories/emergency-contact.repository';
import { logger } from '../utils/logger';

class EmergencyContactService {
  // In-memory fallback map for offline/testing mode
  private fallbackContacts: Map<string, EmergencyContact> = new Map();

  async getContacts(userId: string): Promise<EmergencyContact[]> {
    try {
      return await emergencyContactRepository.findByUserId(userId);
    } catch (err: any) {
      logger.warn(`DB getContacts failed (${err.message}). Using fallback.`);
      return Array.from(this.fallbackContacts.values())
        .filter((c) => c.userId === userId)
        .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    }
  }

  async getContactById(id: string, userId: string): Promise<EmergencyContact | null> {
    try {
      return await emergencyContactRepository.findById(id, userId);
    } catch (err: any) {
      logger.warn(`DB getContactById failed (${err.message}). Using fallback.`);
      const contact = this.fallbackContacts.get(id);
      return contact && contact.userId === userId ? contact : null;
    }
  }

  async addContact(userId: string, dto: CreateEmergencyContactDto): Promise<EmergencyContact> {
    const id = dto.id || randomUUID();
    const now = new Date().toISOString();
    const contact: EmergencyContact = {
      id,
      userId,
      name: dto.name,
      phoneNumber: dto.phoneNumber,
      relationship: dto.relationship || 'Family',
      createdAt: now,
      updatedAt: now
    };

    try {
      return await emergencyContactRepository.create(contact);
    } catch (err: any) {
      logger.warn(`DB addContact failed (${err.message}). Using fallback.`);
      this.fallbackContacts.set(id, contact);
      return contact;
    }
  }

  async updateContact(
    id: string,
    userId: string,
    dto: UpdateEmergencyContactDto
  ): Promise<EmergencyContact | null> {
    try {
      return await emergencyContactRepository.update(id, userId, dto);
    } catch (err: any) {
      logger.warn(`DB updateContact failed (${err.message}). Using fallback.`);
      const existing = this.fallbackContacts.get(id);
      if (!existing || existing.userId !== userId) return null;

      const updated: EmergencyContact = {
        ...existing,
        name: dto.name ?? existing.name,
        phoneNumber: dto.phoneNumber ?? existing.phoneNumber,
        relationship: dto.relationship ?? existing.relationship,
        updatedAt: new Date().toISOString()
      };
      this.fallbackContacts.set(id, updated);
      return updated;
    }
  }

  async deleteContact(id: string, userId: string): Promise<boolean> {
    try {
      return await emergencyContactRepository.delete(id, userId);
    } catch (err: any) {
      logger.warn(`DB deleteContact failed (${err.message}). Using fallback.`);
      const existing = this.fallbackContacts.get(id);
      if (existing && existing.userId === userId) {
        return this.fallbackContacts.delete(id);
      }
      return false;
    }
  }

  async syncContacts(userId: string, incomingList: EmergencyContact[]): Promise<EmergencyContact[]> {
    try {
      return await emergencyContactRepository.syncBatch(userId, incomingList);
    } catch (err: any) {
      logger.warn(`DB syncContacts failed (${err.message}). Using fallback.`);
      const now = new Date().toISOString();
      for (const item of incomingList) {
        const contactId = item.id || randomUUID();
        this.fallbackContacts.set(contactId, {
          id: contactId,
          userId,
          name: item.name,
          phoneNumber: item.phoneNumber,
          relationship: item.relationship || 'Family',
          createdAt: item.createdAt || now,
          updatedAt: item.updatedAt || now
        });
      }
      return this.getContacts(userId);
    }
  }
}

export const emergencyContactService = new EmergencyContactService();
