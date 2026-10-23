package com.samraksha.app.ui.screens.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the SAMRAKSHA Safety Store screen.
 */
data class SafetyStoreUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val products: List<StoreProduct> = emptyList(),
    val categories: List<StoreCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val errorMessage: String? = null,
    val selectedProductDetails: StoreProduct? = null,
    val isLoadingDetails: Boolean = false
) {
    /**
     * Filtered products matching the current category and search query.
     */
    val displayedProducts: List<StoreProduct>
        get() {
            var result = products

            if (!selectedCategoryId.isNullOrBlank() && selectedCategoryId != "all") {
                result = result.filter {
                    it.categoryId.equals(selectedCategoryId, ignoreCase = true) ||
                    it.categoryName.equals(selectedCategoryId, ignoreCase = true)
                }
            }

            if (searchQuery.isNotBlank()) {
                val query = searchQuery.trim().lowercase()
                result = result.filter {
                    it.name.lowercase().contains(query) ||
                    it.description.lowercase().contains(query) ||
                    it.shortDescription.lowercase().contains(query) ||
                    it.storeName.lowercase().contains(query) ||
                    it.categoryName.lowercase().contains(query)
                }
            }

            return result
        }

    val isCatalogEmpty: Boolean
        get() = !isLoading && errorMessage == null && displayedProducts.isEmpty()
}

/**
 * ViewModel managing affiliate product discovery, category filtering, details modal, and external store routing.
 */
class SafetyStoreViewModel(
    private val productRepository: ProductRepository = SamrakshaApp.instance.productRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyStoreUiState(isLoading = true))
    val uiState: StateFlow<SafetyStoreUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads product catalog and categories from the backend.
     */
    fun loadData(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refresh,
                    isRefreshing = refresh,
                    errorMessage = null
                )
            }

            // 1. Fetch categories if needed
            val currentCategories = _uiState.value.categories
            val categories = if (currentCategories.isNotEmpty() && !refresh) {
                currentCategories
            } else {
                productRepository.getCategories().getOrDefault(currentCategories)
            }

            // 2. Fetch products
            val productsResult = productRepository.getProducts(
                category = _uiState.value.selectedCategoryId,
                search = if (_uiState.value.searchQuery.isNotBlank()) _uiState.value.searchQuery else null
            )

            productsResult.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            categories = categories,
                            products = items,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            categories = categories,
                            errorMessage = error.localizedMessage ?: "Unable to connect to SAMRAKSHA Safety Catalog"
                        )
                    }
                }
            )
        }
    }

    /**
     * Selects or unselects a category chip filter.
     */
    fun selectCategory(categoryId: String?) {
        val newCategory = if (_uiState.value.selectedCategoryId == categoryId) null else categoryId
        _uiState.update { it.copy(selectedCategoryId = newCategory) }
        loadData(refresh = false)
    }

    /**
     * Updates search query string.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Submits search to backend.
     */
    fun submitSearch() {
        loadData(refresh = false)
    }

    fun toggleSearch(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
        if (!active) {
            loadData(refresh = false)
        }
    }

    /**
     * Selects a product to view full details in the bottom sheet.
     */
    fun selectProductForDetails(product: StoreProduct) {
        _uiState.update { it.copy(selectedProductDetails = product, isLoadingDetails = true) }

        // Fetch deep details & related recommendations from backend
        viewModelScope.launch {
            val result = productRepository.getProductDetails(product.id)
            result.onSuccess { detailed ->
                _uiState.update {
                    if (it.selectedProductDetails?.id == product.id) {
                        it.copy(selectedProductDetails = detailed, isLoadingDetails = false)
                    } else it
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingDetails = false) }
            }
        }
    }

    /**
     * Closes the product details view.
     */
    fun dismissProductDetails() {
        _uiState.update { it.copy(selectedProductDetails = null) }
    }

    /**
     * Opens the product affiliate URL externally in browser or partner app.
     * Also records outbound referral click on backend.
     * Handles invalid URLs, missing browser handlers, and click-tracking failures gracefully.
     * Note: SAMRAKSHA does not conduct any checkout or collect payments.
     */
    fun openAffiliateProduct(context: Context, product: StoreProduct) {
        // Track outbound click on backend in background (gracefully ignore failure so it never blocks user navigation)
        viewModelScope.launch {
            try {
                productRepository.trackProductClick(product.id)
            } catch (_: Exception) {
                // Click tracking failure must not prevent opening the product URL
            }
        }

        val rawUrl = product.affiliateUrl.trim()
        if (rawUrl.isBlank()) {
            Toast.makeText(context, "Affiliate link is currently unavailable", Toast.LENGTH_SHORT).show()
            return
        }

        val formattedUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) &&
            !rawUrl.startsWith("https://", ignoreCase = true)
        ) {
            "https://$rawUrl"
        } else {
            rawUrl
        }

        try {
            val uri = Uri.parse(formattedUrl)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") {
                Toast.makeText(context, "Invalid product link format", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No web browser found to open link. Please install a browser.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open external browser for ${product.storeName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Retry loading catalog after an error.
     */
    fun retry() {
        loadData()
    }

    companion object {
        fun provideFactory(
            repository: ProductRepository = SamrakshaApp.instance.productRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SafetyStoreViewModel(repository) as T
            }
        }
    }
}
