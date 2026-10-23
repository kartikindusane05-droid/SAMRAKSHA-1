package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for an affiliate product returned by the SAMRAKSHA backend API.
 */
data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("price") val price: Double,
    @SerializedName("originalPrice") val originalPrice: Double? = null,
    @SerializedName("currency") val currency: String? = "INR",
    @SerializedName("category") val category: String,
    @SerializedName("categoryId") val categoryId: String? = null,
    @SerializedName("storeName") val storeName: String,
    @SerializedName("affiliateUrl") val affiliateUrl: String,
    @SerializedName("isFeatured") val isFeatured: Boolean = false,
    @SerializedName("rating") val rating: Float? = 4.5f,
    @SerializedName("reviewCount") val reviewCount: Int? = 0,
    @SerializedName("badge") val badge: String? = null,
    @SerializedName("inStock") val inStock: Boolean? = true,
    @SerializedName("clickCount") val clickCount: Int? = 0,
    @SerializedName("relatedProducts") val relatedProducts: List<ProductDto>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

/**
 * Data envelope for paginated product list from GET /api/products.
 */
data class ProductListResponseData(
    @SerializedName("products") val products: List<ProductDto>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("hasMore") val hasMore: Boolean
)

/**
 * Category metadata summary from GET /api/products/categories.
 */
data class CategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("emoji") val emoji: String,
    @SerializedName("productCount") val productCount: Int,
    @SerializedName("iconName") val iconName: String? = null
)

/**
 * Response for outbound click recording from POST /api/products/:id/click.
 */
data class ProductClickResponseData(
    @SerializedName("productId") val productId: String,
    @SerializedName("storeName") val storeName: String,
    @SerializedName("affiliateUrl") val affiliateUrl: String,
    @SerializedName("clickCount") val clickCount: Int
)
