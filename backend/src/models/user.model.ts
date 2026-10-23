export interface User {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  passwordHash: string;
  medicalInfo?: {
    bloodGroup?: string;
    allergies?: string;
    isOrganDonor?: boolean;
  };
  createdAt: Date;
  updatedAt: Date;
}

export type UserProfile = Omit<User, 'passwordHash'>;

export interface SignupDto {
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export interface LoginDto {
  identifier: string; // email or phone
  password: string;
}

export interface AuthResponse {
  user: UserProfile;
  token: string;
  message: string;
}
