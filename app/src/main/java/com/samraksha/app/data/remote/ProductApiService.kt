package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.CategoryDto
import com.samraksha.app.data.model.ProductClickResponseData
import com.samraksha.app.data.model.ProductDto
import com.samraksha.app.data.model.ProductListResponseData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for SAMRAKSHA Safety Affiliate Product endpoints.
 */
interface ProductApiService {

    /**
     * Retrieve products with optional category, search, and pagination parameters.
     */
    @GET("api/products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<ProductListResponseData>>

    /**
     * Retrieve safety product categories with item count summaries.
     */
    @GET("api/products/categories")
    suspend fun getCategories(): Response<ApiResponse<List<CategoryDto>>>

    /**
     * Retrieve curated featured products.
     */
    @GET("api/products/featured")
    suspend fun getFeaturedProducts(
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<List<ProductDto>>>

    /**
     * Retrieve detailed product information by ID.
     */
    @GET("api/products/{id}")
    suspend fun getProductById(
        @Path("id") id: String
    ): Response<ApiResponse<ProductDto>>

    /**
     * Register outbound affiliate link click and receive destination URL.
     */
    @POST("api/products/{id}/click")
    suspend fun trackClick(
        @Path("id") id: String
    ): Response<ApiResponse<ProductClickResponseData>>
}
