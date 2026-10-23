import { Router } from 'express';
import { requireAuth } from '../middleware/auth.middleware';
import { incidentReportController } from '../controllers/incident-report.controller';

const router = Router();

// Submit an unsafe incident report (requires user authentication)
router.post('/', requireAuth, (req, res) => incidentReportController.create(req, res));

// View the authenticated user's submitted incident reports
router.get('/my-reports', requireAuth, (req, res) => incidentReportController.getMyReports(req, res));

// View specific incident report details
router.get('/:id', requireAuth, (req, res) => incidentReportController.getById(req, res));

// Moderator status update endpoint (guarded by X-Admin-Key)
router.patch('/:id/status', (req, res) => incidentReportController.updateStatus(req, res));

export default router;
