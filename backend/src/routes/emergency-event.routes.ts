import { Router } from 'express';
import { requireAuth } from '../middleware/auth.middleware';
import { emergencyEventController } from '../controllers/emergency-event.controller';

const router = Router();

// All emergency event routes require authentication
router.use(requireAuth);

// GET    /api/emergency-events          – Get all events
router.get('/', (req, res) => emergencyEventController.getEvents(req, res));

// GET    /api/emergency-events/:id      – Get single event
router.get('/:id', (req, res) => emergencyEventController.getEventById(req, res));

// POST   /api/emergency-events          – Create new emergency event
router.post('/', (req, res) => emergencyEventController.createEvent(req, res));

// PATCH  /api/emergency-events/:id/status – Update event status (RESOLVED/CANCELLED)
router.patch('/:id/status', (req, res) => emergencyEventController.updateStatus(req, res));

// POST   /api/emergency-events/sync     – Batch upload/sync events from client
router.post('/sync', (req, res) => emergencyEventController.syncEvents(req, res));

export default router;
