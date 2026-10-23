export type JourneyStatus = 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export interface LatLngCoordinate {
  latitude: number;
  longitude: number;
}

export interface Journey {
  id: string;
  userId: string;
  startLocationName: string;
  endLocationName: string;
  startTime: string;        // ISO timestamp
  expectedEndTime?: string; // ISO timestamp
  completedTime?: string;   // ISO timestamp
  status: JourneyStatus;
  startLatitude: number;
  startLongitude: number;
  currentLatitude: number;
  currentLongitude: number;
  destinationLatitude: number;
  destinationLongitude: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateJourneyDto {
  startLocationName: string;
  endLocationName: string;
  expectedEndTime?: string;
  startLatitude: number;
  startLongitude: number;
  destinationLatitude?: number;
  destinationLongitude?: number;
}

export interface UpdateJourneyLocationDto {
  currentLatitude: number;
  currentLongitude: number;
}

export interface EndJourneyDto {
  status: 'COMPLETED' | 'CANCELLED';
}

export interface JourneyQueryParams {
  status?: JourneyStatus;
  userId?: string;
  limit?: number;
}
