import crypto from 'crypto';
import { User, UserProfile, SignupDto, LoginDto } from '../models/user.model';
import { userRepository } from '../db/repositories/user.repository';
import { logger } from '../utils/logger';

// Scrypt parameters — slow to resist brute-force attacks
const SCRYPT_N = 16384;
const SCRYPT_R = 8;
const SCRYPT_P = 1;
const KEY_LEN = 64;
const SALT_LEN = 32;

/**
 * Service handling authentication and session management.
 * Persists users and sessions in PostgreSQL via typed userRepository.
 * In-memory fallback is disabled: all persistence operations strictly target PostgreSQL.
 */
class AuthService {
  // In-memory session cache for low-latency token verification
  private sessionCache: Map<string, { userId: string; expiresAt?: number }> = new Map();

  /**
   * Derives a cryptographically strong salted scrypt hash.
   * Format: "$scrypt$<saltHex>$<hashHex>"
   */
  public hashPassword(password: string): string {
    const salt = crypto.randomBytes(SALT_LEN);
    const hash = crypto.scryptSync(password, salt, KEY_LEN, {
      N: SCRYPT_N,
      r: SCRYPT_R,
      p: SCRYPT_P
    });
    return `$scrypt$${salt.toString('hex')}$${hash.toString('hex')}`;
  }

  /**
   * Verifies plaintext password against a stored scrypt hash in constant time.
   */
  public verifyPassword(password: string, stored: string): boolean {
    try {
      if (stored.startsWith('$scrypt$')) {
        const parts = stored.split('$');
        const salt = Buffer.from(parts[2], 'hex');
        const expectedHash = Buffer.from(parts[3], 'hex');
        const derivedHash = crypto.scryptSync(password, salt, KEY_LEN, {
          N: SCRYPT_N,
          r: SCRYPT_R,
          p: SCRYPT_P
        });
        return crypto.timingSafeEqual(derivedHash, expectedHash);
      }
      return false;
    } catch {
      return false;
    }
  }

  public generateToken(): string {
    return crypto.randomBytes(32).toString('hex');
  }

  public toProfile(user: User): UserProfile {
    const { passwordHash, ...profile } = user;
    return profile;
  }

  /**
   * Registers a new user with PostgreSQL persistence.
   * Fails with clear database error if PostgreSQL is unavailable.
   */
  public async signup(dto: SignupDto): Promise<{ user: UserProfile; token: string }> {
    const normalizedEmail = dto.email.trim().toLowerCase();
    const normalizedPhone = dto.phone.trim();

    // 1. Check uniqueness in PostgreSQL
    const existingEmail = await userRepository.findByEmail(normalizedEmail);
    if (existingEmail) {
      const err = new Error('An account with this email address already exists');
      (err as any).statusCode = 400;
      throw err;
    }

    const existingPhone = await userRepository.findByPhone(normalizedPhone);
    if (existingPhone) {
      const err = new Error('An account with this phone number already exists');
      (err as any).statusCode = 400;
      throw err;
    }

    const newId = crypto.randomUUID();
    const newUser: User = {
      id: newId,
      fullName: dto.fullName.trim(),
      email: normalizedEmail,
      phone: normalizedPhone,
      passwordHash: this.hashPassword(dto.password),
      createdAt: new Date(),
      updatedAt: new Date()
    };

    // 2. Persist user and session to PostgreSQL
    const savedUser = await userRepository.create(newUser);
    const token = this.generateToken();
    await userRepository.createSession(token, savedUser.id);
    this.sessionCache.set(token, { userId: savedUser.id });

    return {
      user: this.toProfile(savedUser),
      token
    };
  }

  /**
   * Authenticates user via email/phone and securely verifies password.
   * Fails with clear database error if PostgreSQL is unavailable.
   */
  public async login(dto: LoginDto): Promise<{ user: UserProfile; token: string }> {
    const identifier = dto.identifier.trim();

    // Query user directly from PostgreSQL
    const matchedUser = await userRepository.findByIdentifier(identifier);

    // Always run verifyPassword against dummy hash if user not found to prevent timing enumeration
    const storedHash = matchedUser?.passwordHash ?? '$scrypt$00$00';
    const passwordValid = this.verifyPassword(dto.password, storedHash);

    if (!matchedUser || !passwordValid) {
      const err = new Error('Invalid email/phone or password');
      (err as any).statusCode = 401;
      throw err;
    }

    const token = this.generateToken();
    // Persist session to PostgreSQL; fail if DB is unavailable
    await userRepository.createSession(token, matchedUser.id);
    this.sessionCache.set(token, { userId: matchedUser.id });

    return {
      user: this.toProfile(matchedUser),
      token
    };
  }

  /**
   * Revokes session token in database and local cache.
   */
  public async logout(token: string): Promise<boolean> {
    this.sessionCache.delete(token);
    try {
      return await userRepository.deleteSession(token);
    } catch {
      return true;
    }
  }

  /**
   * Retrieves user profile by ID from PostgreSQL.
   */
  public async getProfile(userId: string): Promise<UserProfile | null> {
    const user = await userRepository.findById(userId);
    return user ? this.toProfile(user) : null;
  }

  /**
   * Updates user profile fields in PostgreSQL.
   */
  public async updateProfile(
    userId: string,
    updates: Partial<User>
  ): Promise<UserProfile | null> {
    const updated = await userRepository.update(userId, updates);
    return updated ? this.toProfile(updated) : null;
  }

  /**
   * Verifies session token against cache or PostgreSQL sessions table.
   */
  public async verifyToken(token: string): Promise<string | null> {
    // 1. Fast path from session cache
    const cached = this.sessionCache.get(token);
    if (cached) {
      if (!cached.expiresAt || cached.expiresAt > Date.now()) {
        return cached.userId;
      }
      this.sessionCache.delete(token);
      return null;
    }

    // 2. Query PostgreSQL sessions table
    try {
      const userId = await userRepository.findUserIdBySession(token);
      if (userId) {
        this.sessionCache.set(token, { userId });
        return userId;
      }
    } catch (err: any) {
      logger.error(`Session DB query error: ${err.message}`);
      throw err;
    }

    return null;
  }
}

export const authService = new AuthService();
