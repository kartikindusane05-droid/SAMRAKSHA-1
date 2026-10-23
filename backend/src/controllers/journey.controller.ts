import { Request, Response } from 'express';
import { journeyService } from '../services/journey.service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateJourneyDto, UpdateJourneyLocationDto, EndJourneyDto, JourneyQueryParams } from '../models/journey.model';
import { logger } from '../utils/logger';

/**
 * GET /api/journeys
 * Retrieves journeys for the authenticated user, with optional query filters: status, limit.
 */
export const getAllJourneys = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const query: JourneyQueryParams & { userId: string } = {
      status: req.query.status as any,
      userId,
      limit: req.query.limit ? parseInt(req.query.limit as string, 10) : undefined
    };

    const journeys = await journeyService.getAllJourneys(query);
    sendSuccess(res, `Retrieved ${journeys.length} journeys`, journeys);
  } catch (err: any) {
    logger.error('Error fetching journeys:', err.message);
    sendError(res, 'Failed to retrieve journeys', 500);
  }
};

/**
 * GET /api/journeys/active
 * Returns the current active (IN_PROGRESS) journey for the authenticated user.
 */
export const getActiveJourney = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const active = await journeyService.getActiveJourney(userId);

    if (!active) {
      sendSuccess(res, 'No active journey found', null);
      return;
    }

    sendSuccess(res, 'Active journey retrieved', active);
  } catch (err: any) {
    logger.error('Error fetching active journey:', err.message);
    sendError(res, 'Failed to fetch active journey', 500);
  }
};

/**
 * GET /api/journeys/:id
 * Retrieves a specific journey by ID, ensuring user ownership.
 */
export const getJourneyById = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const { id } = req.params;
    const journey = await journeyService.getJourneyById(id, userId);

    if (!journey) {
      sendError(res, `Journey not found with ID: ${id}`, 404);
      return;
    }

    sendSuccess(res, 'Journey retrieved', journey);
  } catch (err: any) {
    logger.error('Error fetching journey by ID:', err.message);
    sendError(res, 'Failed to retrieve journey', 500, err.message);
  }
};

/**
 * POST /api/journeys
 * Starts a new journey. Accepts CreateJourneyDto in the request body.
 */
export const startJourney = async (req: Request, res: Response): Promise<void> => {
  try {
    const dto: CreateJourneyDto = req.body;

    if (!dto.startLocationName || !dto.endLocationName) {
      sendError(res, 'startLocationName and endLocationName are required', 400);
      return;
    }

    if (dto.startLatitude === undefined || dto.startLongitude === undefined) {
      sendError(res, 'startLatitude and startLongitude are required', 400);
      return;
    }

    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const journey = await journeyService.startJourney(userId, dto);

    logger.info(`Journey started: ${journey.id} → ${journey.endLocationName}`);
    sendSuccess(res, 'Journey started successfully. Guardians notified.', journey, 201);
  } catch (err: any) {
    logger.error('Error starting journey:', err.message);
    sendError(res, 'Failed to start journey', 500);
  }
};

/**
 * PATCH /api/journeys/:id/location
 * Updates current GPS coordinates during active journey, scoped to user.
 */
export const updateJourneyLocation = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const { id } = req.params;
    const dto: UpdateJourneyLocationDto = req.body;

    if (dto.currentLatitude === undefined || dto.currentLongitude === undefined) {
      sendError(res, 'currentLatitude and currentLongitude are required', 400);
      return;
    }

    const updated = await journeyService.updateLocation(id, userId, dto);

    if (!updated) {
      sendError(res, `Journey not found with ID: ${id}`, 404);
      return;
    }

    sendSuccess(res, 'Journey location updated', updated);
  } catch (err: any) {
    logger.error('Error updating journey location:', err.message);
    sendError(res, 'Failed to update journey location', 500, err.message);
  }
};

/**
 * PATCH /api/journeys/:id/end
 * Ends (completes or cancels) an active journey, scoped to user.
 */
export const endJourney = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const { id } = req.params;
    const dto: EndJourneyDto = req.body;

    if (!dto.status || !['COMPLETED', 'CANCELLED'].includes(dto.status)) {
      sendError(res, 'status must be "COMPLETED" or "CANCELLED"', 400);
      return;
    }

    const ended = await journeyService.endJourney(id, userId, dto);

    if (!ended) {
      sendError(res, `Journey not found with ID: ${id}`, 404);
      return;
    }

    const message =
      dto.status === 'COMPLETED'
        ? 'Journey completed! Arrived safely at destination.'
        : 'Journey monitoring cancelled.';

    logger.info(`Journey ${dto.status}: ${id}`);
    sendSuccess(res, message, ended);
  } catch (err: any) {
    logger.error('Error ending journey:', err.message);
    sendError(res, 'Failed to end journey', 500, err.message);
  }
};

/**
 * DELETE /api/journeys/:id
 * Deletes a journey record owned by the authenticated user.
 */
export const deleteJourney = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const { id } = req.params;
    const deleted = await journeyService.deleteJourney(id, userId);

    if (!deleted) {
      sendError(res, `Journey not found with ID: ${id}`, 404);
      return;
    }

    sendSuccess(res, 'Journey record deleted successfully');
  } catch (err: any) {
    logger.error('Error deleting journey:', err.message);
    sendError(res, 'Failed to delete journey', 500, err.message);
  }
};

/**
 * POST /api/journeys/sync
 * Batch sync journeys from offline tracking client.
 */
export const syncJourneys = async (req: Request, res: Response): Promise<void> => {
  try {
    const userId = (req as any).userId as string;
    if (!userId) {
      sendError(res, 'Unauthorized', 401);
      return;
    }
    const incoming = Array.isArray(req.body) ? req.body : req.body.journeys || [];
    const journeys = await journeyService.syncJourneys(userId, incoming);
    sendSuccess(res, `Successfully synchronized ${journeys.length} journeys`, journeys);
  } catch (err: any) {
    logger.error('Error synchronizing journeys:', err.message);
    sendError(res, 'Failed to synchronize journeys', 500);
  }
};
