export type IncidentCategory =
  | 'HARASSMENT'
  | 'THEFT'
  | 'SUSPICIOUS_ACTIVITY'
  | 'PHYSICAL_SAFETY'
  | 'OTHER';

export type IncidentStatus = 'PENDING_REVIEW' | 'VERIFIED' | 'REJECTED';

export interface IncidentReport {
  id: string;
  userId: string;
  category: IncidentCategory;
  latitude: number;
  longitude: number;
  incidentDate: string; // YYYY-MM-DD
  description?: string;
  status: IncidentStatus;
  moderatorNotes?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIncidentReportDto {
  category: IncidentCategory;
  latitude: number;
  longitude: number;
  incidentDate: string; // YYYY-MM-DD
  description?: string;
  confirmDuplicate?: boolean;
}

export interface UpdateIncidentStatusDto {
  status: IncidentStatus;
  moderatorNotes?: string;
}

export interface SanitizedIncidentReport {
  id: string;
  category: IncidentCategory;
  latitude: number;
  longitude: number;
  incidentDate: string;
  description?: string;
  status: IncidentStatus;
  createdAt: string;
}
