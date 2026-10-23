import './config/env';
import express, { Application, Request, Response, NextFunction } from 'express';
import cors from 'cors';
import routes from './routes';
import { notFoundHandler, errorHandler } from './middleware/error.middleware';
import { logger } from './utils/logger';

const app: Application = express();

// ── CORS — Allow mobile app clients (no Origin header) and configured web origins ──
const allowedOrigin = process.env.CLIENT_ORIGIN;
app.use(cors({
  origin: (origin, callback) => {
    // Mobile apps (Retrofit / OkHttp), curl, and server-to-server requests don't send an Origin header
    if (!origin) return callback(null, true);
    if (!allowedOrigin || allowedOrigin === '*') {
      return callback(null, true);
    }
    const origins = allowedOrigin.split(',').map((s) => s.trim());
    if (origins.includes(origin)) {
      return callback(null, true);
    }
    return callback(new Error('Blocked by CORS'));
  },
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Admin-Key']
}));

// ── Body Parsers with size limits to prevent DoS ──────────────────────────
app.use(express.json({ limit: '100kb' }));
app.use(express.urlencoded({ extended: true, limit: '100kb' }));

// ── Request Logging (path only; no query strings to avoid leaking PII) ────
app.use((req: Request, res: Response, next: NextFunction) => {
  logger.info(`${req.method} ${req.path}`);
  next();
});

// ── Root & Health Endpoints ────────────────────────────────────────────────
app.get('/health', (req: Request, res: Response) => {
  res.status(200).json({
    status: 'UP',
    service: 'samraksha-backend',
    uptimeSeconds: Math.floor(process.uptime()),
    timestamp: new Date().toISOString()
  });
});

app.get('/', (req: Request, res: Response) => {
  res.json({
    name: 'SAMRAKSHA API',
    description: 'Personal Safety & Emergency Response Platform API',
    version: '1.0.0',
    documentation: '/api/health',
    health: '/health'
  });
});

// ── API Routes ─────────────────────────────────────────────────────────────
app.use('/api', routes);

// ── Error Handling Middleware ──────────────────────────────────────────────
app.use(notFoundHandler);
app.use(errorHandler);

export default app;
