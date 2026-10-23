import { query } from '../index';
import { EmergencyContact } from '../../models/emergency-contact.model';

export interface EmergencyContactRow {
  id: string;
  user_id: string;
  name: string;
  phone_number: string;
  relationship: string;
  created_at: Date;
  updated_at: Date;
}

function mapRowToContact(row: EmergencyContactRow): EmergencyContact {
  return {
    id: row.id,
    userId: row.user_id,
    name: row.name,
    phoneNumber: row.phone_number,
    relationship: row.relationship,
    createdAt: new Date(row.created_at).toISOString(),
    updatedAt: new Date(row.updated_at).toISOString()
  };
}

export class EmergencyContactRepository {
  async findByUserId(userId: string): Promise<EmergencyContact[]> {
    const text = `
      SELECT * FROM emergency_contacts 
      WHERE user_id = $1 
      ORDER BY created_at ASC;
    `;
    const res = await query<EmergencyContactRow>(text, [userId]);
    return res.rows.map(mapRowToContact);
  }

  async findById(id: string, userId: string): Promise<EmergencyContact | null> {
    const text = `
      SELECT * FROM emergency_contacts 
      WHERE id = $1 AND user_id = $2 
      LIMIT 1;
    `;
    const res = await query<EmergencyContactRow>(text, [id, userId]);
    return res.rows[0] ? mapRowToContact(res.rows[0]) : null;
  }

  async create(contact: EmergencyContact): Promise<EmergencyContact> {
    const text = `
      INSERT INTO emergency_contacts (id, user_id, name, phone_number, relationship, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      ON CONFLICT (id) DO UPDATE SET
        user_id = EXCLUDED.user_id,
        name = EXCLUDED.name,
        phone_number = EXCLUDED.phone_number,
        relationship = EXCLUDED.relationship,
        updated_at = EXCLUDED.updated_at
      RETURNING *;
    `;
    const res = await query<EmergencyContactRow>(text, [
      contact.id,
      contact.userId,
      contact.name,
      contact.phoneNumber,
      contact.relationship,
      contact.createdAt || new Date(),
      contact.updatedAt || new Date()
    ]);
    return mapRowToContact(res.rows[0]);
  }

  async update(
    id: string,
    userId: string,
    updates: Partial<EmergencyContact>
  ): Promise<EmergencyContact | null> {
    const existing = await this.findById(id, userId);
    if (!existing) return null;

    const updatedName = updates.name ?? existing.name;
    const updatedPhone = updates.phoneNumber ?? existing.phoneNumber;
    const updatedRelationship = updates.relationship ?? existing.relationship;

    const text = `
      UPDATE emergency_contacts
      SET name = $1, phone_number = $2, relationship = $3, updated_at = NOW()
      WHERE id = $4 AND user_id = $5
      RETURNING *;
    `;
    const res = await query<EmergencyContactRow>(text, [
      updatedName,
      updatedPhone,
      updatedRelationship,
      id,
      userId
    ]);
    return res.rows[0] ? mapRowToContact(res.rows[0]) : null;
  }

  async delete(id: string, userId: string): Promise<boolean> {
    const text = `
      DELETE FROM emergency_contacts 
      WHERE id = $1 AND user_id = $2;
    `;
    const res = await query(text, [id, userId]);
    return (res.rowCount ?? 0) > 0;
  }

  async syncBatch(userId: string, incomingList: EmergencyContact[]): Promise<EmergencyContact[]> {
    for (const item of incomingList) {
      const now = new Date().toISOString();
      const contact: EmergencyContact = {
        id: item.id,
        userId,
        name: item.name,
        phoneNumber: item.phoneNumber,
        relationship: item.relationship || 'Family',
        createdAt: item.createdAt || now,
        updatedAt: item.updatedAt || now
      };
      await this.create(contact);
    }
    return this.findByUserId(userId);
  }
}

export const emergencyContactRepository = new EmergencyContactRepository();
