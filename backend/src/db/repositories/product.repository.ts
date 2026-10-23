import { query } from '../index';
import {
  Product,
  ProductQueryParams,
  PaginatedProductsResponse,
  CategorySummary
} from '../../models/product.model';

export interface ProductRow {
  id: string;
  name: string;
  description: string;
  image_url: string;
  price: string | number;
  original_price: string | number | null;
  currency: string;
  category: string;
  category_id: string;
  store_name: string;
  affiliate_url: string;
  is_featured: boolean;
  rating: string | number | null;
  review_count: number | null;
  badge: string | null;
  in_stock: boolean;
  click_count: number;
  created_at: Date;
  updated_at: Date;
}

function mapRowToProduct(row: ProductRow): Product {
  return {
    id: row.id,
    name: row.name,
    description: row.description,
    imageUrl: row.image_url,
    price: Number(row.price),
    originalPrice: row.original_price != null ? Number(row.original_price) : undefined,
    currency: row.currency,
    category: row.category,
    categoryId: row.category_id,
    storeName: row.store_name,
    affiliateUrl: row.affiliate_url,
    isFeatured: Boolean(row.is_featured),
    rating: row.rating != null ? Number(row.rating) : 5.0,
    reviewCount: row.review_count ?? 0,
    badge: row.badge || undefined,
    inStock: Boolean(row.in_stock),
    clickCount: row.click_count ?? 0,
    createdAt: new Date(row.created_at).toISOString(),
    updatedAt: new Date(row.updated_at).toISOString()
  };
}

export class ProductRepository {
  async findAll(params: ProductQueryParams): Promise<PaginatedProductsResponse> {
    const page = Math.max(1, params.page || 1);
    const limit = Math.max(1, Math.min(100, params.limit || 20));
    const offset = (page - 1) * limit;

    let whereClauses: string[] = [];
    let queryParams: any[] = [];

    if (params.category) {
      queryParams.push(params.category.toLowerCase());
      whereClauses.push(
        `(LOWER(category_id) = $${queryParams.length} OR LOWER(category) LIKE '%' || $${queryParams.length} || '%')`
      );
    }

    if (params.featured !== undefined) {
      const isFeat = String(params.featured) === 'true';
      queryParams.push(isFeat);
      whereClauses.push(`is_featured = $${queryParams.length}`);
    }

    if (params.storeName) {
      queryParams.push(`%${params.storeName.toLowerCase()}%`);
      whereClauses.push(`LOWER(store_name) LIKE $${queryParams.length}`);
    }

    if (params.search) {
      queryParams.push(`%${params.search.toLowerCase()}%`);
      whereClauses.push(
        `(LOWER(name) LIKE $${queryParams.length} OR LOWER(description) LIKE $${queryParams.length})`
      );
    }

    if (params.minPrice !== undefined) {
      queryParams.push(params.minPrice);
      whereClauses.push(`price >= $${queryParams.length}`);
    }

    if (params.maxPrice !== undefined) {
      queryParams.push(params.maxPrice);
      whereClauses.push(`price <= $${queryParams.length}`);
    }

    const whereSql = whereClauses.length > 0 ? `WHERE ${whereClauses.join(' AND ')}` : '';

    // Total Count
    const countSql = `SELECT COUNT(*) AS total FROM products ${whereSql};`;
    const countRes = await query<{ total: string }>(countSql, queryParams);
    const total = parseInt(countRes.rows[0]?.total || '0', 10);

    // Sorting
    let orderBy = 'created_at DESC';
    if (params.sortBy === 'price_asc') orderBy = 'price ASC';
    else if (params.sortBy === 'price_desc') orderBy = 'price DESC';
    else if (params.sortBy === 'rating') orderBy = 'rating DESC';
    else if (params.sortBy === 'clicks') orderBy = 'click_count DESC';
    else if (params.sortBy === 'name') orderBy = 'name ASC';

    queryParams.push(limit, offset);
    const dataSql = `
      SELECT * FROM products 
      ${whereSql} 
      ORDER BY ${orderBy} 
      LIMIT $${queryParams.length - 1} OFFSET $${queryParams.length};
    `;

    const dataRes = await query<ProductRow>(dataSql, queryParams);
    const products = dataRes.rows.map(mapRowToProduct);
    const totalPages = Math.ceil(total / limit) || 1;

    return {
      products,
      total,
      page,
      limit,
      totalPages,
      hasMore: page < totalPages
    };
  }

  async findById(id: string): Promise<Product | null> {
    const text = `SELECT * FROM products WHERE id = $1 LIMIT 1;`;
    const res = await query<ProductRow>(text, [id]);
    return res.rows[0] ? mapRowToProduct(res.rows[0]) : null;
  }

  async getCategories(): Promise<CategorySummary[]> {
    const sql = `
      SELECT category_id, category, COUNT(*) as count 
      FROM products 
      GROUP BY category_id, category 
      ORDER BY count DESC;
    `;
    const res = await query<{ category_id: string; category: string; count: string }>(sql);

    const emojiMap: Record<string, string> = {
      first_aid: '🩹',
      personal_safety: '🚨',
      fire_safety: '🧯',
      lighting: '🔦',
      medical_wearables: '🩺',
      home_security: '🔒',
      travel_safety: '🚗'
    };

    return res.rows.map((r) => ({
      id: r.category_id,
      name: r.category,
      emoji: emojiMap[r.category_id] || '🛡️',
      productCount: parseInt(r.count, 10),
      iconName: r.category_id
    }));
  }

  async getFeatured(limit: number = 6): Promise<Product[]> {
    const text = `
      SELECT * FROM products 
      WHERE is_featured = TRUE 
      ORDER BY rating DESC 
      LIMIT $1;
    `;
    const res = await query<ProductRow>(text, [limit]);
    return res.rows.map(mapRowToProduct);
  }

  async getRelated(productId: string, limit: number = 4): Promise<Product[]> {
    const current = await this.findById(productId);
    if (!current) return [];

    const text = `
      SELECT * FROM products 
      WHERE id != $1 AND category_id = $2 
      ORDER BY rating DESC 
      LIMIT $3;
    `;
    const res = await query<ProductRow>(text, [productId, current.categoryId, limit]);
    return res.rows.map(mapRowToProduct);
  }

  async incrementClick(id: string): Promise<{ product: Product; redirectUrl: string } | null> {
    const text = `
      UPDATE products 
      SET click_count = click_count + 1, updated_at = NOW() 
      WHERE id = $1 
      RETURNING *;
    `;
    const res = await query<ProductRow>(text, [id]);
    if (!res.rows[0]) return null;
    const product = mapRowToProduct(res.rows[0]);
    return { product, redirectUrl: product.affiliateUrl };
  }

  async create(p: Product): Promise<Product> {
    const text = `
      INSERT INTO products (
        id, name, description, image_url, price, original_price, currency,
        category, category_id, store_name, affiliate_url, is_featured, rating,
        review_count, badge, in_stock, click_count, created_at, updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19)
      ON CONFLICT (id) DO UPDATE SET
        name = EXCLUDED.name,
        description = EXCLUDED.description,
        image_url = EXCLUDED.image_url,
        price = EXCLUDED.price,
        original_price = EXCLUDED.original_price,
        category = EXCLUDED.category,
        category_id = EXCLUDED.category_id,
        store_name = EXCLUDED.store_name,
        affiliate_url = EXCLUDED.affiliate_url,
        is_featured = EXCLUDED.is_featured,
        rating = EXCLUDED.rating,
        review_count = EXCLUDED.review_count,
        badge = EXCLUDED.badge,
        in_stock = EXCLUDED.in_stock,
        updated_at = EXCLUDED.updated_at
      RETURNING *;
    `;
    const res = await query<ProductRow>(text, [
      p.id,
      p.name,
      p.description,
      p.imageUrl,
      p.price,
      p.originalPrice || null,
      p.currency || 'INR',
      p.category,
      p.categoryId || p.category.toLowerCase().replace(/[^a-z0-9]+/g, '_'),
      p.storeName,
      p.affiliateUrl,
      p.isFeatured ?? false,
      p.rating ?? 5.0,
      p.reviewCount ?? 0,
      p.badge || null,
      p.inStock ?? true,
      p.clickCount ?? 0,
      p.createdAt,
      p.updatedAt
    ]);
    return mapRowToProduct(res.rows[0]);
  }

  async update(id: string, updates: Partial<Product>): Promise<Product | null> {
    const existing = await this.findById(id);
    if (!existing) return null;

    const updated: Product = {
      ...existing,
      ...updates,
      updatedAt: new Date().toISOString()
    };
    return await this.create(updated);
  }

  async delete(id: string): Promise<boolean> {
    const text = `DELETE FROM products WHERE id = $1;`;
    const res = await query(text, [id]);
    return (res.rowCount ?? 0) > 0;
  }
}

export const productRepository = new ProductRepository();
