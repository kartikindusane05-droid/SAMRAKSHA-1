import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import app from '../src/app';
import { closeDb } from '../src/db';
import { incidentReportRepository } from '../src/db/repositories/incident-report.repository';
import { incidentRateLimiter } from '../src/services/incident-report.service';

test('SAMRAKSHA Incident Reporting & Moderation Backend Suite', async (t) => {
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, resolve));
  const port = (server.address() as any).port;
  const baseUrl = `http://127.0.0.1:${port}`;

  t.after(async () => {
    server.close();
    await closeDb();
  });

  let tokenA = '';
  let userAId = '';
  let tokenB = '';
  let userBId = '';

  const ts = Date.now();
  const userA = {
    fullName: 'Ananya Roy',
    email: `ananya_${ts}@samraksha.local`,
    phone: `+9191${ts.toString().slice(-8)}`,
    password: 'AnanyaSecure123!'
  };

  const userB = {
    fullName: 'Kiran Patel',
    email: `kiran_${ts}@samraksha.local`,
    phone: `+9192${ts.toString().slice(-8)}`,
    password: 'KiranSecure123!'
  };

  await t.test('1. Setup Users for Incident Reporting Tests', async () => {
    const resA = await fetch(`${baseUrl}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userA)
    });
    assert.equal(resA.status, 201);
    const bodyA = (await resA.json()) as any;
    tokenA = bodyA.data.token;
    userAId = bodyA.data.user.id;

    const resB = await fetch(`${baseUrl}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userB)
    });
    assert.equal(resB.status, 201);
    const bodyB = (await resB.json()) as any;
    tokenB = bodyB.data.token;
    userBId = bodyB.data.user.id;
  });

  await t.test('2. Unauthenticated Submission Rejected with 401', async () => {
    const res = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        category: 'HARASSMENT',
        latitude: 12.9716,
        longitude: 77.5946,
        incidentDate: '2026-10-20',
        description: 'Street harassment incident'
      })
    });
    assert.equal(res.status, 401);
  });

  await t.test('3. Missing or Invalid Fields Rejected with 400', async () => {
    // Missing category
    const resNoCategory = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({
        latitude: 12.9716,
        longitude: 77.5946,
        incidentDate: '2026-10-20'
      })
    });
    assert.equal(resNoCategory.status, 400);

    // Invalid category
    const resBadCategory = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({
        category: 'INVALID_CATEGORY',
        latitude: 12.9716,
        longitude: 77.5946,
        incidentDate: '2026-10-20'
      })
    });
    assert.equal(resBadCategory.status, 400);

    // Invalid coordinates (out of range)
    const resBadCoords = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({
        category: 'THEFT',
        latitude: 195.0,
        longitude: 77.5946,
        incidentDate: '2026-10-20'
      })
    });
    assert.equal(resBadCoords.status, 400);

    // Invalid date format
    const resBadDate = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({
        category: 'THEFT',
        latitude: 12.9716,
        longitude: 77.5946,
        incidentDate: '20-10-2026'
      })
    });
    assert.equal(resBadDate.status, 400);
  });

  await t.test('4. Future Incident Dates Rejected with 400', async () => {
    const futureDate = '2099-12-31';
    const res = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({
        category: 'SUSPICIOUS_ACTIVITY',
        latitude: 12.9716,
        longitude: 77.5946,
        incidentDate: futureDate,
        description: 'Attempting future report'
      })
    });
    assert.equal(res.status, 400);
    const body = (await res.json()) as any;
    assert.match(body.message, /future/i);
  });

  let createdReportId = '';
  const testIncidentDate = new Date().toISOString().split('T')[0];
  const testLat = 12.0 + ((Date.now() % 1000) / 1000.0);
  const testLng = 77.0 + ((Date.now() % 1000) / 1000.0);

  await t.test('5. Valid Incident Submission & Database Persistence', async () => {
    incidentRateLimiter.reset(userAId);

    const payload = {
      category: 'HARASSMENT',
      latitude: testLat,
      longitude: testLng,
      incidentDate: testIncidentDate,
      description: 'Verbal harassment near metro gate 2.'
    };

    const res = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify(payload)
    });

    assert.equal(res.status, 201);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.ok(body.data.id.startsWith('inc_'));
    assert.equal(body.data.category, 'HARASSMENT');
    assert.equal(body.data.status, 'PENDING_REVIEW');
    assert.equal(body.data.incidentDate, testIncidentDate);
    assert.equal(body.data.description, 'Verbal harassment near metro gate 2.');

    // Public / client response must NOT leak reporter identity or internal fields
    assert.equal(body.data.userId, undefined);
    assert.equal(body.data.email, undefined);
    assert.equal(body.data.phone, undefined);

    createdReportId = body.data.id;

    // Verify database persistence via direct repository lookup
    const inDb = await incidentReportRepository.findById(createdReportId);
    assert.ok(inDb);
    assert.equal(inDb?.userId, userAId);
    assert.equal(inDb?.status, 'PENDING_REVIEW');
    assert.equal(inDb?.latitude, testLat);
    assert.equal(inDb?.longitude, testLng);
  });

  await t.test('6. Duplicate Detection Warning & Confirmation Override', async () => {
    incidentRateLimiter.reset(userBId);

    // User B submits an incident in the same category within ~150 meters on the same date
    const duplicatePayload = {
      category: 'HARASSMENT',
      latitude: testLat + 0.001, // ~110m away
      longitude: testLng + 0.001,
      incidentDate: testIncidentDate,
      description: 'Another witness reporting the same event near metro gate 2.'
    };

    // First attempt without confirmDuplicate: returns warning
    const resWarning = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenB}`
      },
      body: JSON.stringify(duplicatePayload)
    });

    assert.equal(resWarning.status, 200);
    const bodyWarning = (await resWarning.json()) as any;
    assert.equal(bodyWarning.success, false);
    assert.equal(bodyWarning.warning, 'POTENTIAL_DUPLICATE');
    assert.ok(bodyWarning.data?.isPotentialDuplicate);
    // Warning must NOT expose the previous reporter's identity, email, or user ID
    assert.equal(bodyWarning.data?.userId, undefined);
    assert.equal(bodyWarning.data?.reporterName, undefined);

    // Second attempt with confirmDuplicate = true: accepted and persisted
    const resConfirmed = await fetch(`${baseUrl}/api/incidents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenB}`
      },
      body: JSON.stringify({
        ...duplicatePayload,
        confirmDuplicate: true
      })
    });

    assert.equal(resConfirmed.status, 201);
    const bodyConfirmed = (await resConfirmed.json()) as any;
    assert.equal(bodyConfirmed.success, true);
    assert.equal(bodyConfirmed.data.status, 'PENDING_REVIEW');
  });

  await t.test('7. Privacy & Authorization: User B Cannot Access User A Report', async () => {
    // User B attempts to access User A's report details
    const resForbidden = await fetch(`${baseUrl}/api/incidents/${createdReportId}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenB}` }
    });
    assert.equal(resForbidden.status, 403);

    // User A can access their own report details
    const resAllowed = await fetch(`${baseUrl}/api/incidents/${createdReportId}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenA}` }
    });
    assert.equal(resAllowed.status, 200);
    const bodyAllowed = (await resAllowed.json()) as any;
    assert.equal(bodyAllowed.data.id, createdReportId);
  });

  await t.test('8. Moderation Authorization: Admin Status Updates', async () => {
    // 1. Unauthorized attempt without X-Admin-Key: rejected with 403
    const resNoKey = await fetch(`${baseUrl}/api/incidents/${createdReportId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'VERIFIED', moderatorNotes: 'Confirmed with local security patrol.' })
    });
    assert.equal(resNoKey.status, 403);

    // 2. Unauthorized attempt with invalid X-Admin-Key: rejected with 403
    const resBadKey = await fetch(`${baseUrl}/api/incidents/${createdReportId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Key': 'incorrect_admin_key_12345'
      },
      body: JSON.stringify({ status: 'VERIFIED' })
    });
    assert.equal(resBadKey.status, 403);

    // 3. Authorized moderation with correct X-Admin-Key
    const adminKey = process.env.ADMIN_API_KEY || 'samraksha_admin_secret_key';
    const resAuthorized = await fetch(`${baseUrl}/api/incidents/${createdReportId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Key': adminKey
      },
      body: JSON.stringify({
        status: 'VERIFIED',
        moderatorNotes: 'Verified via local civic patrol audit.'
      })
    });

    assert.equal(resAuthorized.status, 200);
    const bodyAuthorized = (await resAuthorized.json()) as any;
    assert.equal(bodyAuthorized.success, true);
    assert.equal(bodyAuthorized.data.status, 'VERIFIED');
    assert.equal(bodyAuthorized.data.moderatorNotes, 'Verified via local civic patrol audit.');

    // Confirm database reflects the verified state
    const inDb = await incidentReportRepository.findById(createdReportId);
    assert.equal(inDb?.status, 'VERIFIED');
  });

  await t.test('9. User My-Reports Listing Returns Owned Reports', async () => {
    const res = await fetch(`${baseUrl}/api/incidents/my-reports`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenA}` }
    });
    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.ok(Array.isArray(body.data));
    assert.ok(body.data.some((r: any) => r.id === createdReportId));
  });
});
