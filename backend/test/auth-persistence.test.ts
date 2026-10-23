import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import app from '../src/app';
import { closeDb } from '../src/db';

test('SAMRAKSHA Full-Stack Persistence & Security Test Suite', async (t) => {
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, resolve));
  const port = (server.address() as any).port;
  const baseUrl = `http://127.0.0.1:${port}`;

  t.after(async () => {
    server.close();
    await closeDb();
  });

  let userAToken = '';
  let userAId = '';
  let userBToken = '';
  let userBId = '';

  const timestamp = Date.now();
  const userAPayload = {
    fullName: 'Alice Walker',
    email: `alice_${timestamp}@samraksha.local`,
    phone: `+9191${timestamp.toString().slice(-8)}`,
    password: 'AliceSecurePassword456!'
  };

  const userBPayload = {
    fullName: 'Bob Smith',
    email: `bob_${timestamp}@samraksha.local`,
    phone: `+9192${timestamp.toString().slice(-8)}`,
    password: 'BobSecurePassword789!'
  };

  await t.test('1. User Signup & Authentication Persistence', async () => {
    // Signup Alice
    const resA = await fetch(`${baseUrl}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userAPayload)
    });
    assert.equal(resA.status, 201);
    const bodyA = (await resA.json()) as any;
    assert.equal(bodyA.success, true);
    assert.ok(bodyA.data.token);
    assert.equal(bodyA.data.user.email, userAPayload.email);
    assert.equal(bodyA.data.user.passwordHash, undefined, 'passwordHash must never be exposed');
    userAToken = bodyA.data.token;
    userAId = bodyA.data.user.id;

    // Signup Bob
    const resB = await fetch(`${baseUrl}/api/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(userBPayload)
    });
    assert.equal(resB.status, 201);
    const bodyB = (await resB.json()) as any;
    userBToken = bodyB.data.token;
    userBId = bodyB.data.user.id;

    // Verify Login for Alice
    const loginRes = await fetch(`${baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        identifier: userAPayload.email,
        password: userAPayload.password
      })
    });
    assert.equal(loginRes.status, 200);
    const loginBody = (await loginRes.json()) as any;
    assert.equal(loginBody.success, true);
    assert.ok(loginBody.data.token);
    assert.equal(loginBody.data.user.id, userAId);
  });

  await t.test('2. Profile Access with Session Bearer Token', async () => {
    const res = await fetch(`${baseUrl}/api/auth/profile`, {
      headers: { Authorization: `Bearer ${userAToken}` }
    });
    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.data.id, userAId);
    assert.equal(body.data.fullName, userAPayload.fullName);
  });

  let contactAId = '';

  await t.test('3. Emergency Contact Creation & Retrieval for User A', async () => {
    const contactPayload = {
      name: 'Alice Mother',
      phoneNumber: '+919911223344',
      relationship: 'Mother'
    };

    const res = await fetch(`${baseUrl}/api/contacts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userAToken}`
      },
      body: JSON.stringify(contactPayload)
    });

    assert.equal(res.status, 201);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.equal(body.data.name, 'Alice Mother');
    assert.equal(body.data.userId, userAId);
    contactAId = body.data.id;
  });

  await t.test('4. Enforce Authorization: User B cannot access User A contact', async () => {
    // User B attempts to get Alice's contact
    const res = await fetch(`${baseUrl}/api/contacts/${contactAId}`, {
      headers: { Authorization: `Bearer ${userBToken}` }
    });
    // Must be 404 Not Found (or 403 Forbidden)
    assert.ok([404, 403].includes(res.status), `Expected 404 or 403, got ${res.status}`);

    // User B listing contacts must NOT contain Alice's contact
    const listRes = await fetch(`${baseUrl}/api/contacts`, {
      headers: { Authorization: `Bearer ${userBToken}` }
    });
    assert.equal(listRes.status, 200);
    const listBody = (await listRes.json()) as any;
    const ids = listBody.data.map((c: any) => c.id);
    assert.equal(ids.includes(contactAId), false, 'Alice contact must not be visible to Bob');
  });

  let journeyAId = '';

  await t.test('5. Journey Lifecycle: Start, Update Location, End', async () => {
    // Start Journey
    const startRes = await fetch(`${baseUrl}/api/journeys`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userAToken}`
      },
      body: JSON.stringify({
        startLocationName: 'MG Road Metro',
        endLocationName: 'Home',
        startLatitude: 12.9716,
        startLongitude: 77.5946,
        destinationLatitude: 12.9352,
        destinationLongitude: 77.6245
      })
    });

    assert.equal(startRes.status, 201);
    const startBody = (await startRes.json()) as any;
    assert.equal(startBody.success, true);
    journeyAId = startBody.data.id;
    assert.equal(startBody.data.status, 'IN_PROGRESS');

    // Update GPS Location
    const updateRes = await fetch(`${baseUrl}/api/journeys/${journeyAId}/location`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userAToken}`
      },
      body: JSON.stringify({
        currentLatitude: 12.9500,
        currentLongitude: 77.6100
      })
    });
    assert.equal(updateRes.status, 200);
    const updateBody = (await updateRes.json()) as any;
    assert.equal(updateBody.data.currentLatitude, 12.9500);

    // End Journey
    const endRes = await fetch(`${baseUrl}/api/journeys/${journeyAId}/end`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userAToken}`
      },
      body: JSON.stringify({ status: 'COMPLETED' })
    });
    assert.equal(endRes.status, 200);
    const endBody = (await endRes.json()) as any;
    assert.equal(endBody.data.status, 'COMPLETED');
  });

  await t.test('6. Enforce Authorization: User B cannot modify User A journey', async () => {
    const res = await fetch(`${baseUrl}/api/journeys/${journeyAId}/location`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userBToken}`
      },
      body: JSON.stringify({ currentLatitude: 0, currentLongitude: 0 })
    });
    assert.ok([404, 403].includes(res.status), `Expected 404 or 403, got ${res.status}`);
  });

  await t.test('7. Emergency Event Creation & Status Resolution', async () => {
    const createRes = await fetch(`${baseUrl}/api/emergency-events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userAToken}`
      },
      body: JSON.stringify({
        latitude: 12.9716,
        longitude: 77.5946,
        eventType: 'MANUAL_SOS',
        address: 'MG Road, Bangalore'
      })
    });

    assert.equal(createRes.status, 201);
    const createBody = (await createRes.json()) as any;
    assert.equal(createBody.success, true);
    const eventId = createBody.data.id;
    assert.equal(createBody.data.status, 'ACTIVE');

    // Resolve event
    const resolveRes = await fetch(`${baseUrl}/api/emergency-events/${eventId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${userAToken}`
      },
      body: JSON.stringify({ status: 'RESOLVED' })
    });
    assert.equal(resolveRes.status, 200);
    const resolveBody = (await resolveRes.json()) as any;
    assert.equal(resolveBody.data.status, 'RESOLVED');
  });
});
