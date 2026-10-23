export type EmergencyEventStatus = 'ACTIVE' | 'CANCELLED' | 'RESOLVED';
export type EmergencyEventType =
  | 'MANUAL_SOS'
  | 'JOURNEY_TIMEOUT'
  | 'GEOFENCE_ALERT'
  | 'VOICE_TRIGGER'
  | 'IMPACT_DETECTION';

export interface EmergencyEvent {
  id: string;
  userId: string;
  timestamp: number; // epoch millis
  latitude: number;
  longitude: number;
  status: EmergencyEventStatus;
  eventType: EmergencyEventType;
  address?: string;
  notes?: string;
  resolvedAt?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmergencyEventDto {
  id?: string;
  timestamp?: number;
  latitude: number;
  longitude: number;
  status?: EmergencyEventStatus;
  eventType?: EmergencyEventType;
  address?: string;
  notes?: string;
  resolvedAt?: number;
}

export interface UpdateEmergencyEventStatusDto {
  status: EmergencyEventStatus;
  resolvedAt?: number;
}

export interface SyncEmergencyEventsDto {
  events: EmergencyEvent[];
}
