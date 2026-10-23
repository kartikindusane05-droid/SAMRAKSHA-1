import './config/env';
import app from './app';
import { logger } from './utils/logger';
import { testDbConnection, closeDb } from './db';

const PORT = parseInt(process.env.PORT || '5000', 10);
const HOST = process.env.HOST || '0.0.0.0';

const server = app.listen(PORT, HOST, async () => {
  logger.info(`══════════════════════════════════════════════════`);
  logger.info(`🛡️  SAMRAKSHA Safety Backend Server Started`);
  logger.info(`📡 Port: http://localhost:${PORT}`);
  logger.info(`🌐 Local Network: http://${HOST}:${PORT}`);
  logger.info(`🩺 Health Check: http://localhost:${PORT}/api/health`);
  logger.info(`🔐 Auth Endpoints: http://localhost:${PORT}/api/auth`);
  logger.info(`🗺️  Safety Zones: http://localhost:${PORT}/api/safety-zones`);
  logger.info(`🚶 Journey Safety: http://localhost:${PORT}/api/journeys`);
  logger.info(`🌱 Environment: ${process.env.NODE_ENV || 'development'}`);

  // Safe startup database connectivity check (never print password or secrets)
  const dbStatus = await testDbConnection();
  if (dbStatus.connected) {
    logger.info(`🐘 PostgreSQL Connected: ${dbStatus.user}@${dbStatus.host}:${dbStatus.port}/${dbStatus.database}`);
  } else {
    logger.error(`❌ PostgreSQL Connection Failed: ${dbStatus.error}`);
  }

  logger.info(`══════════════════════════════════════════════════`);
});

// Graceful process shutdown
const handleShutdown = (signal: string) => {
  logger.info(`Received ${signal}. Shutting down SAMRAKSHA server gracefully...`);
  server.close(async () => {
    await closeDb();
    logger.info('Server closed. Process terminating.');
    process.exit(0);
  });
};

process.on('SIGTERM', () => handleShutdown('SIGTERM'));
process.on('SIGINT', () => handleShutdown('SIGINT'));

export default server;
