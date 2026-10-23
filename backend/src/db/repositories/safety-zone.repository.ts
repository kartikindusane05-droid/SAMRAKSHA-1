import { query } from '../index';
import { SafetyZone, RiskLevel, SafetyZoneQueryParams } from '../../models/safety-zone.model';

export interface SafetyZoneRow {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  radius: number;
  risk_level: string;
  description: string;
  safety_score: number;
  advisory: string | null;
  active_patrols: number;
  cctv_coverage: number;
  polygon_points: any;
  created_at: Date;
  updated_at: Date;
}

function mapRowToZone(row: SafetyZoneRow): SafetyZone {
  return {
    id: row.id,
    name: row.name,
    latitude: Number(row.latitude),
    longitude: Number(row.longitude),
    radius: Number(row.radius),
    riskLevel: row.risk_level as RiskLevel,
    description: row.description,
    safetyScore: row.safety_score,
    advisory: row.advisory || undefined,
    activePatrols: row.active_patrols,
    cctvCoverage: row.cctv_coverage,
    polygonPoints: row.polygon_points || undefined,
    createdAt: new Date(row.created_at).toISOString(),
    updatedAt: new Date(row.updated_at).toISOString()
  };
}

// Haversine distance formula in meters
function calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371e3;
  const phi1 = (lat1 * Math.PI) / 180;
  const phi2 = (lat2 * Math.PI) / 180;
  const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c;
}

export class SafetyZoneRepository {
  async findAll(queryParams?: SafetyZoneQueryParams): Promise<SafetyZone[]> {
    let sql = `SELECT * FROM safety_zones`;
    const params: any[] = [];

    if (queryParams?.riskLevel) {
      params.push(queryParams.riskLevel);
      sql += ` WHERE risk_level = $${params.length}`;
    }

    sql += ` ORDER BY safety_score DESC;`;

    const res = await query<SafetyZoneRow>(sql, params);
    let zones = res.rows.map(mapRowToZone);

    if (
      queryParams?.lat !== undefined &&
      queryParams?.lng !== undefined &&
      queryParams?.radius !== undefined
    ) {
      const uLat = Number(queryParams.lat);
      const uLng = Number(queryParams.lng);
      const searchRadius = Number(queryParams.radius);

      zones = zones.filter((zone) => {
        const dist = calculateDistanceMeters(uLat, uLng, zone.latitude, zone.longitude);
        return dist <= zone.radius + searchRadius;
      });
    }

    return zones;
  }

  async findById(id: string): Promise<SafetyZone | null> {
    const text = `SELECT * FROM safety_zones WHERE id = $1 LIMIT 1;`;
    const res = await query<SafetyZoneRow>(text, [id]);
    return res.rows[0] ? mapRowToZone(res.rows[0]) : null;
  }

  async create(zone: SafetyZone): Promise<SafetyZone> {
    const text = `
      INSERT INTO safety_zones (
        id, name, latitude, longitude, radius, risk_level, description,
        safety_score, advisory, active_patrols, cctv_coverage, polygon_points,
        created_at, updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
      ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude,
        radius = EXCLUDED.radius,
        risk_level = EXCLUDED.risk_level,
        description = EXCLUDED.description,
        safety_score = EXCLUDED.safety_score,
        advisory = EXCLUDED.advisory,
        active_patrols = EXCLUDED.active_patrols,
        cctv_coverage = EXCLUDED.cctv_coverage,
        polygon_points = EXCLUDED.polygon_points,
        updated_at = EXCLUDED.updated_at
      RETURNING *;
    `;
    const res = await query<SafetyZoneRow>(text, [
      zone.id,
      zone.name,
      zone.latitude,
      zone.longitude,
      zone.radius,
      zone.riskLevel,
      zone.description,
      zone.safetyScore ?? 100,
      zone.advisory || null,
      zone.activePatrols ?? 0,
      zone.cctvCoverage ?? 0,
      zone.polygonPoints ? JSON.stringify(zone.polygonPoints) : null,
      zone.createdAt || new Date().toISOString(),
      zone.updatedAt || new Date().toISOString()
    ]);
    return mapRowToZone(res.rows[0]);
  }

  async update(id: string, updates: Partial<SafetyZone>): Promise<SafetyZone | null> {
    const existing = await this.findById(id);
    if (!existing) return null;

    const updated: SafetyZone = {
      ...existing,
      ...updates,
      updatedAt: new Date().toISOString()
    };

    return await this.create(updated);
  }

  async delete(id: string): Promise<boolean> {
    const text = `DELETE FROM safety_zones WHERE id = $1;`;
    const res = await query(text, [id]);
    return (res.rowCount ?? 0) > 0;
  }

  async syncBatch(zones: SafetyZone[]): Promise<SafetyZone[]> {
    for (const z of zones) {
      await this.create(z);
    }
    return this.findAll();
  }
}

export const safetyZoneRepository = new SafetyZoneRepository();
