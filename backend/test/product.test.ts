import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import app from '../src/app';
import { closeDb } from '../src/db';

test('SAMRAKSHA Affiliate Products Backend API Suite', async (t) => {
  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(0, resolve));
  const port = (server.address() as any).port;
  const baseUrl = `http://127.0.0.1:${port}`;

  t.after(async () => {
    server.close();
    await closeDb();
  });

  let sampleProductId = '';

  await t.test('1. Product Listing (GET /api/products)', async () => {
    const res = await fetch(`${baseUrl}/api/products?limit=10&page=1`);
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.ok(Array.isArray(body.data.products));
    assert.ok(body.data.products.length > 0);
    assert.ok(body.data.total >= 8);
    assert.equal(body.data.page, 1);
    assert.equal(body.data.limit, 10);

    // Verify all required fields on the product schema
    const product = body.data.products[0];
    sampleProductId = product.id;

    assert.ok(typeof product.name === 'string' && product.name.length > 0);
    assert.ok(typeof product.description === 'string' && product.description.length > 0);
    assert.ok(typeof product.imageUrl === 'string' && product.imageUrl.startsWith('http'));
    assert.ok(typeof product.price === 'number' && product.price > 0);
    assert.ok(typeof product.category === 'string' && product.category.length > 0);
    assert.ok(typeof product.storeName === 'string' && product.storeName.length > 0);
    assert.ok(typeof product.affiliateUrl === 'string' && product.affiliateUrl.startsWith('http'));
    assert.ok(typeof product.isFeatured === 'boolean');
  });

  await t.test('2. Category Filtering (GET /api/products?category=first_aid)', async () => {
    const res = await fetch(`${baseUrl}/api/products?category=first_aid`);
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.ok(body.data.products.length > 0);

    for (const prod of body.data.products) {
      const match =
        prod.category.toLowerCase().includes('first aid') ||
        prod.categoryId === 'first_aid';
      assert.ok(match, `Expected product category to match 'first_aid', got ${prod.category}`);
    }
  });

  await t.test('3. Featured Status Filtering (GET /api/products?featured=true)', async () => {
    const res = await fetch(`${baseUrl}/api/products?featured=true`);
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.ok(body.data.products.length > 0);

    for (const prod of body.data.products) {
      assert.equal(prod.isFeatured, true);
    }
  });

  await t.test('4. Product Categories Overview (GET /api/products/categories)', async () => {
    const res = await fetch(`${baseUrl}/api/products/categories`);
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.ok(Array.isArray(body.data));
    assert.ok(body.data.length >= 4);

    const firstAidCategory = body.data.find((c: any) => c.id === 'first_aid');
    assert.ok(firstAidCategory);
    assert.equal(firstAidCategory.name, 'First Aid');
    assert.ok(firstAidCategory.productCount > 0);
    assert.ok(firstAidCategory.emoji);
  });

  await t.test('5. Featured Endpoint (GET /api/products/featured)', async () => {
    const res = await fetch(`${baseUrl}/api/products/featured?limit=4`);
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.ok(Array.isArray(body.data));
    assert.ok(body.data.length <= 4);
    assert.ok(body.data.every((p: any) => p.isFeatured));
  });

  await t.test('6. Product Details (GET /api/products/:id)', async () => {
    assert.ok(sampleProductId, 'Sample product ID should have been populated');

    const res = await fetch(`${baseUrl}/api/products/${sampleProductId}`);
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.equal(body.data.id, sampleProductId);
    assert.ok(body.data.name);
    assert.ok(body.data.affiliateUrl);
    assert.ok(Array.isArray(body.data.relatedProducts));
  });

  await t.test('7. Product Details 404 for invalid ID', async () => {
    const res = await fetch(`${baseUrl}/api/products/non_existent_id_9999`);
    assert.equal(res.status, 404);

    const body = await res.json() as any;
    assert.equal(body.success, false);
    assert.ok(body.message.includes('not found'));
  });

  await t.test('8. Affiliate Referral Click Tracking (POST /api/products/:id/click)', async () => {
    const res = await fetch(`${baseUrl}/api/products/${sampleProductId}/click`, {
      method: 'POST'
    });
    assert.equal(res.status, 200);

    const body = await res.json() as any;
    assert.equal(body.success, true);
    assert.equal(body.data.productId, sampleProductId);
    assert.ok(body.data.affiliateUrl);
    assert.ok(body.data.clickCount >= 1);
  });

  await t.test('9. Validation Error on Product Creation (POST /api/products)', async () => {
    const incompletePayload = {
      name: 'Partial Safety Product'
      // Missing description, imageUrl, price, category, storeName, affiliateUrl
    };

    const res = await fetch(`${baseUrl}/api/products`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(incompletePayload)
    });

    assert.equal(res.status, 400);
    const body = await res.json() as any;
    assert.equal(body.success, false);
    assert.ok(body.message.includes('Validation failed'));
  });

  await t.test('10. Create, Update and Delete Affiliate Product Lifecycle', async () => {
    // A. Create
    const createPayload = {
      name: 'Automated Test Emergency Whistle',
      description: 'Ultralight dual-tone signaling whistle for emergency search and rescue.',
      imageUrl: 'https://images.unsplash.com/photo-test',
      price: 299,
      category: 'Personal Safety',
      storeName: 'Decathlon India',
      affiliateUrl: 'https://www.decathlon.in/p/test-whistle?aff_id=samraksha_test',
      isFeatured: false
    };

    const createRes = await fetch(`${baseUrl}/api/products`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(createPayload)
    });

    assert.equal(createRes.status, 201);
    const createBody = await createRes.json() as any;
    assert.equal(createBody.success, true);
    const newId = createBody.data.id;
    assert.ok(newId);
    assert.equal(createBody.data.name, createPayload.name);
    assert.equal(createBody.data.storeName, 'Decathlon India');

    // B. Update
    const updateRes = await fetch(`${baseUrl}/api/products/${newId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        price: 249,
        isFeatured: true
      })
    });
    assert.equal(updateRes.status, 200);
    const updateBody = await updateRes.json() as any;
    assert.equal(updateBody.data.price, 249);
    assert.equal(updateBody.data.isFeatured, true);

    // C. Delete
    const deleteRes = await fetch(`${baseUrl}/api/products/${newId}`, {
      method: 'DELETE'
    });
    assert.equal(deleteRes.status, 200);

    // D. Confirm 404 after deletion
    const fetchRes = await fetch(`${baseUrl}/api/products/${newId}`);
    assert.equal(fetchRes.status, 404);
  });
});
