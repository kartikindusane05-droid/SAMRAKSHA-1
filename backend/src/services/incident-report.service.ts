import crypto from 'crypto';
import {
  IncidentReport,
  IncidentCategory,
  CreateIncidentReportDto,
  IncidentStatus,
  SanitizedIncidentReport
} from '../models/incident-report.model';
import { incidentReportRepository, IncidentReportRepository } from '../db/repositories/incident-report.repository';
import { logger } from '../utils/logger';

const VALID_CATEGORIES: IncidentCategory[] = [
  'HARASSMENT',
  'THEFT',
  'SUSPICIOUS_ACTIVITY',
  'PHYSICAL_SAFETY',
  'OTHER'
];

/**
 * Escapes HTML characters to prevent stored XSS or HTML injection.
 */
function sanitizeString(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;');
}

/**
 * Validates date string in YYYY-MM-DD format.
 */
function isValidDateString(dateStr: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return false;
  const d = new Date(dateStr + 'T00:00:00Z');
  return !isNaN(d.getTime());
}

/**
 * In-memory sliding window rate limiter: max 10 reports per user per 15 minutes.
 */
class IncidentRateLimiter {
  private userSubmissions = new Map<string, number[]>();
  private readonly windowMs = 15 * 60 * 1000;
  private readonly maxRequests = 10;

  isRateLimited(userId: string): boolean {
    const now = Date.now();
    const timestamps = this.userSubmissions.get(userId) || [];
    const validTimestamps = timestamps.filter((t) => now - t < this.windowMs);

    if (validTimestamps.length >= this.maxRequests) {
      this.userSubmissions.set(userId, validTimestamps);
      return true;
    }

    validTimestamps.push(now);
    this.userSubmissions.set(userId, validTimestamps);
    return false;
  }

  reset(userId?: string) {
    if (userId) {
      this.userSubmissions.delete(userId);
    } else {
      this.userSubmissions.clear();
    }
  }
}

export const incidentRateLimiter = new IncidentRateLimiter();

export class IncidentReportService {
  constructor(private repository: IncidentReportRepository = incidentReportRepository) {}

  /**
   * Sanitizes report for safe public/user display, omitting reporter PII and moderator internal fields.
   */
  sanitize(report: IncidentReport): SanitizedIncidentReport {
    return {
      id: report.id,
      category: report.category,
      latitude: report.latitude,
      longitude: report.longitude,
      incidentDate: report.incidentDate,
      description: report.description,
      status: report.status,
      createdAt: report.createdAt
    };
  }

  async createReport(
    userId: string,
    dto: CreateIncidentReportDto
  ): Promise<{
    report?: SanitizedIncidentReport;
    isDuplicateWarning?: boolean;
    warning?: string;
    message?: string;
  }> {
    // 1. Rate limiting check
    if (incidentRateLimiter.isRateLimited(userId)) {
      throw new Error('RATE_LIMIT_EXCEEDED: Too many incident reports submitted. Please wait a few minutes.');
    }

    // 2. Validate category
    if (!dto.category || !VALID_CATEGORIES.includes(dto.category)) {
      throw new Error(
        `VALIDATION_ERROR: Category must be one of: ${VALID_CATEGORIES.join(', ')}`
      );
    }

    // 3. Validate coordinates
    if (
      typeof dto.latitude !== 'number' ||
      isNaN(dto.latitude) ||
      dto.latitude < -90 ||
      dto.latitude > 90
    ) {
      throw new Error('VALIDATION_ERROR: Valid latitude between -90 and 90 is required.');
    }
    if (
      typeof dto.longitude !== 'number' ||
      isNaN(dto.longitude) ||
      dto.longitude < -180 ||
      dto.longitude > 180
    ) {
      throw new Error('VALIDATION_ERROR: Valid longitude between -180 and 180 is required.');
    }

    // 4. Validate incident date
    if (!dto.incidentDate || !isValidDateString(dto.incidentDate)) {
      throw new Error('VALIDATION_ERROR: Incident date must be in YYYY-MM-DD format.');
    }

    const todayStr = new Date().toISOString().split('T')[0];
    if (dto.incidentDate > todayStr) {
      throw new Error('VALIDATION_ERROR: Incident date cannot be in the future.');
    }

    const reportDate = new Date(dto.incidentDate + 'T00:00:00Z');
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
    if (reportDate < oneYearAgo) {
      throw new Error('VALIDATION_ERROR: Incident date cannot be older than one year.');
    }

    // 5. Validate description
    let sanitizedDescription: string | undefined = undefined;
    if (dto.description) {
      const trimmed = dto.description.trim();
      if (trimmed.length > 500) {
        throw new Error('VALIDATION_ERROR: Description cannot exceed 500 characters.');
      }
      sanitizedDescription = sanitizeString(trimmed);
    }

    // 6. Check for potential duplicate reports (within 500m and ±1 day) unless confirmed
    if (!dto.confirmDuplicate) {
      const duplicates = await this.repository.findPotentialDuplicates(
        dto.category,
        dto.latitude,
        dto.longitude,
        dto.incidentDate,
        500
      );

      if (duplicates.length > 0) {
        logger.info(
          `Potential duplicate incident detected for category ${dto.category} at (${dto.latitude}, ${dto.longitude}) on ${dto.incidentDate}`
        );
        return {
          isDuplicateWarning: true,
          warning: 'POTENTIAL_DUPLICATE',
          message:
            'A similar incident in this approximate area on this date has already been reported. If you witnessed or experienced this incident, you may still confirm and submit your report.'
        };
      }
    }

    // 7. Construct persistent report
    const newReport: IncidentReport = {
      id: `inc_${crypto.randomUUID()}`,
      userId,
      category: dto.category,
      latitude: dto.latitude,
      longitude: dto.longitude,
      incidentDate: dto.incidentDate,
      description: sanitizedDescription,
      status: 'PENDING_REVIEW',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    const saved = await this.repository.create(newReport);
    logger.info(`Incident report created with id ${saved.id} (status: ${saved.status})`);

    return {
      report: this.sanitize(saved)
    };
  }

  async getUserReports(userId: string): Promise<SanitizedIncidentReport[]> {
    const list = await this.repository.findByUserId(userId);
    return list.map((r) => this.sanitize(r));
  }

  async getReportById(id: string, requestingUserId: string, isAdmin: boolean = false): Promise<SanitizedIncidentReport | null> {
    const report = await this.repository.findById(id);
    if (!report) return null;

    // Authorization guard: Only the report creator or an admin can access report details
    if (!isAdmin && report.userId !== requestingUserId) {
      throw new Error('FORBIDDEN: You are not authorized to view this incident report.');
    }

    return this.sanitize(report);
  }

  async updateReportStatus(
    id: string,
    status: IncidentStatus,
    moderatorNotes?: string,
    reviewedBy?: string
  ): Promise<IncidentReport | null> {
    if (!['PENDING_REVIEW', 'VERIFIED', 'REJECTED'].includes(status)) {
      throw new Error('VALIDATION_ERROR: Status must be PENDING_REVIEW, VERIFIED, or REJECTED.');
    }

    const sanitizedNotes = moderatorNotes ? sanitizeString(moderatorNotes.trim()) : undefined;
    return await this.repository.updateStatus(id, status, sanitizedNotes, reviewedBy);
  }
}

export const incidentReportService = new IncidentReportService();
