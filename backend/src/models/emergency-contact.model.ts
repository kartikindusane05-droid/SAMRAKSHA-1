export interface EmergencyContact {
  id: string;
  userId: string;
  name: string;
  phoneNumber: string;
  relationship: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateEmergencyContactDto {
  id?: string;
  name: string;
  phoneNumber: string;
  relationship: string;
}

export interface UpdateEmergencyContactDto {
  name?: string;
  phoneNumber?: string;
  relationship?: string;
}

export interface SyncEmergencyContactsDto {
  contacts: EmergencyContact[];
}
