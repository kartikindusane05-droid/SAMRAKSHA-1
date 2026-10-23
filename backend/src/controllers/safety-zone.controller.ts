import { Request, Response } from 'express';
import { safetyZoneService } from '../services/safety-zone.service';
import { CreateSafetyZoneDto, RiskLevel, UpdateSafetyZoneDto } from '../models/safety-zone.model';
import { sendSuccess, sendError } from '../utils/response';

const VALID_RISK_LEVELS: RiskLevel[] = ['SAFE', 'MODERATE_RISK', 'UNSAFE'];

export class SafetyZoneController {
  /**
   * GET /api/safety-zones
   * Retrieve all safety zones with optional filtering by riskLevel and proximity.
   */
  async getZones(req: Request, res: Response): Promise<Response> {
    try {
      const { riskLevel, lat, lng, radius } = req.query;

      if (riskLevel && !VALID_RISK_LEVELS.includes(riskLevel as RiskLevel)) {
        return sendError(
          res,
          `Invalid riskLevel. Must be one of: ${VALID_RISK_LEVELS.join(', ')}`,
          400
        );
      }

      const queryParams = {
        riskLevel: riskLevel as RiskLevel | undefined,
        lat: lat ? parseFloat(lat as string) : undefined,
        lng: lng ? parseFloat(lng as string) : undefined,
        radius: radius ? parseFloat(radius as string) : undefined
      };

      const zones = await safetyZoneService.getAllZones(queryParams);
      return sendSuccess(res, 'Safety zones retrieved successfully', {
        count: zones.length,
        zones
      });
    } catch (error: any) {
      return sendError(res, 'Failed to fetch safety zones', 500, error.message);
    }
  }

  /**
   * GET /api/safety-zones/:id
   * Retrieve a specific safety zone by ID.
   */
  async getZoneById(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const zone = await safetyZoneService.getZoneById(id);

      if (!zone) {
        return sendError(res, `Safety zone with ID '${id}' not found`, 404);
      }

      return sendSuccess(res, 'Safety zone retrieved successfully', zone);
    } catch (error: any) {
      return sendError(res, 'Failed to fetch safety zone', 500, error.message);
    }
  }

  /**
   * POST /api/safety-zones
   * Create a new safety zone record.
   */
  async createZone(req: Request, res: Response): Promise<Response> {
    try {
      const {
        name,
        latitude,
        longitude,
        radius,
        riskLevel,
        description,
        safetyScore,
        advisory,
        activePatrols,
        cctvCoverage,
        polygonPoints
      } = req.body;

      // Validation
      if (!name || typeof name !== 'string' || !name.trim()) {
        return sendError(res, 'Field "name" is required and must be a non-empty string', 400);
      }

      if (typeof latitude !== 'number' || latitude < -90 || latitude > 90) {
        return sendError(res, 'Field "latitude" must be a valid number between -90 and 90', 400);
      }

      if (typeof longitude !== 'number' || longitude < -180 || longitude > 180) {
        return sendError(res, 'Field "longitude" must be a valid number between -180 and 180', 400);
      }

      if (typeof radius !== 'number' || radius <= 0) {
        return sendError(res, 'Field "radius" must be a positive number in meters', 400);
      }

      if (!riskLevel || !VALID_RISK_LEVELS.includes(riskLevel)) {
        return sendError(
          res,
          `Field "riskLevel" is required and must be one of: ${VALID_RISK_LEVELS.join(', ')}`,
          400
        );
      }

      if (!description || typeof description !== 'string' || !description.trim()) {
        return sendError(res, 'Field "description" is required and must be a non-empty string', 400);
      }

      const dto: CreateSafetyZoneDto = {
        name: name.trim(),
        latitude,
        longitude,
        radius,
        riskLevel,
        description: description.trim(),
        safetyScore,
        advisory,
        activePatrols,
        cctvCoverage,
        polygonPoints
      };

      const createdZone = await safetyZoneService.createZone(dto);
      return sendSuccess(res, 'Safety zone created successfully', createdZone, 201);
    } catch (error: any) {
      return sendError(res, 'Failed to create safety zone', 500, error.message);
    }
  }

  /**
   * PUT /api/safety-zones/:id
   * Update an existing safety zone by ID.
   */
  async updateZone(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const {
        name,
        latitude,
        longitude,
        radius,
        riskLevel,
        description,
        safetyScore,
        advisory,
        activePatrols,
        cctvCoverage,
        polygonPoints
      } = req.body;

      const existing = await safetyZoneService.getZoneById(id);
      if (!existing) {
        return sendError(res, `Safety zone with ID '${id}' not found`, 404);
      }

      // Validate optional fields if provided
      if (latitude !== undefined && (typeof latitude !== 'number' || latitude < -90 || latitude > 90)) {
        return sendError(res, 'Field "latitude" must be a valid number between -90 and 90', 400);
      }

      if (longitude !== undefined && (typeof longitude !== 'number' || longitude < -180 || longitude > 180)) {
        return sendError(res, 'Field "longitude" must be a valid number between -180 and 180', 400);
      }

      if (radius !== undefined && (typeof radius !== 'number' || radius <= 0)) {
        return sendError(res, 'Field "radius" must be a positive number in meters', 400);
      }

      if (riskLevel !== undefined && !VALID_RISK_LEVELS.includes(riskLevel)) {
        return sendError(
          res,
          `Field "riskLevel" must be one of: ${VALID_RISK_LEVELS.join(', ')}`,
          400
        );
      }

      const dto: UpdateSafetyZoneDto = {
        name: name?.trim(),
        latitude,
        longitude,
        radius,
        riskLevel,
        description: description?.trim(),
        safetyScore,
        advisory,
        activePatrols,
        cctvCoverage,
        polygonPoints
      };

      const updated = await safetyZoneService.updateZone(id, dto);
      return sendSuccess(res, 'Safety zone updated successfully', updated);
    } catch (error: any) {
      return sendError(res, 'Failed to update safety zone', 500, error.message);
    }
  }

  /**
   * DELETE /api/safety-zones/:id
   * Remove a safety zone record.
   */
  async deleteZone(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const deleted = await safetyZoneService.deleteZone(id);

      if (!deleted) {
        return sendError(res, `Safety zone with ID '${id}' not found`, 404);
      }

      return sendSuccess(res, 'Safety zone deleted successfully', { id });
    } catch (error: any) {
      return sendError(res, 'Failed to delete safety zone', 500, error.message);
    }
  }

  /**
   * POST /api/safety-zones/sync
   * Batch synchronize safety zones from client cache.
   */
  async syncZones(req: Request, res: Response): Promise<Response> {
    try {
      const incoming = Array.isArray(req.body) ? req.body : req.body.zones || [];
      const zones = await safetyZoneService.syncZones(incoming);
      return sendSuccess(res, `Successfully synchronized ${zones.length} safety zones`, {
        count: zones.length,
        zones
      });
    } catch (error: any) {
      return sendError(res, 'Failed to synchronize safety zones', 500, error.message);
    }
  }
}

export const safetyZoneController = new SafetyZoneController();
