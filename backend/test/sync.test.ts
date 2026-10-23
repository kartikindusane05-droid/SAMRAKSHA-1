import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import app from '../src/app';
import { closeDb } from '../src/db';

test('SAMRAKSHA Backend Synchronization Suite', async (t) => {
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, resolve));
  const port = (server.address() as any).port;
  const baseUrl = `http://127.0.0.1:${port}`;

  t.after(async () => {
    server.close();
    await closeDb();
  });

  // Create an authenticated test user
  const signupPayload = {
    fullName: 'Sync Test User',
    email: `sync_test_${Date.now()}@samraksha.local`,
    phone: `+9198${Date.now().toString().slice(-8)}`,
    password: 'SecurePassword123!'
  };

  const signupRes = await fetch(`${baseUrl}/api/auth/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(signupPayload)
  });

  assert.equal(signupRes.status, 201);
  const signupBody = (await signupRes.json()) as any;
  const authToken = signupBody.data.token;
  const testUserId = signupBody.data.user.id;
  assert.ok(authToken, 'Auth token must be returned on signup');
  assert.ok(testUserId, 'User ID must be returned on signup');

  const authHeaders = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${authToken}`
  };

  await t.test('1. User Profile Synchronization (POST /api/auth/sync)', async () => {
    const payload = {
      fullName: 'Karthik Updated',
      phone: signupPayload.phone,
      medicalInfo: {
        bloodGroup: 'B+',
        allergies: 'Penicillin',
        isOrganDonor: true
      }
    };

    const res = await fetch(`${baseUrl}/api/auth/sync`, {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify(payload)
    });

    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.equal(body.data.fullName, 'Karthik Updated');
    assert.equal(body.data.medicalInfo.bloodGroup, 'B+');
    assert.equal(body.data.medicalInfo.allergies, 'Penicillin');
    assert.equal(body.data.medicalInfo.isOrganDonor, true);
  });

  await t.test('2. Emergency Contacts Batch Synchronization (POST /api/contacts/sync)', async () => {
    const offlineContacts = [
      {
        id: 'contact_sync_001',
        name: 'Dr. Ramesh Kumar',
        phoneNumber: '+919845012345',
        relationship: 'Family Doctor',
        userId: testUserId
      },
      {
        id: 'contact_sync_002',
        name: 'Priya Sharma',
        phoneNumber: '+919876543210',
        relationship: 'Sister',
        userId: testUserId
      }
    ];

    const res = await fetch(`${baseUrl}/api/contacts/sync`, {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify(offlineContacts)
    });

    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.ok(Array.isArray(body.data));
    const ids = body.data.map((c: any) => c.id);
    assert.ok(ids.includes('contact_sync_001'));
    assert.ok(ids.includes('contact_sync_002'));
  });

  await t.test('3. Emergency Events Synchronization (POST /api/emergency-events/sync)', async () => {
    const offlineEvents = [
      {
        id: 'event_sync_001',
        timestamp: Date.now() - 3600000,
        latitude: 12.9716,
        longitude: 77.5946,
        status: 'RESOLVED',
        eventType: 'MANUAL_SOS',
        address: 'MG Road, Bangalore',
        notes: 'False alarm - safely resolved offline',
        resolvedAt: Date.now() - 1800000,
        userId: testUserId
      }
    ];

    const res = await fetch(`${baseUrl}/api/emergency-events/sync`, {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify(offlineEvents)
    });

    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.ok(Array.isArray(body.data));
    const event = body.data.find((e: any) => e.id === 'event_sync_001');
    assert.ok(event);
    assert.equal(event.status, 'RESOLVED');
    assert.equal(event.eventType, 'MANUAL_SOS');
  });

  await t.test('4. Journeys Synchronization (POST /api/journeys/sync)', async () => {
    const offlineJourneys = [
      {
        id: 'journey_sync_001',
        userId: testUserId,
        startLocationName: 'Indiranagar 100ft Rd',
        endLocationName: 'Koramangala 5th Block',
        startTime: new Date(Date.now() - 7200000).toISOString(),
        completedTime: new Date(Date.now() - 3600000).toISOString(),
        status: 'COMPLETED',
        startLatitude: 12.9784,
        startLongitude: 77.6408,
        currentLatitude: 12.9352,
        currentLongitude: 77.6245,
        destinationLatitude: 12.9352,
        destinationLongitude: 77.6245
      }
    ];

    const res = await fetch(`${baseUrl}/api/journeys/sync`, {
      method: 'POST',
      headers: authHeaders,
      body: JSON.stringify(offlineJourneys)
    });

    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.ok(Array.isArray(body.data));
    const synced = body.data.find((j: any) => j.id === 'journey_sync_001');
    assert.ok(synced);
    assert.equal(synced.status, 'COMPLETED');
    assert.equal(synced.endLocationName, 'Koramangala 5th Block');
  });

  await t.test('5. Safety Zones Synchronization (POST /api/safety-zones/sync)', async () => {
    const cachedZones = [
      {
        id: 'zone_sync_001',
        name: 'Cubbon Park Safe Corridor',
        latitude: 12.9763,
        longitude: 77.5929,
        radius: 600,
        riskLevel: 'SAFE',
        description: 'Well-lit pedestrian corridor with CCTV and active patrol'
      }
    ];

    const res = await fetch(`${baseUrl}/api/safety-zones/sync`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cachedZones)
    });

    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.ok(body.data.zones.length > 0);
    const zone = body.data.zones.find((z: any) => z.id === 'zone_sync_001');
    assert.ok(zone);
    assert.equal(zone.name, 'Cubbon Park Safe Corridor');
  });

  await t.test('6. Unauthorized Access Rejection (401 without Bearer token)', async () => {
    const res = await fetch(`${baseUrl}/api/contacts`);
    assert.equal(res.status, 401);
    const body = (await res.json()) as any;
    assert.equal(body.success, false);
  });
});
