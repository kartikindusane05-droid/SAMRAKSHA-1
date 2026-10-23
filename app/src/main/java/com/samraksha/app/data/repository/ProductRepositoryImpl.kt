package com.samraksha.app.data.repository

import com.samraksha.app.data.remote.ProductApiService
import com.samraksha.app.domain.repository.ProductRepository
import com.samraksha.app.ui.screens.store.StoreCategory
import com.samraksha.app.ui.screens.store.StoreProduct
import com.samraksha.app.ui.screens.store.toStoreCategory
import com.samraksha.app.ui.screens.store.toStoreProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production implementation of [ProductRepository] connecting to the SAMRAKSHA backend product APIs.
 */
class ProductRepositoryImpl(
    private val apiService: ProductApiService
) : ProductRepository {

    override suspend fun getProducts(
        category: String?,
        search: String?
    ): Result<List<StoreProduct>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProducts(
                category = if (category.isNullOrBlank() || category == "all") null else category,
                search = if (search.isNullOrBlank()) null else search,
                limit = 50
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val dtoList = response.body()?.data?.products ?: emptyList()
                val products = dtoList.map { it.toStoreProduct() }
                Result.success(products)
            } else {
                val errorMsg = response.body()?.message
                    ?: response.errorBody()?.string()
                    ?: "Failed to load products (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<StoreCategory>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCategories()
            if (response.isSuccessful && response.body()?.success == true) {
                val dtoList = response.body()?.data ?: emptyList()
                val categories = dtoList.map { it.toStoreCategory() }
                Result.success(categories)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to load categories (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProductDetails(id: String): Result<StoreProduct> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProductById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()?.data
                if (dto != null) {
                    Result.success(dto.toStoreProduct())
                } else {
                    Result.failure(Exception("Product details unavailable"))
                }
            } else {
                val errorMsg = response.body()?.message ?: "Product not found (HTTP ${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trackProductClick(id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.trackClick(id)
            if (response.isSuccessful && response.body()?.success == true) {
                val url = response.body()?.data?.affiliateUrl ?: ""
                Result.success(url)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to register affiliate click"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
