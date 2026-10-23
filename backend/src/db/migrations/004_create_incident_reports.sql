-- SAMRAKSHA Migration 004: Create Incident Reports Schema
-- Stores persistent community safety incident reports with moderation workflow

CREATE TABLE IF NOT EXISTS incident_reports (
  id VARCHAR(128) PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  category VARCHAR(64) NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  incident_date DATE NOT NULL,
  description TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
  moderator_notes TEXT,
  reviewed_by VARCHAR(128) REFERENCES users(id) ON DELETE SET NULL,
  reviewed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incident_reports_user_id ON incident_reports (user_id);
CREATE INDEX IF NOT EXISTS idx_incident_reports_status ON incident_reports (status);
CREATE INDEX IF NOT EXISTS idx_incident_reports_category ON incident_reports (category);
CREATE INDEX IF NOT EXISTS idx_incident_reports_date ON incident_reports (incident_date DESC);
CREATE INDEX IF NOT EXISTS idx_incident_reports_coords ON incident_reports (latitude, longitude);
