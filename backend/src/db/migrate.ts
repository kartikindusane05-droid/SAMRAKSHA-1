import '../config/env';
import path from 'path';
import fs from 'fs';
import { Client } from 'pg';
import { logger } from '../utils/logger';

const connectionString = process.env.DATABASE_URL;
const isProduction = process.env.NODE_ENV === 'production';
const ssl = isProduction || (connectionString && (connectionString.includes('sslmode=require') || connectionString.includes('render.com') || connectionString.includes('neon.tech') || connectionString.includes('supabase.co')))
  ? { rejectUnauthorized: false }
  : undefined;

const dbConfig = {
  host: process.env.PG_HOST || 'localhost',
  port: parseInt(process.env.PG_PORT || '5432', 10),
  database: process.env.PG_DATABASE || 'samraksha_db',
  user: process.env.PG_USER || 'postgres',
  password: process.env.PG_PASSWORD || ''
};

/**
 * Ensures the target database exists on the PostgreSQL server.
 * Skips check if a managed cloud DATABASE_URL is provided.
 */
async function ensureDatabaseExists(): Promise<void> {
  if (connectionString) {
    logger.info('Using cloud-managed DATABASE_URL. Database provisioning handled by hosting provider.');
    return;
  }

  const adminClient = new Client({
    ...dbConfig,
    database: 'postgres'
  });

  try {
    await adminClient.connect();
    const res = await adminClient.query(
      `SELECT 1 FROM pg_database WHERE datname = $1`,
      [dbConfig.database]
    );

    if (res.rowCount === 0) {
      logger.info(`Database "${dbConfig.database}" does not exist. Creating...`);
      // Escape database name safely
      await adminClient.query(`CREATE DATABASE "${dbConfig.database}"`);
      logger.info(`Database "${dbConfig.database}" created successfully.`);
    } else {
      logger.info(`Database "${dbConfig.database}" already exists.`);
    }
  } catch (error: any) {
    logger.warn(`Could not verify/create database via admin client: ${error.message}`);
  } finally {
    await adminClient.end().catch(() => {});
  }
}

/**
 * Runs all unapplied database migrations in sequence.
 */
export async function runMigrations(): Promise<void> {
  await ensureDatabaseExists();

  const client = connectionString
    ? new Client({ connectionString, ssl })
    : new Client(dbConfig);

  try {
    await client.connect();
    logger.info(`Connected to PostgreSQL database${connectionString ? ' (via DATABASE_URL)' : ': ' + dbConfig.database}`);

    // Create migrations table if not exists
    await client.query(`
      CREATE TABLE IF NOT EXISTS migrations (
        id SERIAL PRIMARY KEY,
        name VARCHAR(255) NOT NULL UNIQUE,
        applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      );
    `);

    // Fetch previously applied migrations
    const appliedRes = await client.query('SELECT name FROM migrations ORDER BY id ASC');
    const appliedNames = new Set(appliedRes.rows.map((r: { name: string }) => r.name));

    // Find migrations directory in dist or src
    const candidateDirs = [
      path.resolve(__dirname, 'migrations'),
      path.resolve(__dirname, '../../src/db/migrations'),
      path.resolve(__dirname, '../src/db/migrations'),
      path.resolve(process.cwd(), 'src/db/migrations'),
      path.resolve(process.cwd(), 'dist/db/migrations'),
      path.resolve(process.cwd(), 'backend/src/db/migrations'),
      path.resolve(process.cwd(), 'backend/dist/db/migrations')
    ];

    const migrationsDir = candidateDirs.find((dir) => fs.existsSync(dir));
    if (!migrationsDir) {
      logger.warn(`Migrations directory not found in candidate paths: ${candidateDirs.join(', ')}`);
      return;
    }
    logger.info(`Loading migrations from: ${migrationsDir}`);

    const files = fs
      .readdirSync(migrationsDir)
      .filter((file) => file.endsWith('.sql'))
      .sort();

    let appliedCount = 0;

    for (const file of files) {
      if (appliedNames.has(file)) {
        logger.info(`Migration already applied: ${file}`);
        continue;
      }

      logger.info(`Applying migration: ${file}...`);
      const filePath = path.join(migrationsDir, file);
      const sql = fs.readFileSync(filePath, 'utf-8');

      try {
        await client.query('BEGIN');
        await client.query(sql);
        await client.query('INSERT INTO migrations (name) VALUES ($1)', [file]);
        await client.query('COMMIT');
        appliedCount++;
        logger.info(`Successfully applied migration: ${file}`);
      } catch (migrationError: any) {
        await client.query('ROLLBACK');
        logger.error(`Failed to apply migration ${file}: ${migrationError.message}`);
        throw migrationError;
      }
    }

    logger.info(
      appliedCount > 0
        ? `Migration completed: ${appliedCount} new migrations applied.`
        : `Database is already up to date. No pending migrations.`
    );
  } finally {
    await client.end().catch(() => {});
  }
}

// Allow direct execution: npx tsx src/db/migrate.ts
if (require.main === module) {
  runMigrations()
    .then(() => {
      logger.info('Migration process finished.');
      process.exit(0);
    })
    .catch((err) => {
      logger.error('Migration failed:', err);
      process.exit(1);
    });
}
