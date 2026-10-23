-- SAMRAKSHA Migration 001: Initial Relational Database Schema
-- Defines core tables and relationships for full-stack persistence

-- 1. Migration History Table
CREATE TABLE IF NOT EXISTS migrations (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(128) PRIMARY KEY,
  full_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  medical_info JSONB DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users (phone);

-- 3. Sessions Table (Opaque Session Tokens)
CREATE TABLE IF NOT EXISTS sessions (
  token VARCHAR(255) PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions (user_id);

-- 4. Emergency Contacts Table
CREATE TABLE IF NOT EXISTS emergency_contacts (
  id VARCHAR(128) PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  phone_number VARCHAR(50) NOT NULL,
  relationship VARCHAR(100) NOT NULL DEFAULT 'Family',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_emergency_contacts_user_id ON emergency_contacts (user_id);

-- 5. Emergency Events Table
CREATE TABLE IF NOT EXISTS emergency_events (
  id VARCHAR(128) PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  timestamp BIGINT NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
  event_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL_SOS',
  address TEXT,
  notes TEXT,
  resolved_at BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_emergency_events_user_id ON emergency_events (user_id);
CREATE INDEX IF NOT EXISTS idx_emergency_events_status ON emergency_events (status);
CREATE INDEX IF NOT EXISTS idx_emergency_events_timestamp ON emergency_events (timestamp DESC);

-- 6. Journeys Table
CREATE TABLE IF NOT EXISTS journeys (
  id VARCHAR(128) PRIMARY KEY,
  user_id VARCHAR(128) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  start_location_name TEXT NOT NULL,
  end_location_name TEXT NOT NULL,
  start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expected_end_time TIMESTAMPTZ,
  completed_time TIMESTAMPTZ,
  status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
  start_latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  start_longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  current_latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  current_longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  destination_latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  destination_longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_journeys_user_id ON journeys (user_id);
CREATE INDEX IF NOT EXISTS idx_journeys_user_status ON journeys (user_id, status);
CREATE INDEX IF NOT EXISTS idx_journeys_start_time ON journeys (start_time DESC);

-- 7. Safety / Risk Zones Table
CREATE TABLE IF NOT EXISTS safety_zones (
  id VARCHAR(128) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  radius DOUBLE PRECISION NOT NULL,
  risk_level VARCHAR(50) NOT NULL DEFAULT 'SAFE',
  description TEXT NOT NULL,
  safety_score INT DEFAULT 100,
  advisory TEXT,
  active_patrols INT DEFAULT 0,
  cctv_coverage INT DEFAULT 0,
  polygon_points JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_safety_zones_risk_level ON safety_zones (risk_level);

-- 8. Safety Store Products Table
CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(128) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  image_url TEXT NOT NULL,
  price NUMERIC(10, 2) NOT NULL,
  original_price NUMERIC(10, 2),
  currency VARCHAR(10) NOT NULL DEFAULT 'INR',
  category VARCHAR(100) NOT NULL,
  category_id VARCHAR(100) NOT NULL,
  store_name VARCHAR(100) NOT NULL,
  affiliate_url TEXT NOT NULL,
  is_featured BOOLEAN NOT NULL DEFAULT FALSE,
  rating NUMERIC(3, 2) DEFAULT 5.0,
  review_count INT DEFAULT 0,
  badge VARCHAR(100),
  in_stock BOOLEAN NOT NULL DEFAULT TRUE,
  click_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_products_category_id ON products (category_id);
CREATE INDEX IF NOT EXISTS idx_products_is_featured ON products (is_featured);
