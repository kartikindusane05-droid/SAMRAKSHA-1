import { Router } from 'express';
import productController from '../controllers/product.controller';

const router = Router();

// ── Specific Routes (must precede /:id) ───────────────────
// GET /api/products/categories - List available product categories with summary counts
router.get('/categories', (req, res) => productController.getCategories(req, res));

// GET /api/products/featured - List curated featured affiliate products
router.get('/featured', (req, res) => productController.getFeaturedProducts(req, res));

// ── Root Collection Routes ───────────────────────────────
// GET /api/products - List products (category filtering, search, pagination, sorting)
router.get('/', (req, res) => productController.getProducts(req, res));

// POST /api/products - Create new product
router.post('/', (req, res) => productController.createProduct(req, res));

// ── Parameterized Item Routes ─────────────────────────────
// GET /api/products/:id - Retrieve product details and related recommendations
router.get('/:id', (req, res) => productController.getProductById(req, res));

// POST /api/products/:id/click - Track outbound affiliate referral click
router.post('/:id/click', (req, res) => productController.trackClick(req, res));

// PUT /api/products/:id - Update product
router.put('/:id', (req, res) => productController.updateProduct(req, res));

// DELETE /api/products/:id - Delete product
router.delete('/:id', (req, res) => productController.deleteProduct(req, res));

export default router;
