export type RiskLevel = 'SAFE' | 'MODERATE_RISK' | 'UNSAFE';

export interface LatLngPoint {
  latitude: number;
  longitude: number;
}

export interface SafetyZone {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  radius: number; // Radius in meters
  riskLevel: RiskLevel;
  description: string;
  safetyScore?: number; // 0 - 100
  advisory?: string;
  activePatrols?: number;
  cctvCoverage?: number; // percentage
  polygonPoints?: LatLngPoint[]; // Optional boundary for complex corridors
  createdAt: string;
  updatedAt: string;
}

export interface CreateSafetyZoneDto {
  name: string;
  latitude: number;
  longitude: number;
  radius: number;
  riskLevel: RiskLevel;
  description: string;
  safetyScore?: number;
  advisory?: string;
  activePatrols?: number;
  cctvCoverage?: number;
  polygonPoints?: LatLngPoint[];
}

export interface UpdateSafetyZoneDto {
  name?: string;
  latitude?: number;
  longitude?: number;
  radius?: number;
  riskLevel?: RiskLevel;
  description?: string;
  safetyScore?: number;
  advisory?: string;
  activePatrols?: number;
  cctvCoverage?: number;
  polygonPoints?: LatLngPoint[];
}

export interface SafetyZoneQueryParams {
  riskLevel?: RiskLevel;
  lat?: number;
  lng?: number;
  radius?: number; // Proximity radius filter in meters
}
