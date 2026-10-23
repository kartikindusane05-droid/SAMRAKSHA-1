import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import app from '../src/app';
import { closeDb } from '../src/db';

test('SAMRAKSHA Emergency SOS & Contact Alerts Backend Suite', async (t) => {
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
    fullName: 'Priya Sharma',
    email: `priya_${ts}@samraksha.local`,
    phone: `+9198${ts.toString().slice(-8)}`,
    password: 'PriyaSecure123!'
  };

  const userB = {
    fullName: 'Rahul Verma',
    email: `rahul_${ts}@samraksha.local`,
    phone: `+9197${ts.toString().slice(-8)}`,
    password: 'RahulSecure123!'
  };

  await t.test('1. Setup Users for Emergency Alert Testing', async () => {
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

  let contactAId = '';

  await t.test('2. Add Emergency Contacts with Phone Number Validation', async () => {
    // Attempt invalid contact with blank phone
    const resInvalid = await fetch(`${baseUrl}/api/contacts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({ name: 'Guardian Mother', phoneNumber: '' })
    });
    assert.equal(resInvalid.status, 400);

    // Valid contact creation
    const resValid = await fetch(`${baseUrl}/api/contacts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({
        name: 'Guardian Mother',
        phoneNumber: '+919876543210',
        relationship: 'Mother'
      })
    });
    assert.equal(resValid.status, 201);
    const bodyValid = (await resValid.json()) as any;
    assert.equal(bodyValid.success, true);
    assert.equal(bodyValid.data.name, 'Guardian Mother');
    assert.equal(bodyValid.data.phoneNumber, '+919876543210');
    assert.equal(bodyValid.data.userId, userAId);
    contactAId = bodyValid.data.id;
  });

  await t.test('3. Privacy & Authorization: User B Cannot Access User A Contact', async () => {
    // User B tries to get User A's contact
    const resForbidden = await fetch(`${baseUrl}/api/contacts/${contactAId}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenB}` }
    });
    assert.equal(resForbidden.status, 404);

    // User B contact list does not contain User A contact
    const resListB = await fetch(`${baseUrl}/api/contacts`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenB}` }
    });
    const bodyListB = (await resListB.json()) as any;
    assert.equal(bodyListB.data.length, 0);
  });

  let eventId = '';

  await t.test('4. Create Manual SOS Emergency Event with Coordinates & Delivery Notes', async () => {
    const emergencyPayload = {
      latitude: 12.9716,
      longitude: 77.5946,
      eventType: 'MANUAL_SOS',
      notes: 'Deliberate press-and-hold trigger. Alerts dispatched to 1 contact(s).',
      timestamp: Date.now()
    };

    const res = await fetch(`${baseUrl}/api/emergency-events`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify(emergencyPayload)
    });
    assert.equal(res.status, 201);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.equal(body.data.userId, userAId);
    assert.equal(body.data.status, 'ACTIVE');
    assert.equal(body.data.eventType, 'MANUAL_SOS');
    assert.equal(body.data.latitude, 12.9716);
    assert.equal(body.data.longitude, 77.5946);
    assert.ok(body.data.notes.includes('Deliberate press-and-hold trigger'));
    eventId = body.data.id;
  });

  await t.test('5. Privacy & Authorization: User B Cannot Access User A Emergency Event', async () => {
    const resForbidden = await fetch(`${baseUrl}/api/emergency-events/${eventId}`, {
      method: 'GET',
      headers: { Authorization: `Bearer ${tokenB}` }
    });
    assert.equal(resForbidden.status, 404);

    // User B cannot patch status of User A event
    const resPatchForbidden = await fetch(`${baseUrl}/api/emergency-events/${eventId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenB}`
      },
      body: JSON.stringify({ status: 'CANCELLED' })
    });
    assert.equal(resPatchForbidden.status, 404);
  });

  await t.test('6. Cancel/Resolve SOS Event by Authorized Owner', async () => {
    const res = await fetch(`${baseUrl}/api/emergency-events/${eventId}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokenA}`
      },
      body: JSON.stringify({ status: 'CANCELLED', resolvedAt: Date.now() })
    });
    assert.equal(res.status, 200);
    const body = (await res.json()) as any;
    assert.equal(body.success, true);
    assert.equal(body.data.status, 'CANCELLED');
    assert.ok(body.data.resolvedAt);
  });

  await t.test('7. Unauthorized Requests Rejected with 401', async () => {
    const resEvents = await fetch(`${baseUrl}/api/emergency-events`, {
      method: 'GET'
    });
    assert.equal(resEvents.status, 401);

    const resContacts = await fetch(`${baseUrl}/api/contacts`, {
      method: 'GET'
    });
    assert.equal(resContacts.status, 401);
  });
});
