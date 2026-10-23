import { query } from '../index';
import { EmergencyEvent, EmergencyEventStatus, EmergencyEventType } from '../../models/emergency-event.model';

export interface EmergencyEventRow {
  id: string;
  user_id: string;
  timestamp: string | number;
  latitude: number;
  longitude: number;
  status: string;
  event_type: string;
  address: string | null;
  notes: string | null;
  resolved_at: string | number | null;
  created_at: Date;
  updated_at: Date;
}

function mapRowToEvent(row: EmergencyEventRow): EmergencyEvent {
  return {
    id: row.id,
    userId: row.user_id,
    timestamp: typeof row.timestamp === 'string' ? parseInt(row.timestamp, 10) : row.timestamp,
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    status: row.status as EmergencyEventStatus,
    eventType: row.event_type as EmergencyEventType,
    address: row.address || undefined,
    notes: row.notes || undefined,
    resolvedAt: row.resolved_at ? (typeof row.resolved_at === 'string' ? parseInt(row.resolved_at, 10) : row.resolved_at) : undefined,
    createdAt: new Date(row.created_at).toISOString(),
    updatedAt: new Date(row.updated_at).toISOString()
  };
}

export class EmergencyEventRepository {
  async findByUserId(userId: string): Promise<EmergencyEvent[]> {
    const text = `
      SELECT * FROM emergency_events 
      WHERE user_id = $1 
      ORDER BY timestamp DESC;
    `;
    const res = await query<EmergencyEventRow>(text, [userId]);
    return res.rows.map(mapRowToEvent);
  }

  async findById(id: string, userId: string): Promise<EmergencyEvent | null> {
    const text = `
      SELECT * FROM emergency_events 
      WHERE id = $1 AND user_id = $2 
      LIMIT 1;
    `;
    const res = await query<EmergencyEventRow>(text, [id, userId]);
    return res.rows[0] ? mapRowToEvent(res.rows[0]) : null;
  }

  async create(event: EmergencyEvent): Promise<EmergencyEvent> {
    const text = `
      INSERT INTO emergency_events (
        id, user_id, timestamp, latitude, longitude, status, event_type, address, notes, resolved_at, created_at, updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
      ON CONFLICT (id) DO UPDATE SET
        user_id = EXCLUDED.user_id,
        timestamp = EXCLUDED.timestamp,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude,
        status = EXCLUDED.status,
        event_type = EXCLUDED.event_type,
        address = EXCLUDED.address,
        notes = EXCLUDED.notes,
        resolved_at = EXCLUDED.resolved_at,
        updated_at = EXCLUDED.updated_at
      RETURNING *;
    `;
    const res = await query<EmergencyEventRow>(text, [
      event.id,
      event.userId,
      event.timestamp,
      event.latitude,
      event.longitude,
      event.status,
      event.eventType,
      event.address || null,
      event.notes || null,
      event.resolvedAt || null,
      event.createdAt || new Date().toISOString(),
      event.updatedAt || new Date().toISOString()
    ]);
    return mapRowToEvent(res.rows[0]);
  }

  async updateStatus(
    id: string,
    userId: string,
    status: EmergencyEventStatus,
    resolvedAt?: number
  ): Promise<EmergencyEvent | null> {
    const resolvedTime = resolvedAt ?? (status === 'RESOLVED' ? Date.now() : null);
    const text = `
      UPDATE emergency_events
      SET status = $1, resolved_at = COALESCE($2, resolved_at), updated_at = NOW()
      WHERE id = $3 AND user_id = $4
      RETURNING *;
    `;
    const res = await query<EmergencyEventRow>(text, [status, resolvedTime, id, userId]);
    return res.rows[0] ? mapRowToEvent(res.rows[0]) : null;
  }

  async syncBatch(userId: string, incomingList: EmergencyEvent[]): Promise<EmergencyEvent[]> {
    for (const item of incomingList) {
      const now = new Date().toISOString();
      const event: EmergencyEvent = {
        id: item.id,
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
      };
      await this.create(event);
    }
    return this.findByUserId(userId);
  }
}

export const emergencyEventRepository = new EmergencyEventRepository();
