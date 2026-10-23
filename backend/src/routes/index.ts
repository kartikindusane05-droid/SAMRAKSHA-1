import { Router } from 'express';
import authRoutes from './auth.routes';
import safetyZoneRoutes from './safety-zone.routes';
import journeyRoutes from './journey.routes';
import emergencyContactRoutes from './emergency-contact.routes';
import emergencyEventRoutes from './emergency-event.routes';
import productRoutes from './product.routes';
import incidentReportRoutes from './incident-report.routes';
import { sendSuccess } from '../utils/response';

const router = Router();

// Health check endpoint
router.get('/health', (req, res) => {
  sendSuccess(res, 'SAMRAKSHA Safety Backend API is operational', {
    status: 'ONLINE',
    service: 'samraksha-backend',
    version: '1.0.0',
    uptimeSeconds: Math.floor(process.uptime()),
    timestamp: new Date().toISOString(),
    endpoints: {
      auth: '/api/auth',
      safetyZones: '/api/safety-zones',
      journeys: '/api/journeys',
      contacts: '/api/contacts',
      emergencyEvents: '/api/emergency-events',
      products: '/api/products',
      incidents: '/api/incidents'
    }
  });
});

// Mount module routes
router.use('/auth', authRoutes);
router.use('/safety-zones', safetyZoneRoutes);
router.use('/journeys', journeyRoutes);
router.use('/contacts', emergencyContactRoutes);
router.use('/emergency-events', emergencyEventRoutes);
router.use('/products', productRoutes);
router.use('/incidents', incidentReportRoutes);

export default router;
