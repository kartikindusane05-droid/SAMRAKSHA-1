import { Request, Response } from 'express';
import productService from '../services/product.service';
import { sendSuccess, sendError } from '../utils/response';
import { CreateProductDto, UpdateProductDto, ProductQueryParams } from '../models/product.model';

export class ProductController {
  /**
   * GET /api/products
   * List products with category filtering, search, pagination, and sorting
   */
  public async getProducts(req: Request, res: Response): Promise<Response> {
    try {
      const {
        category,
        featured,
        storeName,
        search,
        minPrice,
        maxPrice,
        sortBy,
        page,
        limit
      } = req.query;

      const queryParams: ProductQueryParams = {
        category: typeof category === 'string' ? category : undefined,
        featured: typeof featured === 'string' ? featured : undefined,
        storeName: typeof storeName === 'string' ? storeName : undefined,
        search: typeof search === 'string' ? search : undefined,
        minPrice: minPrice !== undefined ? Number(minPrice) : undefined,
        maxPrice: maxPrice !== undefined ? Number(maxPrice) : undefined,
        sortBy: typeof sortBy === 'string' ? (sortBy as any) : undefined,
        page: page !== undefined ? Number(page) : 1,
        limit: limit !== undefined ? Number(limit) : 20
      };

      const result = await productService.listProducts(queryParams);
      return sendSuccess(res, 'Products retrieved successfully', result);
    } catch (error: any) {
      return sendError(res, 'Failed to fetch products', 500, error.message);
    }
  }

  /**
   * GET /api/products/categories
   * Return category list with item counts and emoji / icon metadata
   */
  public async getCategories(req: Request, res: Response): Promise<Response> {
    try {
      const categories = await productService.getCategories();
      return sendSuccess(res, 'Product categories retrieved successfully', categories);
    } catch (error: any) {
      return sendError(res, 'Failed to fetch categories', 500, error.message);
    }
  }

  /**
   * GET /api/products/featured
   * Retrieve highlighted featured affiliate products
   */
  public async getFeaturedProducts(req: Request, res: Response): Promise<Response> {
    try {
      const limit = req.query.limit ? Number(req.query.limit) : 6;
      const featured = await productService.getFeaturedProducts(limit);
      return sendSuccess(res, 'Featured products retrieved successfully', featured);
    } catch (error: any) {
      return sendError(res, 'Failed to fetch featured products', 500, error.message);
    }
  }

  /**
   * GET /api/products/:id
   * Retrieve single product details and related recommendations
   */
  public async getProductById(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const product = await productService.getProductById(id);

      if (!product) {
        return sendError(res, `Product with ID '${id}' not found`, 404);
      }

      const relatedProducts = await productService.getRelatedProducts(id, 4);

      return sendSuccess(res, 'Product details retrieved successfully', {
        ...product,
        relatedProducts
      });
    } catch (error: any) {
      return sendError(res, 'Failed to fetch product details', 500, error.message);
    }
  }

  /**
   * POST /api/products/:id/click
   * Record affiliate outbound click and return the target affiliate URL
   */
  public async trackClick(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const result = await productService.trackClick(id);

      if (!result) {
        return sendError(res, `Product with ID '${id}' not found`, 404);
      }

      return sendSuccess(res, 'Affiliate outbound click registered', {
        productId: result.product.id,
        storeName: result.product.storeName,
        affiliateUrl: result.redirectUrl,
        clickCount: result.product.clickCount
      });
    } catch (error: any) {
      return sendError(res, 'Failed to process affiliate click', 500, error.message);
    }
  }

  /**
   * POST /api/products
   * Create a new affiliate product
   */
  public async createProduct(req: Request, res: Response): Promise<Response> {
    try {
      const {
        name,
        description,
        imageUrl,
        price,
        category,
        storeName,
        affiliateUrl,
        isFeatured,
        originalPrice,
        currency,
        rating,
        reviewCount,
        badge,
        inStock
      } = req.body;

      // Validate mandatory fields
      const missingFields: string[] = [];
      if (!name) missingFields.push('name');
      if (!description) missingFields.push('description');
      if (!imageUrl) missingFields.push('imageUrl');
      if (price === undefined || typeof price !== 'number') missingFields.push('price (number)');
      if (!category) missingFields.push('category');
      if (!storeName) missingFields.push('storeName');
      if (!affiliateUrl) missingFields.push('affiliateUrl');

      if (missingFields.length > 0) {
        return sendError(
          res,
          `Validation failed. Missing required fields: ${missingFields.join(', ')}`,
          400
        );
      }

      const dto: CreateProductDto = {
        name,
        description,
        imageUrl,
        price,
        originalPrice,
        currency,
        category,
        storeName,
        affiliateUrl,
        isFeatured: Boolean(isFeatured),
        rating: rating !== undefined ? Number(rating) : undefined,
        reviewCount: reviewCount !== undefined ? Number(reviewCount) : undefined,
        badge,
        inStock
      };

      const product = await productService.createProduct(dto);
      return sendSuccess(res, 'Product created successfully', product, 201);
    } catch (error: any) {
      return sendError(res, 'Failed to create product', 500, error.message);
    }
  }

  /**
   * PUT /api/products/:id
   * Update an existing product
   */
  public async updateProduct(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const updateDto: UpdateProductDto = req.body;

      const updated = await productService.updateProduct(id, updateDto);
      if (!updated) {
        return sendError(res, `Product with ID '${id}' not found`, 404);
      }

      return sendSuccess(res, 'Product updated successfully', updated);
    } catch (error: any) {
      return sendError(res, 'Failed to update product', 500, error.message);
    }
  }

  /**
   * DELETE /api/products/:id
   * Remove a product from the catalog
   */
  public async deleteProduct(req: Request, res: Response): Promise<Response> {
    try {
      const { id } = req.params;
      const deleted = await productService.deleteProduct(id);

      if (!deleted) {
        return sendError(res, `Product with ID '${id}' not found`, 404);
      }

      return sendSuccess(res, 'Product deleted successfully', { id });
    } catch (error: any) {
      return sendError(res, 'Failed to delete product', 500, error.message);
    }
  }
}

export const productController = new ProductController();
export default productController;
