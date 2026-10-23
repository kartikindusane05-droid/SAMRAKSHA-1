import { Router } from 'express';
import { authController } from '../controllers/auth.controller';
import { requireAuth } from '../middleware/auth.middleware';

const router = Router();

// Public auth endpoints
router.post('/signup', (req, res, next) => authController.signup(req, res, next));
router.post('/login', (req, res, next) => authController.login(req, res, next));

// Protected / session-aware auth endpoints
router.post('/logout', requireAuth, (req, res, next) => authController.logout(req, res, next));
router.get('/profile', requireAuth, (req, res, next) => authController.getProfile(req, res, next));
router.put('/profile', requireAuth, (req, res, next) => authController.updateProfile(req, res, next));
router.post('/sync', requireAuth, (req, res, next) => authController.syncProfile(req, res, next));

export default router;
