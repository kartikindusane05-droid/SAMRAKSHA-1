import { Router } from 'express';
import { requireAuth } from '../middleware/auth.middleware';
import { emergencyContactController } from '../controllers/emergency-contact.controller';

const router = Router();

// All contact routes require authentication
router.use(requireAuth);

// GET    /api/contacts          – Get all emergency contacts
router.get('/', (req, res) => emergencyContactController.getContacts(req, res));

// GET    /api/contacts/:id      – Get single emergency contact
router.get('/:id', (req, res) => emergencyContactController.getContactById(req, res));

// POST   /api/contacts          – Add an emergency contact
router.post('/', (req, res) => emergencyContactController.addContact(req, res));

// PUT    /api/contacts/:id      – Update emergency contact
router.put('/:id', (req, res) => emergencyContactController.updateContact(req, res));

// DELETE /api/contacts/:id      – Delete emergency contact
router.delete('/:id', (req, res) => emergencyContactController.deleteContact(req, res));

// POST   /api/contacts/sync     – Batch sync contacts
router.post('/sync', (req, res) => emergencyContactController.syncContacts(req, res));

export default router;
