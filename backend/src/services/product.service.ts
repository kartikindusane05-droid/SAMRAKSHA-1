import { randomUUID } from 'crypto';
import {
  Product,
  CreateProductDto,
  UpdateProductDto,
  ProductQueryParams,
  CategorySummary,
  PaginatedProductsResponse
} from '../models/product.model';
import { productRepository } from '../db/repositories/product.repository';
import { logger } from '../utils/logger';

class ProductService {
  private fallbackProducts: Map<string, Product> = new Map();

  constructor() {
    this.seedFallbackProducts();
  }

  private seedFallbackProducts(): void {
    const now = new Date().toISOString();
    const seeds: (Omit<Product, 'createdAt' | 'updatedAt' | 'clickCount'>)[] = [
      {
        id: 'prod_001_sirona_spray',
        name: 'Sirona Impower Pepper Spray for Women',
        description: 'Compact, pocket-size self-defence pepper spray with strong OC formula for instant deterrent protection and quick-reach thumb safety.',
        imageUrl: 'https://m.media-amazon.com/images/I/61bQnBo4j4L._SL1500_.jpg',
        price: 199,
        originalPrice: 249,
        currency: 'INR',
        category: 'Personal Safety',
        categoryId: 'personal_safety',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/02M1hVHn',
        isFeatured: true,
        rating: 4.8,
        reviewCount: 1420,
        badge: 'Bestseller',
        inStock: true
      },
      {
        id: 'prod_002_eveready_dl102',
        name: 'EVEREADY Siren DL102 Rechargeable LED Torch',
        description: 'Rechargeable bright LED emergency torch integrated with an audible safety alarm siren and durable lightweight handheld body.',
        imageUrl: 'https://m.media-amazon.com/images/I/618nupcI3IL._SL1500_.jpg',
        price: 169,
        originalPrice: 220,
        currency: 'INR',
        category: 'Emergency Lighting',
        categoryId: 'lighting',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/0ijyoq4c',
        isFeatured: true,
        rating: 4.7,
        reviewCount: 860,
        badge: 'Featured',
        inStock: true
      },
      {
        id: 'prod_003_shadow_alarm',
        name: 'Shadow Securitronics 130dB Personal Safety Alarm with LED',
        description: 'High-decibel 130dB emergency keychain siren with built-in LED strobe flashlight and swift pull-pin activation for immediate distress signalling.',
        imageUrl: 'https://m.media-amazon.com/images/I/71mX5ZidbbL._SL1500_.jpg',
        price: 437,
        originalPrice: 699,
        currency: 'INR',
        category: 'Personal Safety',
        categoryId: 'personal_safety',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/0c0MxMd6',
        isFeatured: true,
        rating: 4.6,
        reviewCount: 950,
        badge: 'Recommended',
        inStock: true
      },
      {
        id: 'prod_004_siago_hammer',
        name: 'SIAGO Car Safety Hammer SIA478',
        description: 'Multi-function automotive emergency escape tool with hardened steel window glass breaker and integrated seatbelt cutter for vehicle rescue.',
        imageUrl: 'https://m.media-amazon.com/images/I/71sqm4fWbtL._SL1500_.jpg',
        price: 399,
        originalPrice: 599,
        currency: 'INR',
        category: 'Travel Safety',
        categoryId: 'travel_safety',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/04zxGvUA',
        isFeatured: false,
        rating: 4.5,
        reviewCount: 410,
        badge: 'Vehicle Essential',
        inStock: true
      },
      {
        id: 'prod_005_dwc_alarm',
        name: 'Devil Will Cry Defence Emergency Alarm Keychain',
        description: 'Compact, ultra-loud personal alarm keychain designed for women, students, and seniors with convenient bag clip and quick-pull distress pin.',
        imageUrl: 'https://m.media-amazon.com/images/I/81GkOfiJQCL._SL1500_.jpg',
        price: 384,
        originalPrice: 499,
        currency: 'INR',
        category: 'Personal Safety',
        categoryId: 'personal_safety',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/06q8klje',
        isFeatured: false,
        rating: 4.4,
        reviewCount: 520,
        inStock: true
      },
      {
        id: 'prod_006_anti_choking',
        name: 'Portable Anti Choking Device — One-Button Operation, 3 Masks Included',
        description: 'Emergency choking rescue suction device featuring intuitive one-button operation, dual one-way valves, and 3 interchangeable masks for all ages.',
        imageUrl: 'https://m.media-amazon.com/images/I/71VxKz9t0VL._SL1500_.jpg',
        price: 1399,
        originalPrice: 1999,
        currency: 'INR',
        category: 'First Aid',
        categoryId: 'first_aid',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/0h7tLqK9',
        isFeatured: true,
        rating: 4.9,
        reviewCount: 310,
        badge: 'Life Saving',
        inStock: true
      },
      {
        id: 'prod_007_dwc_spray',
        name: 'Devil Will Cry Pepper Spray for Women Safety',
        description: 'Long-range stream defence pepper spray with 10–12 ft accurate reach, safety flip-top dispenser, and non-lethal maximum deterrence formula.',
        imageUrl: 'https://m.media-amazon.com/images/I/71UmiAE0q8L._SL1403_.jpg',
        price: 207,
        originalPrice: 299,
        currency: 'INR',
        category: 'Personal Safety',
        categoryId: 'personal_safety',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/06qJCOU2',
        isFeatured: false,
        rating: 4.6,
        reviewCount: 680,
        inStock: true
      },
      {
        id: 'prod_008_eveready_dl101',
        name: 'EVEREADY DL 101 Minimight Rechargeable Emergency Torch',
        description: 'Pocket-sized rechargeable emergency LED torch with long battery backup, slide power switch, and convenient foldaway charging plug.',
        imageUrl: 'https://m.media-amazon.com/images/I/71+pBhQRdwL._SL1500_.jpg',
        price: 139,
        originalPrice: 180,
        currency: 'INR',
        category: 'Emergency Lighting',
        categoryId: 'lighting',
        storeName: 'Amazon India',
        affiliateUrl: 'https://amzn.in/d/0iopyunM',
        isFeatured: false,
        rating: 4.5,
        reviewCount: 1140,
        badge: 'Budget Pick',
        inStock: true
      }
    ];

    seeds.forEach((item) => {
      this.fallbackProducts.set(item.id, {
        ...item,
        clickCount: 0,
        createdAt: now,
        updatedAt: now
      });
    });
  }

  public async listProducts(params: ProductQueryParams): Promise<PaginatedProductsResponse> {
    try {
      const result = await productRepository.findAll(params);
      if (result.total > 0) return result;
    } catch (err: any) {
      logger.warn(`DB listProducts failed (${err.message}). Using fallback.`);
    }

    let items = Array.from(this.fallbackProducts.values());

    if (params.category) {
      const cat = params.category.toLowerCase();
      items = items.filter(
        (p) => p.category.toLowerCase().includes(cat) || p.categoryId === cat
      );
    }

    if (params.featured !== undefined) {
      const isFeat = String(params.featured) === 'true';
      items = items.filter((p) => p.isFeatured === isFeat);
    }

    if (params.search) {
      const s = params.search.toLowerCase();
      items = items.filter(
        (p) => p.name.toLowerCase().includes(s) || p.description.toLowerCase().includes(s)
      );
    }

    const page = Math.max(1, params.page || 1);
    const limit = Math.max(1, Math.min(100, params.limit || 20));
    const total = items.length;
    const totalPages = Math.ceil(total / limit) || 1;
    const products = items.slice((page - 1) * limit, page * limit);

    return {
      products,
      total,
      page,
      limit,
      totalPages,
      hasMore: page < totalPages
    };
  }

  public async getCategories(): Promise<CategorySummary[]> {
    try {
      const categories = await productRepository.getCategories();
      if (categories.length > 0) return categories;
    } catch (err: any) {
      logger.warn(`DB getCategories failed (${err.message}). Using fallback.`);
    }

    return [
      { id: 'first_aid', name: 'First Aid', emoji: '🩹', productCount: 3, iconName: 'first_aid' },
      { id: 'personal_safety', name: 'Personal Safety', emoji: '🚨', productCount: 2, iconName: 'personal_safety' },
      { id: 'fire_safety', name: 'Fire Safety', emoji: '🧯', productCount: 1, iconName: 'fire_safety' },
      { id: 'lighting', name: 'Emergency Lighting', emoji: '🔦', productCount: 2, iconName: 'lighting' },
      { id: 'medical_wearables', name: 'Smart Wearables', emoji: '🩺', productCount: 2, iconName: 'medical_wearables' },
      { id: 'home_security', name: 'Home Security', emoji: '🔒', productCount: 2, iconName: 'home_security' }
    ];
  }

  public async getFeaturedProducts(limit: number = 6): Promise<Product[]> {
    try {
      const list = await productRepository.getFeatured(limit);
      if (list.length > 0) return list;
    } catch (err: any) {
      logger.warn(`DB getFeaturedProducts failed (${err.message}). Using fallback.`);
    }

    return Array.from(this.fallbackProducts.values())
      .filter((p) => p.isFeatured)
      .slice(0, limit);
  }

  public async getProductById(id: string): Promise<Product | null> {
    try {
      const p = await productRepository.findById(id);
      if (p) return p;
    } catch (err: any) {
      logger.warn(`DB getProductById failed (${err.message}). Using fallback.`);
    }
    return this.fallbackProducts.get(id) || null;
  }

  public async getRelatedProducts(productId: string, limit: number = 4): Promise<Product[]> {
    try {
      const list = await productRepository.getRelated(productId, limit);
      if (list.length > 0) return list;
    } catch (err: any) {
      logger.warn(`DB getRelatedProducts failed (${err.message}). Using fallback.`);
    }

    const current = this.fallbackProducts.get(productId);
    if (!current) return [];

    return Array.from(this.fallbackProducts.values())
      .filter((p) => p.id !== productId && p.category === current.category)
      .slice(0, limit);
  }

  public async trackClick(id: string): Promise<{ product: Product; redirectUrl: string } | null> {
    try {
      const res = await productRepository.incrementClick(id);
      if (res) return res;
    } catch (err: any) {
      logger.warn(`DB trackClick failed (${err.message}). Using fallback.`);
    }

    const product = this.fallbackProducts.get(id);
    if (!product) return null;

    product.clickCount += 1;
    product.updatedAt = new Date().toISOString();
    this.fallbackProducts.set(id, product);

    return {
      product,
      redirectUrl: product.affiliateUrl
    };
  }

  public async createProduct(dto: CreateProductDto): Promise<Product> {
    const id = 'prod_' + randomUUID().substring(0, 10);
    const now = new Date().toISOString();
    const categoryId =
      dto.categoryId ||
      dto.category.toLowerCase().replace(/[^a-z0-9]+/g, '_');

    const newProduct: Product = {
      id,
      name: dto.name,
      description: dto.description,
      imageUrl: dto.imageUrl,
      price: dto.price,
      originalPrice: dto.originalPrice,
      currency: dto.currency || 'INR',
      category: dto.category,
      categoryId,
      storeName: dto.storeName,
      affiliateUrl: dto.affiliateUrl,
      isFeatured: dto.isFeatured ?? false,
      rating: dto.rating ?? 5.0,
      reviewCount: dto.reviewCount ?? 0,
      badge: dto.badge,
      inStock: dto.inStock ?? true,
      clickCount: 0,
      createdAt: now,
      updatedAt: now
    };

    try {
      return await productRepository.create(newProduct);
    } catch (err: any) {
      logger.warn(`DB createProduct failed (${err.message}). Using fallback.`);
      this.fallbackProducts.set(id, newProduct);
      return newProduct;
    }
  }

  public async updateProduct(id: string, dto: UpdateProductDto): Promise<Product | null> {
    try {
      const updated = await productRepository.update(id, dto);
      if (updated) return updated;
    } catch (err: any) {
      logger.warn(`DB updateProduct failed (${err.message}). Using fallback.`);
    }

    const existing = this.fallbackProducts.get(id);
    if (!existing) return null;

    const updated: Product = {
      ...existing,
      ...dto,
      categoryId: dto.category
        ? dto.categoryId || dto.category.toLowerCase().replace(/[^a-z0-9]+/g, '_')
        : existing.categoryId,
      updatedAt: new Date().toISOString()
    };

    this.fallbackProducts.set(id, updated);
    return updated;
  }

  public async deleteProduct(id: string): Promise<boolean> {
    try {
      return await productRepository.delete(id);
    } catch (err: any) {
      logger.warn(`DB deleteProduct failed (${err.message}). Using fallback.`);
      return this.fallbackProducts.delete(id);
    }
  }
}

export const productService = new ProductService();
export default productService;
