import '../config/env';
import { Pool, PoolClient, QueryResult, QueryResultRow } from 'pg';
import { logger } from '../utils/logger';

const connectionString = process.env.DATABASE_URL;
const hasPassword = Boolean(process.env.PG_PASSWORD && process.env.PG_PASSWORD.trim().length > 0);
export const isConfigured = Boolean(connectionString || hasPassword);

const isProduction = process.env.NODE_ENV === 'production';
const ssl = isProduction || (connectionString && (connectionString.includes('sslmode=require') || connectionString.includes('render.com') || connectionString.includes('neon.tech') || connectionString.includes('supabase.co')))
  ? { rejectUnauthorized: false }
  : undefined;

const poolConfig = connectionString
  ? {
      connectionString,
      ssl,
      max: parseInt(process.env.PG_MAX_POOL || '20', 10),
      idleTimeoutMillis: 10000,
      connectionTimeoutMillis: 5000
    }
  : {
      host: process.env.PG_HOST || 'localhost',
      port: parseInt(process.env.PG_PORT || '5432', 10),
      database: process.env.PG_DATABASE || 'samraksha_db',
      user: process.env.PG_USER || 'postgres',
      password: process.env.PG_PASSWORD || '',
      max: parseInt(process.env.PG_MAX_POOL || '20', 10),
      idleTimeoutMillis: 10000,
      connectionTimeoutMillis: 2000
    };

export const pool = new Pool(poolConfig);

// Catch unexpected errors on idle database clients
pool.on('error', (err: Error) => {
  logger.error('Unexpected error on idle PostgreSQL client:', err.message);
});

/**
 * Execute a parameterized SQL query with type-safety and query logging.
 */
export async function query<R extends QueryResultRow = any, I extends any[] = any[]>(
  text: string,
  params?: I
): Promise<QueryResult<R>> {
  if (!isConfigured) {
    throw new Error('PostgreSQL credentials not configured (set PG_PASSWORD in backend/.env)');
  }

  const start = Date.now();
  try {
    const res = await pool.query<R>(text, params);
    const duration = Date.now() - start;
    if (process.env.NODE_ENV === 'development' && duration > 100) {
      logger.debug(`Slow SQL query executed in ${duration}ms: ${text.slice(0, 100)}...`);
    }
    return res;
  } catch (error: any) {
    logger.error(`Database query failed [${text.slice(0, 80)}...]: ${error.message}`);
    throw error;
  }
}

/**
 * Acquire a dedicated client from the pool (e.g. for multi-statement transactions).
 */
export async function getClient(): Promise<PoolClient> {
  if (!isConfigured) {
    throw new Error('PostgreSQL credentials not configured (set PG_PASSWORD in backend/.env)');
  }
  return await pool.connect();
}

/**
 * Executes a callback within a managed transaction.
 * Automatically handles BEGIN, COMMIT, and ROLLBACK.
 */
export async function withTransaction<T>(
  callback: (client: PoolClient) => Promise<T>
): Promise<T> {
  const client = await getClient();
  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

/**
 * Tests database connectivity. Returns true if healthy, false otherwise.
 */
export async function isDbConnected(): Promise<boolean> {
  if (!isConfigured) return false;
  try {
    const res = await pool.query('SELECT 1 AS healthy');
    return res.rows[0]?.healthy === 1;
  } catch {
    return false;
  }
}

/**
 * Safely tests database connection on startup and returns status with safe metadata.
 * Never prints or leaks the password or secrets.
 */
export async function testDbConnection(): Promise<{
  connected: boolean;
  database?: string;
  host?: string;
  port?: number;
  user?: string;
  error?: string;
}> {
  if (!isConfigured) {
    return {
      connected: false,
      error: 'PostgreSQL credentials not configured (set PG_PASSWORD in backend/.env)'
    };
  }

  try {
    const res = await pool.query('SELECT 1 AS healthy, current_database() AS db_name, current_user AS db_user');
    const healthy = res.rows[0]?.healthy === 1;
    return {
      connected: healthy,
      database: res.rows[0]?.db_name || process.env.PG_DATABASE || 'samraksha_db',
      host: process.env.PG_HOST || 'localhost',
      port: parseInt(process.env.PG_PORT || '5432', 10),
      user: res.rows[0]?.db_user || process.env.PG_USER || 'postgres'
    };
  } catch (err: any) {
    return {
      connected: false,
      error: err.message || 'Connection failed'
    };
  }
}

/**
 * Gracefully close all pool connections on application shutdown.
 */
export async function closeDb(): Promise<void> {
  try {
    await pool.end();
  } catch (err: any) {
    logger.error('Error closing PostgreSQL pool:', err.message);
  }
}

export default {
  pool,
  query,
  getClient,
  withTransaction,
  isDbConnected,
  testDbConnection,
  closeDb,
  isConfigured
};
