package com.samraksha.app.domain.repository

import com.samraksha.app.ui.screens.store.StoreCategory
import com.samraksha.app.ui.screens.store.StoreProduct

/**
 * Repository interface for safety affiliate products and category discovery.
 */
interface ProductRepository {

    /**
     * Fetch products from backend, optionally filtered by category or search term.
     */
    suspend fun getProducts(
        category: String? = null,
        search: String? = null
    ): Result<List<StoreProduct>>

    /**
     * Fetch all available safety product categories with item counts.
     */
    suspend fun getCategories(): Result<List<StoreCategory>>

    /**
     * Fetch detailed product specifications by ID.
     */
    suspend fun getProductDetails(id: String): Result<StoreProduct>

    /**
     * Record an outbound click for affiliate attribution and obtain destination URL.
     */
    suspend fun trackProductClick(id: String): Result<String>
}
