import { query } from '../index';
import { IncidentReport, IncidentCategory, IncidentStatus } from '../../models/incident-report.model';

export interface IncidentReportRow {
  id: string;
  user_id: string;
  category: string;
  latitude: number;
  longitude: number;
  incident_date: Date | string;
  description: string | null;
  status: string;
  moderator_notes: string | null;
  reviewed_by: string | null;
  reviewed_at: Date | string | null;
  created_at: Date | string;
  updated_at: Date | string;
  distance_meters?: number;
}

function formatDateOnly(val: Date | string): string {
  if (val instanceof Date) {
    const year = val.getFullYear();
    const month = String(val.getMonth() + 1).padStart(2, '0');
    const day = String(val.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  return String(val).split('T')[0];
}

function mapRowToReport(row: IncidentReportRow): IncidentReport {
  return {
    id: row.id,
    userId: row.user_id,
    category: row.category as IncidentCategory,
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    incidentDate: formatDateOnly(row.incident_date),
    description: row.description || undefined,
    status: row.status as IncidentStatus,
    moderatorNotes: row.moderator_notes || undefined,
    reviewedBy: row.reviewed_by || undefined,
    reviewedAt: row.reviewed_at ? new Date(row.reviewed_at).toISOString() : undefined,
    createdAt: new Date(row.created_at).toISOString(),
    updatedAt: new Date(row.updated_at).toISOString()
  };
}

export class IncidentReportRepository {
  async create(report: IncidentReport): Promise<IncidentReport> {
    const text = `
      INSERT INTO incident_reports (
        id, user_id, category, latitude, longitude, incident_date,
        description, status, moderator_notes, reviewed_by, reviewed_at,
        created_at, updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
      RETURNING *;
    `;
    const params = [
      report.id,
      report.userId,
      report.category,
      report.latitude,
      report.longitude,
      report.incidentDate,
      report.description || null,
      report.status,
      report.moderatorNotes || null,
      report.reviewedBy || null,
      report.reviewedAt || null,
      report.createdAt || new Date().toISOString(),
      report.updatedAt || new Date().toISOString()
    ];
    const res = await query<IncidentReportRow>(text, params);
    return mapRowToReport(res.rows[0]);
  }

  async findById(id: string): Promise<IncidentReport | null> {
    const text = `
      SELECT * FROM incident_reports
      WHERE id = $1
      LIMIT 1;
    `;
    const res = await query<IncidentReportRow>(text, [id]);
    return res.rows[0] ? mapRowToReport(res.rows[0]) : null;
  }

  async findByUserId(userId: string): Promise<IncidentReport[]> {
    const text = `
      SELECT * FROM incident_reports
      WHERE user_id = $1
      ORDER BY created_at DESC;
    `;
    const res = await query<IncidentReportRow>(text, [userId]);
    return res.rows.map(mapRowToReport);
  }

  /**
   * Finds potential duplicate reports in the same category within [radiusMeters]
   * and within 1 calendar day of [date].
   */
  async findPotentialDuplicates(
    category: IncidentCategory,
    lat: number,
    lng: number,
    date: string,
    radiusMeters: number = 500
  ): Promise<IncidentReport[]> {
    const text = `
      SELECT *,
        (6371000 * acos(
          LEAST(1.0, GREATEST(-1.0,
            cos(radians($2)) * cos(radians(latitude)) * cos(radians(longitude) - radians($3)) +
            sin(radians($2)) * sin(radians(latitude))
          ))
        )) AS distance_meters
      FROM incident_reports
      WHERE category = $1
        AND incident_date BETWEEN ($4::date - INTERVAL '1 day') AND ($4::date + INTERVAL '1 day')
        AND status != 'REJECTED'
        AND (
          6371000 * acos(
            LEAST(1.0, GREATEST(-1.0,
              cos(radians($2)) * cos(radians(latitude)) * cos(radians(longitude) - radians($3)) +
              sin(radians($2)) * sin(radians(latitude))
            ))
          )
        ) <= $5
      ORDER BY created_at DESC
      LIMIT 5;
    `;
    const res = await query<IncidentReportRow>(text, [category, lat, lng, date, radiusMeters]);
    return res.rows.map(mapRowToReport);
  }

  async updateStatus(
    id: string,
    status: IncidentStatus,
    moderatorNotes?: string,
    reviewedBy?: string
  ): Promise<IncidentReport | null> {
    const text = `
      UPDATE incident_reports
      SET status = $2,
          moderator_notes = COALESCE($3, moderator_notes),
          reviewed_by = COALESCE($4, reviewed_by),
          reviewed_at = NOW(),
          updated_at = NOW()
      WHERE id = $1
      RETURNING *;
    `;
    const res = await query<IncidentReportRow>(text, [id, status, moderatorNotes || null, reviewedBy || null]);
    return res.rows[0] ? mapRowToReport(res.rows[0]) : null;
  }
}

export const incidentReportRepository = new IncidentReportRepository();
