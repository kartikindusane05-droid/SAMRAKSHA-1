import { query } from '../index';
import { User } from '../../models/user.model';

export interface UserRow {
  id: string;
  full_name: string;
  email: string;
  phone: string;
  password_hash: string;
  medical_info: any;
  created_at: Date;
  updated_at: Date;
}

function mapRowToUser(row: UserRow): User {
  return {
    id: row.id,
    fullName: row.full_name,
    email: row.email,
    phone: row.phone,
    passwordHash: row.password_hash,
    medicalInfo: row.medical_info || {},
    createdAt: new Date(row.created_at),
    updatedAt: new Date(row.updated_at)
  };
}

export class UserRepository {
  async create(user: User): Promise<User> {
    const text = `
      INSERT INTO users (id, full_name, email, phone, password_hash, medical_info, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      RETURNING *;
    `;
    const params = [
      user.id,
      user.fullName,
      user.email.toLowerCase(),
      user.phone,
      user.passwordHash,
      JSON.stringify(user.medicalInfo || {}),
      user.createdAt,
      user.updatedAt
    ];
    const res = await query<UserRow>(text, params);
    return mapRowToUser(res.rows[0]);
  }

  async findById(id: string): Promise<User | null> {
    const text = `SELECT * FROM users WHERE id = $1 LIMIT 1;`;
    const res = await query<UserRow>(text, [id]);
    return res.rows[0] ? mapRowToUser(res.rows[0]) : null;
  }

  async findByEmail(email: string): Promise<User | null> {
    const text = `SELECT * FROM users WHERE LOWER(email) = LOWER($1) LIMIT 1;`;
    const res = await query<UserRow>(text, [email.trim()]);
    return res.rows[0] ? mapRowToUser(res.rows[0]) : null;
  }

  async findByPhone(phone: string): Promise<User | null> {
    const text = `SELECT * FROM users WHERE phone = $1 LIMIT 1;`;
    const res = await query<UserRow>(text, [phone.trim()]);
    return res.rows[0] ? mapRowToUser(res.rows[0]) : null;
  }

  async findByIdentifier(identifier: string): Promise<User | null> {
    const trimmed = identifier.trim();
    const text = `
      SELECT * FROM users 
      WHERE LOWER(email) = LOWER($1) OR phone = $1 
      LIMIT 1;
    `;
    const res = await query<UserRow>(text, [trimmed]);
    return res.rows[0] ? mapRowToUser(res.rows[0]) : null;
  }

  async update(id: string, updates: Partial<User>): Promise<User | null> {
    const existing = await this.findById(id);
    if (!existing) return null;

    const updatedFullName = updates.fullName ?? existing.fullName;
    const updatedPhone = updates.phone ?? existing.phone;
    const updatedMedical = {
      ...(existing.medicalInfo || {}),
      ...(updates.medicalInfo || {})
    };

    const text = `
      UPDATE users
      SET full_name = $1, phone = $2, medical_info = $3, updated_at = NOW()
      WHERE id = $4
      RETURNING *;
    `;
    const res = await query<UserRow>(text, [
      updatedFullName,
      updatedPhone,
      JSON.stringify(updatedMedical),
      id
    ]);
    return res.rows[0] ? mapRowToUser(res.rows[0]) : null;
  }

  // ── Session Management ────────────────────────────────────
  async createSession(token: string, userId: string, expiresAt?: Date): Promise<void> {
    const text = `
      INSERT INTO sessions (token, user_id, created_at, expires_at)
      VALUES ($1, $2, NOW(), $3)
      ON CONFLICT (token) DO UPDATE SET user_id = $2, created_at = NOW(), expires_at = $3;
    `;
    await query(text, [token, userId, expiresAt || null]);
  }

  async findUserIdBySession(token: string): Promise<string | null> {
    const text = `
      SELECT user_id FROM sessions 
      WHERE token = $1 AND (expires_at IS NULL OR expires_at > NOW())
      LIMIT 1;
    `;
    const res = await query<{ user_id: string }>(text, [token]);
    return res.rows[0]?.user_id || null;
  }

  async deleteSession(token: string): Promise<boolean> {
    const text = `DELETE FROM sessions WHERE token = $1;`;
    const res = await query(text, [token]);
    return (res.rowCount ?? 0) > 0;
  }
}

export const userRepository = new UserRepository();
