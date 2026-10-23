import { query } from '../index';
import { Journey, JourneyStatus, JourneyQueryParams } from '../../models/journey.model';

export interface JourneyRow {
  id: string;
  user_id: string;
  start_location_name: string;
  end_location_name: string;
  start_time: Date;
  expected_end_time: Date | null;
  completed_time: Date | null;
  status: string;
  start_latitude: number;
  start_longitude: number;
  current_latitude: number;
  current_longitude: number;
  destination_latitude: number;
  destination_longitude: number;
  created_at: Date;
  updated_at: Date;
}

function mapRowToJourney(row: JourneyRow): Journey {
  return {
    id: row.id,
    userId: row.user_id,
    startLocationName: row.start_location_name,
    endLocationName: row.end_location_name,
    startTime: new Date(row.start_time).toISOString(),
    expectedEndTime: row.expected_end_time ? new Date(row.expected_end_time).toISOString() : undefined,
    completedTime: row.completed_time ? new Date(row.completed_time).toISOString() : undefined,
    status: row.status as JourneyStatus,
    startLatitude: Number(row.start_latitude),
    startLongitude: Number(row.start_longitude),
    currentLatitude: Number(row.current_latitude),
    currentLongitude: Number(row.current_longitude),
    destinationLatitude: Number(row.destination_latitude),
    destinationLongitude: Number(row.destination_longitude),
    createdAt: new Date(row.created_at).toISOString(),
    updatedAt: new Date(row.updated_at).toISOString()
  };
}

export class JourneyRepository {
  async findAll(queryParams: JourneyQueryParams & { userId: string }): Promise<Journey[]> {
    let sql = `SELECT * FROM journeys WHERE user_id = $1`;
    const params: any[] = [queryParams.userId];

    if (queryParams.status) {
      params.push(queryParams.status);
      sql += ` AND status = $${params.length}`;
    }

    sql += ` ORDER BY start_time DESC`;

    if (queryParams.limit && queryParams.limit > 0) {
      params.push(queryParams.limit);
      sql += ` LIMIT $${params.length}`;
    }

    const res = await query<JourneyRow>(sql, params);
    return res.rows.map(mapRowToJourney);
  }

  async findActive(userId: string): Promise<Journey | null> {
    const text = `
      SELECT * FROM journeys 
      WHERE user_id = $1 AND status = 'IN_PROGRESS' 
      ORDER BY start_time DESC 
      LIMIT 1;
    `;
    const res = await query<JourneyRow>(text, [userId]);
    return res.rows[0] ? mapRowToJourney(res.rows[0]) : null;
  }

  async findById(id: string, userId: string): Promise<Journey | null> {
    const text = `
      SELECT * FROM journeys 
      WHERE id = $1 AND user_id = $2 
      LIMIT 1;
    `;
    const res = await query<JourneyRow>(text, [id, userId]);
    return res.rows[0] ? mapRowToJourney(res.rows[0]) : null;
  }

  async create(journey: Journey): Promise<Journey> {
    // Auto-complete any active journeys for this user first
    await query(
      `UPDATE journeys SET status = 'COMPLETED', completed_time = NOW(), updated_at = NOW() 
       WHERE user_id = $1 AND status = 'IN_PROGRESS';`,
      [journey.userId]
    );

    const text = `
      INSERT INTO journeys (
        id, user_id, start_location_name, end_location_name, start_time, expected_end_time,
        status, start_latitude, start_longitude, current_latitude, current_longitude,
        destination_latitude, destination_longitude, created_at, updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
      ON CONFLICT (id) DO UPDATE SET
        user_id = EXCLUDED.user_id,
        start_location_name = EXCLUDED.start_location_name,
        end_location_name = EXCLUDED.end_location_name,
        start_time = EXCLUDED.start_time,
        expected_end_time = EXCLUDED.expected_end_time,
        current_latitude = EXCLUDED.current_latitude,
        current_longitude = EXCLUDED.current_longitude,
        destination_latitude = EXCLUDED.destination_latitude,
        destination_longitude = EXCLUDED.destination_longitude,
        status = EXCLUDED.status,
        completed_time = EXCLUDED.completed_time,
        updated_at = EXCLUDED.updated_at
      RETURNING *;
    `;
    const res = await query<JourneyRow>(text, [
      journey.id,
      journey.userId,
      journey.startLocationName,
      journey.endLocationName,
      journey.startTime,
      journey.expectedEndTime || null,
      journey.status,
      journey.startLatitude,
      journey.startLongitude,
      journey.currentLatitude,
      journey.currentLongitude,
      journey.destinationLatitude,
      journey.destinationLongitude,
      journey.createdAt || new Date().toISOString(),
      journey.updatedAt || new Date().toISOString()
    ]);
    return mapRowToJourney(res.rows[0]);
  }

  async updateLocation(
    id: string,
    userId: string,
    lat: number,
    lng: number
  ): Promise<Journey | null> {
    const text = `
      UPDATE journeys
      SET current_latitude = $1, current_longitude = $2, updated_at = NOW()
      WHERE id = $3 AND user_id = $4 AND status = 'IN_PROGRESS'
      RETURNING *;
    `;
    const res = await query<JourneyRow>(text, [lat, lng, id, userId]);
    return res.rows[0] ? mapRowToJourney(res.rows[0]) : null;
  }

  async endJourney(
    id: string,
    userId: string,
    status: 'COMPLETED' | 'CANCELLED'
  ): Promise<Journey | null> {
    const text = `
      UPDATE journeys
      SET status = $1, completed_time = NOW(), updated_at = NOW()
      WHERE id = $2 AND user_id = $3
      RETURNING *;
    `;
    const res = await query<JourneyRow>(text, [status, id, userId]);
    return res.rows[0] ? mapRowToJourney(res.rows[0]) : null;
  }

  async delete(id: string, userId: string): Promise<boolean> {
    const text = `
      DELETE FROM journeys 
      WHERE id = $1 AND user_id = $2;
    `;
    const res = await query(text, [id, userId]);
    return (res.rowCount ?? 0) > 0;
  }

  async syncBatch(userId: string, incomingList: Journey[]): Promise<Journey[]> {
    for (const item of incomingList) {
      const now = new Date().toISOString();
      const journey: Journey = {
        ...item,
        id: item.id,
        userId,
        createdAt: item.createdAt || now,
        updatedAt: item.updatedAt || now
      };
      const text = `
        INSERT INTO journeys (
          id, user_id, start_location_name, end_location_name, start_time, expected_end_time,
          completed_time, status, start_latitude, start_longitude, current_latitude, current_longitude,
          destination_latitude, destination_longitude, created_at, updated_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16)
        ON CONFLICT (id) DO UPDATE SET
          user_id = EXCLUDED.user_id,
          start_location_name = EXCLUDED.start_location_name,
          end_location_name = EXCLUDED.end_location_name,
          start_time = EXCLUDED.start_time,
          expected_end_time = EXCLUDED.expected_end_time,
          completed_time = EXCLUDED.completed_time,
          status = EXCLUDED.status,
          start_latitude = EXCLUDED.start_latitude,
          start_longitude = EXCLUDED.start_longitude,
          current_latitude = EXCLUDED.current_latitude,
          current_longitude = EXCLUDED.current_longitude,
          destination_latitude = EXCLUDED.destination_latitude,
          destination_longitude = EXCLUDED.destination_longitude,
          updated_at = EXCLUDED.updated_at;
      `;
      await query(text, [
        journey.id,
        journey.userId,
        journey.startLocationName,
        journey.endLocationName,
        journey.startTime,
        journey.expectedEndTime || null,
        journey.completedTime || null,
        journey.status,
        journey.startLatitude,
        journey.startLongitude,
        journey.currentLatitude,
        journey.currentLongitude,
        journey.destinationLatitude,
        journey.destinationLongitude,
        journey.createdAt,
        journey.updatedAt
      ]);
    }
    return this.findAll({ userId });
  }
}

export const journeyRepository = new JourneyRepository();
