import '../config/env';

import { runMigrations } from './migrate';
import { isDbConnected, closeDb } from './index';
import { userRepository } from './repositories/user.repository';
import { emergencyContactRepository } from './repositories/emergency-contact.repository';
import { emergencyEventRepository } from './repositories/emergency-event.repository';
import { journeyRepository } from './repositories/journey.repository';
import { safetyZoneRepository } from './repositories/safety-zone.repository';
import { productRepository } from './repositories/product.repository';
import { authService } from '../services/auth.service';
import { logger } from '../utils/logger';

async function verifyPostgreSql() {
  logger.info('══════════════════════════════════════════════════════════');
  logger.info('🛡️  SAMRAKSHA PostgreSQL Persistence Verification Suite');
  logger.info('══════════════════════════════════════════════════════════');

  const connected = await isDbConnected();
  if (!connected) {
    logger.warn('⚠️  PostgreSQL is not reachable with current credentials.');
    logger.warn('   To run live database verification:');
    logger.warn('   1. Open backend/.env');
    logger.warn('   2. Set PG_PASSWORD=<your_postgres_password>');
    logger.warn('   3. Run: npm run db:verify');
    logger.info('══════════════════════════════════════════════════════════');
    process.exit(0);
  }

  logger.info('✅ 1. Database Connection: Connected to PostgreSQL server.');

  // Run migrations
  logger.info('📦 2. Running Schema Migrations...');
  await runMigrations();
  logger.info('✅ 2. Migrations successfully verified.');

  // Test User Persistence
  logger.info('👤 3. Testing User & Password Scrypt Persistence...');
  const testEmail = `verify_${Date.now()}@samraksha.local`;
  const rawPassword = 'StrongPassword!2026';
  const hashedPassword = authService.hashPassword(rawPassword);

  const testUser = await userRepository.create({
    id: `usr_${Date.now()}`,
    fullName: 'Postgres Verification User',
    email: testEmail,
    phone: `+9190${Date.now().toString().slice(-8)}`,
    passwordHash: hashedPassword,
    medicalInfo: { bloodGroup: 'O+', allergies: 'None' },
    createdAt: new Date(),
    updatedAt: new Date()
  });

  const retrievedUser = await userRepository.findById(testUser.id);
  if (!retrievedUser || retrievedUser.email !== testEmail.toLowerCase()) {
    throw new Error('User was not persisted to PostgreSQL');
  }

  const passwordValid = authService.verifyPassword(rawPassword, retrievedUser.passwordHash);
  if (!passwordValid) {
    throw new Error('Password verification against scrypt hash failed');
  }
  logger.info('✅ 3. User & Scrypt Hash persisted and verified in PostgreSQL.');

  // Test Session Persistence
  logger.info('🔑 4. Testing Session Persistence...');
  const sessionToken = authService.generateToken();
  await userRepository.createSession(sessionToken, testUser.id);
  const foundUserId = await userRepository.findUserIdBySession(sessionToken);
  if (foundUserId !== testUser.id) {
    throw new Error('Session token lookup from PostgreSQL failed');
  }
  logger.info('✅ 4. Session token persisted and verified.');

  // Test Emergency Contacts Persistence & Scoping
  logger.info('📞 5. Testing Emergency Contacts Persistence...');
  const contact = await emergencyContactRepository.create({
    id: `cnt_${Date.now()}`,
    userId: testUser.id,
    name: 'Primary Contact',
    phoneNumber: '+919876543210',
    relationship: 'Father',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  });
  const contactsList = await emergencyContactRepository.findByUserId(testUser.id);
  if (!contactsList.some((c) => c.id === contact.id)) {
    throw new Error('Emergency contact was not persisted');
  }
  // User isolation check
  const crossUserContact = await emergencyContactRepository.findById(contact.id, 'other_user_id');
  if (crossUserContact !== null) {
    throw new Error('Authorization violation: Contact was accessed by another userId');
  }
  logger.info('✅ 5. Emergency Contacts persisted with user authorization scoping.');

  // Test Emergency Events Persistence
  logger.info('🚨 6. Testing Emergency Events Persistence...');
  const event = await emergencyEventRepository.create({
    id: `ev_${Date.now()}`,
    userId: testUser.id,
    timestamp: Date.now(),
    latitude: 12.9716,
    longitude: 77.5946,
    status: 'ACTIVE',
    eventType: 'MANUAL_SOS',
    address: 'Bengaluru Safe Zone',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  });
  const updatedEvent = await emergencyEventRepository.updateStatus(event.id, testUser.id, 'RESOLVED');
  if (!updatedEvent || updatedEvent.status !== 'RESOLVED') {
    throw new Error('Emergency event status update failed in PostgreSQL');
  }
  logger.info('✅ 6. Emergency Event persisted and updated.');

  // Test Journeys Persistence
  logger.info('🚶 7. Testing Journey Persistence...');
  const journey = await journeyRepository.create({
    id: `jrn_${Date.now()}`,
    userId: testUser.id,
    startLocationName: 'Metro Station',
    endLocationName: 'Home',
    startTime: new Date().toISOString(),
    status: 'IN_PROGRESS',
    startLatitude: 12.9716,
    startLongitude: 77.5946,
    currentLatitude: 12.9716,
    currentLongitude: 77.5946,
    destinationLatitude: 12.9352,
    destinationLongitude: 77.6245,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  });
  const activeJourney = await journeyRepository.findActive(testUser.id);
  if (!activeJourney || activeJourney.id !== journey.id) {
    throw new Error('Active journey query from PostgreSQL failed');
  }
  await journeyRepository.endJourney(journey.id, testUser.id, 'COMPLETED');
  logger.info('✅ 7. Journey lifecycle persisted in PostgreSQL.');

  // Test Safety Zones & Products
  logger.info('🛍️ 8. Testing Catalog & Safety Zones from PostgreSQL...');
  const zones = await safetyZoneRepository.findAll();
  const products = await productRepository.findAll({ page: 1, limit: 10 });
  logger.info(`   Found ${zones.length} safety zones and ${products.total} products in PostgreSQL.`);
  if (zones.length === 0 || products.total === 0) {
    throw new Error('Standard seeds not found in safety_zones or products tables');
  }
  logger.info('✅ 8. Safety zones and Safety Store products verified.');

  // Cleanup test user (cascades sessions, contacts, events, journeys)
  logger.info('🧹 9. Cleaning up test verification records...');
  const { query } = require('./index');
  await query('DELETE FROM users WHERE id = $1', [testUser.id]);
  logger.info('✅ 9. Verification records cleaned up.');

  logger.info('══════════════════════════════════════════════════════════');
  logger.info('🎉 All PostgreSQL Persistence & Scoping Tests PASSED!');
  logger.info('══════════════════════════════════════════════════════════');

  await closeDb();
  process.exit(0);
}

verifyPostgreSql().catch(async (err) => {
  logger.error('PostgreSQL verification failed:', err);
  await closeDb();
  process.exit(1);
});
