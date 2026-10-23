export interface Product {
  id: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  originalPrice?: number;
  currency: string;
  category: string;
  categoryId?: string;
  storeName: string;
  affiliateUrl: string;
  isFeatured: boolean;
  rating?: number;
  reviewCount?: number;
  badge?: string;
  inStock: boolean;
  clickCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductDto {
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  originalPrice?: number;
  currency?: string;
  category: string;
  categoryId?: string;
  storeName: string;
  affiliateUrl: string;
  isFeatured?: boolean;
  rating?: number;
  reviewCount?: number;
  badge?: string;
  inStock?: boolean;
}

export interface UpdateProductDto {
  name?: string;
  description?: string;
  imageUrl?: string;
  price?: number;
  originalPrice?: number;
  currency?: string;
  category?: string;
  categoryId?: string;
  storeName?: string;
  affiliateUrl?: string;
  isFeatured?: boolean;
  rating?: number;
  reviewCount?: number;
  badge?: string;
  inStock?: boolean;
}

export interface ProductQueryParams {
  category?: string;
  featured?: boolean | string;
  storeName?: string;
  search?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: 'price_asc' | 'price_desc' | 'rating' | 'newest' | 'clicks' | 'name';
  page?: number;
  limit?: number;
}

export interface CategorySummary {
  id: string;
  name: string;
  emoji: string;
  productCount: number;
  iconName?: string;
}

export interface PaginatedProductsResponse {
  products: Product[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
  hasMore: boolean;
}
