package com.samraksha.app.ui.screens.store

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import com.samraksha.app.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafetyStoreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeRepository: FakeProductRepository
    private lateinit var viewModel: SafetyStoreViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeProductRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SafetyStoreViewModel {
        return SafetyStoreViewModel(fakeRepository)
    }

    @Test
    fun loadData_initialSuccess_populatesProductsAndCategories() = testScope.runTest {
        viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertEquals(3, state.products.size)
        assertEquals(2, state.categories.size)
        assertFalse(state.isCatalogEmpty)
    }

    @Test
    fun selectCategory_filtersProductsProperly() = testScope.runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Select category
        viewModel.selectCategory("personal_safety")
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("personal_safety", state.selectedCategoryId)
        assertEquals(2, state.displayedProducts.size)
        assertEquals("prod_1", state.displayedProducts[0].id)
        assertEquals("prod_2", state.displayedProducts[1].id)

        // Deselect category (toggle back to all)
        viewModel.selectCategory("personal_safety")
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNull(state.selectedCategoryId)
        assertEquals(3, state.displayedProducts.size)
    }

    @Test
    fun updateSearchQuery_filtersDisplayedProductsInPlace() = testScope.runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("Pepper")

        val state = viewModel.uiState.value
        assertEquals("Pepper", state.searchQuery)
        assertEquals(1, state.displayedProducts.size)
        assertEquals("Pepper Spray Quick Key", state.displayedProducts[0].name)
    }

    @Test
    fun loadData_failure_showsErrorStateAndRetryWorks() = testScope.runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureError = "Network connection timed out"

        viewModel = createViewModel()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network connection timed out", state.errorMessage)
        assertTrue(state.products.isEmpty())

        // Fix error and retry
        fakeRepository.shouldFail = false
        viewModel.retry()

        advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(3, state.products.size)
    }

    @Test
    fun emptyState_catalogEmptyWhenNoProductsMatch() = testScope.runTest {
        fakeRepository.products = emptyList()

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.isCatalogEmpty)
    }

    @Test
    fun selectProductForDetails_loadsDeepDetails() = testScope.runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val product = viewModel.uiState.value.products.first()
        viewModel.selectProductForDetails(product)

        // Initially product is set and loading details
        assertNotNull(viewModel.uiState.value.selectedProductDetails)

        advanceUntilIdle()

        val detailedState = viewModel.uiState.value
        assertFalse(detailedState.isLoadingDetails)
        assertNotNull(detailedState.selectedProductDetails)
        assertEquals("prod_1", detailedState.selectedProductDetails?.id)
        assertEquals("Detailed full description for prod_1", detailedState.selectedProductDetails?.description)

        // Dismiss
        viewModel.dismissProductDetails()
        assertNull(viewModel.uiState.value.selectedProductDetails)
    }

    @Test
    fun submitSearch_triggersBackendProductLoad() = testScope.runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("Flashlight")
        viewModel.submitSearch()

        advanceUntilIdle()

        assertEquals("Flashlight", fakeRepository.lastSearchParam)
    }
}

/**
 * Fake implementation of [ProductRepository] for testing.
 */
class FakeProductRepository : ProductRepository {

    var shouldFail: Boolean = false
    var failureError: String = "Repository error"
    var lastSearchParam: String? = null
    var lastCategoryParam: String? = null
    var trackedClicks = mutableListOf<String>()

    var categories: List<StoreCategory> = listOf(
        StoreCategory(
            id = "personal_safety",
            name = "Personal Safety",
            icon = Icons.Default.Shield,
            productCount = 1,
            emoji = "🛡️"
        ),
        StoreCategory(
            id = "first_aid",
            name = "First Aid",
            icon = Icons.Default.Shield,
            productCount = 2,
            emoji = "🧰"
        )
    )

    var products: List<StoreProduct> = listOf(
        StoreProduct(
            id = "prod_1",
            name = "130dB Personal Alarm Siren",
            shortDescription = "Loud alarm with strobe light",
            price = 899.0,
            originalPrice = 1299.0,
            categoryId = "personal_safety",
            imageEmoji = "🛡️",
            storeName = "Amazon India",
            affiliateUrl = "https://www.amazon.in/dp/sample1",
            rating = 4.7f,
            reviewCount = 240
        ),
        StoreProduct(
            id = "prod_2",
            name = "Pepper Spray Quick Key",
            shortDescription = "Max strength self defense pepper spray",
            price = 449.0,
            originalPrice = 599.0,
            categoryId = "personal_safety",
            imageEmoji = "🛡️",
            storeName = "Flipkart",
            affiliateUrl = "https://www.flipkart.com/sample2",
            rating = 4.6f,
            reviewCount = 512
        ),
        StoreProduct(
            id = "prod_3",
            name = "Comprehensive 120-Piece Trauma First Aid Kit",
            shortDescription = "Hospital grade first aid supplies",
            price = 1499.0,
            originalPrice = 2199.0,
            categoryId = "first_aid",
            imageEmoji = "🧰",
            storeName = "Apollo Pharmacy",
            affiliateUrl = "https://www.apollopharmacy.in/sample3",
            rating = 4.9f,
            reviewCount = 120
        )
    )

    override suspend fun getProducts(category: String?, search: String?): Result<List<StoreProduct>> {
        if (shouldFail) return Result.failure(Exception(failureError))
        lastCategoryParam = category
        lastSearchParam = search

        var filtered = products
        if (!category.isNullOrBlank() && category != "all") {
            filtered = filtered.filter { it.categoryId.equals(category, ignoreCase = true) }
        }
        if (!search.isNullOrBlank()) {
            val q = search.trim().lowercase()
            filtered = filtered.filter { it.name.lowercase().contains(q) || it.shortDescription.lowercase().contains(q) }
        }
        return Result.success(filtered)
    }

    override suspend fun getCategories(): Result<List<StoreCategory>> {
        if (shouldFail) return Result.failure(Exception(failureError))
        return Result.success(categories)
    }

    override suspend fun getProductDetails(id: String): Result<StoreProduct> {
        if (shouldFail) return Result.failure(Exception(failureError))
        val p = products.find { it.id == id }
            ?: return Result.failure(Exception("Product not found"))
        return Result.success(
            p.copy(
                description = "Detailed full description for $id"
            )
        )
    }

    override suspend fun trackProductClick(id: String): Result<String> {
        if (shouldFail) return Result.failure(Exception(failureError))
        trackedClicks.add(id)
        val p = products.find { it.id == id }
        return Result.success(p?.affiliateUrl ?: "https://partner.example.com")
    }
}
