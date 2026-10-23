import { Router } from 'express';
import { safetyZoneController } from '../controllers/safety-zone.controller';

const router = Router();

// GET /api/safety-zones - List all zones (with optional query filters)
router.get('/', (req, res) => safetyZoneController.getZones(req, res));

// POST /api/safety-zones/sync - Batch sync safety zones from client
router.post('/sync', (req, res) => safetyZoneController.syncZones(req, res));

// GET /api/safety-zones/:id - Get zone by ID
router.get('/:id', (req, res) => safetyZoneController.getZoneById(req, res));

// POST /api/safety-zones - Create new safety zone
router.post('/', (req, res) => safetyZoneController.createZone(req, res));

// PUT /api/safety-zones/:id - Update existing safety zone
router.put('/:id', (req, res) => safetyZoneController.updateZone(req, res));

// DELETE /api/safety-zones/:id - Delete safety zone
router.delete('/:id', (req, res) => safetyZoneController.deleteZone(req, res));

export default router;
