-- SAMRAKSHA Migration 002: Seed Standard Safety Zones and Store Catalog
-- Idempotent upsert so it can be re-run safely

-- 1. Standard Bengaluru Safety Zones
INSERT INTO safety_zones (
  id, name, latitude, longitude, radius, risk_level, description, safety_score, advisory, active_patrols, cctv_coverage
) VALUES
  (
    'zone_001_mgroad',
    'MG Road Tech & Commercial Boulevard',
    12.9746, 77.6094, 650, 'SAFE',
    'Prime metro corridor with continuous 24/7 security patrols, high foot traffic, and extensive public illumination.',
    94,
    'Safe to walk 24/7. Police beat booths active along the main walkway.',
    4, 96
  ),
  (
    'zone_002_civic_hospital',
    'Civic Hospital & University Perimeter',
    12.9660, 77.5990, 450, 'SAFE',
    'Healthcare hub with 24-hour emergency vehicle access, verified safe havens, and security checkpoints.',
    89,
    'Well-monitored. Dedicated emergency response assistance available.',
    3, 88
  ),
  (
    'zone_003_industrial_alley',
    'Industrial Transit & Mixed Alleyway',
    12.9696, 77.5880, 500, 'MODERATE_RISK',
    'Commercial warehouse zone with sparse pedestrian movement and reduced lighting after 9:00 PM.',
    60,
    'Caution advised during late night hours. Stick to primary lit roads.',
    1, 40
  ),
  (
    'zone_004_railway_underpass',
    'Old Railway Canal Underpass',
    12.9830, 77.6010, 380, 'UNSAFE',
    'Poorly lit underpass with minimal CCTV surveillance and multiple incident reports.',
    28,
    'High risk area. Avoid after sunset or travel accompanied.',
    0, 15
  )
ON CONFLICT (id) DO NOTHING;

-- 2. Standard Safety Store Catalog (Verified Safety Gear)
INSERT INTO products (
  id, name, description, image_url, price, original_price, currency,
  category, category_id, store_name, affiliate_url, is_featured, rating, review_count, badge, in_stock, click_count
) VALUES
  (
    'prod_001_sirona_spray',
    'Sirona Impower Pepper Spray for Women',
    'Compact, pocket-size self-defence pepper spray with strong OC formula for instant deterrent protection and quick-reach thumb safety.',
    'https://m.media-amazon.com/images/I/61bQnBo4j4L._SL1500_.jpg',
    199.00, 249.00, 'INR',
    'Personal Safety', 'personal_safety', 'Amazon India',
    'https://amzn.in/d/02M1hVHn',
    TRUE, 4.8, 1420, 'Bestseller', TRUE, 0
  ),
  (
    'prod_002_eveready_dl102',
    'EVEREADY Siren DL102 Rechargeable LED Torch',
    'Rechargeable bright LED emergency torch integrated with an audible safety alarm siren and durable lightweight handheld body.',
    'https://m.media-amazon.com/images/I/618nupcI3IL._SL1500_.jpg',
    169.00, 220.00, 'INR',
    'Emergency Lighting', 'lighting', 'Amazon India',
    'https://amzn.in/d/0ijyoq4c',
    TRUE, 4.7, 860, 'Featured', TRUE, 0
  ),
  (
    'prod_003_shadow_alarm',
    'Shadow Securitronics 130dB Personal Safety Alarm with LED',
    'High-decibel 130dB emergency keychain siren with built-in LED strobe flashlight and swift pull-pin activation for immediate distress signalling.',
    'https://m.media-amazon.com/images/I/71mX5ZidbbL._SL1500_.jpg',
    437.00, 699.00, 'INR',
    'Personal Safety', 'personal_safety', 'Amazon India',
    'https://amzn.in/d/0c0MxMd6',
    TRUE, 4.6, 950, 'Recommended', TRUE, 0
  ),
  (
    'prod_004_siago_hammer',
    'SIAGO Car Safety Hammer SIA478',
    'Multi-function automotive emergency escape tool with hardened steel window glass breaker and integrated seatbelt cutter for vehicle rescue.',
    'https://m.media-amazon.com/images/I/71sqm4fWbtL._SL1500_.jpg',
    399.00, 599.00, 'INR',
    'Travel Safety', 'travel_safety', 'Amazon India',
    'https://amzn.in/d/04zxGvUA',
    FALSE, 4.5, 410, 'Vehicle Essential', TRUE, 0
  ),
  (
    'prod_005_dwc_alarm',
    'Devil Will Cry Defence Emergency Alarm Keychain',
    'Compact, ultra-loud personal alarm keychain designed for women, students, and seniors with convenient bag clip and quick-pull distress pin.',
    'https://m.media-amazon.com/images/I/81GkOfiJQCL._SL1500_.jpg',
    384.00, 499.00, 'INR',
    'Personal Safety', 'personal_safety', 'Amazon India',
    'https://amzn.in/d/06q8klje',
    FALSE, 4.4, 520, NULL, TRUE, 0
  ),
  (
    'prod_006_anti_choking',
    'Portable Anti Choking Device — One-Button Operation, 3 Masks Included',
    'Emergency choking rescue suction device featuring intuitive one-button operation, dual one-way valves, and 3 interchangeable masks for all ages.',
    'https://m.media-amazon.com/images/I/71VxKz9t0VL._SL1500_.jpg',
    1399.00, 1999.00, 'INR',
    'First Aid', 'first_aid', 'Amazon India',
    'https://amzn.in/d/0h7tLqK9',
    TRUE, 4.9, 310, 'Life Saving', TRUE, 0
  ),
  (
    'prod_007_dwc_spray',
    'Devil Will Cry Pepper Spray for Women Safety',
    'Long-range stream defence pepper spray with 10–12 ft accurate reach, safety flip-top dispenser, and non-lethal maximum deterrence formula.',
    'https://m.media-amazon.com/images/I/71UmiAE0q8L._SL1403_.jpg',
    207.00, 299.00, 'INR',
    'Personal Safety', 'personal_safety', 'Amazon India',
    'https://amzn.in/d/06qJCOU2',
    FALSE, 4.6, 680, NULL, TRUE, 0
  ),
  (
    'prod_008_eveready_dl101',
    'EVEREADY DL 101 Minimight Rechargeable Emergency Torch',
    'Pocket-sized rechargeable emergency LED torch with long battery backup, slide power switch, and convenient foldaway charging plug.',
    'https://m.media-amazon.com/images/I/71+pBhQRdwL._SL1500_.jpg',
    139.00, 180.00, 'INR',
    'Emergency Lighting', 'lighting', 'Amazon India',
    'https://amzn.in/d/0iopyunM',
    FALSE, 4.5, 1140, 'Budget Pick', TRUE, 0
  )
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  image_url = EXCLUDED.image_url,
  price = EXCLUDED.price,
  original_price = EXCLUDED.original_price,
  currency = EXCLUDED.currency,
  category = EXCLUDED.category,
  category_id = EXCLUDED.category_id,
  store_name = EXCLUDED.store_name,
  affiliate_url = EXCLUDED.affiliate_url,
  is_featured = EXCLUDED.is_featured,
  rating = EXCLUDED.rating,
  review_count = EXCLUDED.review_count,
  badge = EXCLUDED.badge,
  in_stock = EXCLUDED.in_stock,
  updated_at = NOW();
