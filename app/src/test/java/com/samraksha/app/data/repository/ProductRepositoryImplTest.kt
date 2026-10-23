package com.samraksha.app.data.repository

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.CategoryDto
import com.samraksha.app.data.model.ProductClickResponseData
import com.samraksha.app.data.model.ProductDto
import com.samraksha.app.data.model.ProductListResponseData
import com.samraksha.app.data.remote.ProductApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRepositoryImplTest {

    private lateinit var fakeApiService: FakeProductApiService
    private lateinit var repository: ProductRepositoryImpl

    @Before
    fun setUp() {
        fakeApiService = FakeProductApiService()
        repository = ProductRepositoryImpl(fakeApiService)
    }

    @Test
    fun getProducts_success_returnsMappedProducts() = runTest {
        val result = repository.getProducts(category = "first_aid", search = null)

        assertTrue(result.isSuccess)
        val products = result.getOrThrow()
        assertEquals(1, products.size)
        assertEquals("prod_101", products[0].id)
        assertEquals("Tourniquet Kit", products[0].name)
        assertEquals("first_aid", products[0].categoryId)
        assertEquals("🧰", products[0].imageEmoji)
    }

    @Test
    fun getProducts_httpError_returnsFailure() = runTest {
        fakeApiService.shouldReturnError = true

        val result = repository.getProducts(category = null, search = null)

        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getCategories_success_returnsMappedCategories() = runTest {
        val result = repository.getCategories()

        assertTrue(result.isSuccess)
        val categories = result.getOrThrow()
        assertEquals(2, categories.size)
        assertEquals("first_aid", categories[0].id)
        assertEquals("First Aid", categories[0].name)
    }

    @Test
    fun getProductDetails_success_returnsProduct() = runTest {
        val result = repository.getProductDetails("prod_101")

        assertTrue(result.isSuccess)
        val product = result.getOrThrow()
        assertEquals("prod_101", product.id)
        assertEquals("Tourniquet Kit", product.name)
    }

    @Test
    fun trackProductClick_success_returnsAffiliateUrl() = runTest {
        val result = repository.trackProductClick("prod_101")

        assertTrue(result.isSuccess)
        val url = result.getOrThrow()
        assertEquals("https://affiliate.example.com/click", url)
    }
}

class FakeProductApiService : ProductApiService {
    var shouldReturnError: Boolean = false

    private val sampleProduct = ProductDto(
        id = "prod_101",
        name = "Tourniquet Kit",
        description = "Combat application tourniquet",
        imageUrl = "https://example.com/image.jpg",
        price = 799.0,
        originalPrice = 999.0,
        category = "First Aid",
        categoryId = "first_aid",
        storeName = "Apollo Pharmacy",
        affiliateUrl = "https://affiliate.example.com/click",
        isFeatured = true,
        rating = 4.8f,
        reviewCount = 150
    )

    override suspend fun getProducts(
        category: String?,
        featured: Boolean?,
        search: String?,
        sortBy: String?,
        page: Int?,
        limit: Int?
    ): Response<ApiResponse<ProductListResponseData>> {
        if (shouldReturnError) {
            val errorResponse = "{\"success\":false,\"message\":\"Internal Server Error\"}"
            return Response.error(500, errorResponse.toResponseBody("application/json".toMediaTypeOrNull()))
        }

        return Response.success(
            ApiResponse(
                success = true,
                message = "Products retrieved",
                data = ProductListResponseData(
                    products = listOf(sampleProduct),
                    total = 1,
                    page = 1,
                    limit = 50,
                    totalPages = 1,
                    hasMore = false
                )
            )
        )
    }

    override suspend fun getCategories(): Response<ApiResponse<List<CategoryDto>>> {
        if (shouldReturnError) {
            val errorResponse = "{\"success\":false,\"message\":\"Failed to fetch categories\"}"
            return Response.error(500, errorResponse.toResponseBody("application/json".toMediaTypeOrNull()))
        }

        val categories = listOf(
            CategoryDto(id = "first_aid", name = "First Aid", emoji = "🧰", productCount = 5),
            CategoryDto(id = "personal_safety", name = "Personal Safety", emoji = "🛡️", productCount = 7)
        )
        return Response.success(ApiResponse(success = true, message = "Categories retrieved", data = categories))
    }

    override suspend fun getFeaturedProducts(limit: Int?): Response<ApiResponse<List<ProductDto>>> {
        return Response.success(ApiResponse(success = true, message = "Featured products", data = listOf(sampleProduct)))
    }

    override suspend fun getProductById(id: String): Response<ApiResponse<ProductDto>> {
        if (shouldReturnError) {
            val errorResponse = "{\"success\":false,\"message\":\"Product not found\"}"
            return Response.error(404, errorResponse.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(ApiResponse(success = true, message = "Product details", data = sampleProduct))
    }

    override suspend fun trackClick(id: String): Response<ApiResponse<ProductClickResponseData>> {
        if (shouldReturnError) {
            val errorResponse = "{\"success\":false,\"message\":\"Error\"}"
            return Response.error(500, errorResponse.toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(
            ApiResponse(
                success = true,
                message = "Click tracked",
                data = ProductClickResponseData(
                    productId = id,
                    storeName = "Apollo Pharmacy",
                    affiliateUrl = "https://affiliate.example.com/click",
                    clickCount = 1
                )
            )
        )
    }
}
