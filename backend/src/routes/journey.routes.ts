import { Router } from 'express';
import { requireAuth } from '../middleware/auth.middleware';
import {
  getAllJourneys,
  getActiveJourney,
  getJourneyById,
  startJourney,
  updateJourneyLocation,
  endJourney,
  deleteJourney,
  syncJourneys
} from '../controllers/journey.controller';

const router = Router();

// All journey routes require authentication
router.use(requireAuth);

// GET  /api/journeys          – List all journeys (optional: ?status=IN_PROGRESS&limit=10)
router.get('/', getAllJourneys);

// GET  /api/journeys/active   – Get current active journey for authenticated user
router.get('/active', getActiveJourney);

// POST /api/journeys/sync     – Batch sync journeys from offline tracking client
router.post('/sync', syncJourneys);

// GET  /api/journeys/:id      – Get a specific journey by ID
router.get('/:id', getJourneyById);

// POST /api/journeys          – Start a new journey
router.post('/', startJourney);

// PATCH /api/journeys/:id/location – Update current GPS coordinates during active journey
router.patch('/:id/location', updateJourneyLocation);

// PATCH /api/journeys/:id/end     – End (complete or cancel) a journey
router.patch('/:id/end', endJourney);

// DELETE /api/journeys/:id    – Delete a journey record
router.delete('/:id', deleteJourney);

export default router;
