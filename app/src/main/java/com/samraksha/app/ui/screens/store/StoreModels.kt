package com.samraksha.app.ui.screens.store

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector
import com.samraksha.app.data.model.CategoryDto
import com.samraksha.app.data.model.ProductDto

/**
 * Represents a product category in the SAMRAKSHA Safety Store.
 */
data class StoreCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val productCount: Int,
    /** Emoji used as the visual badge in the category chip. */
    val emoji: String
)

/**
 * Represents a safety product in the store catalog.
 */
data class StoreProduct(
    val id: String,
    val name: String,
    val shortDescription: String,
    val price: Double,
    val originalPrice: Double? = null,
    val categoryId: String,
    val imageEmoji: String,
    /** Product rating out of 5. */
    val rating: Float = 4.5f,
    val reviewCount: Int = 0,
    val isBestseller: Boolean = false,
    val badge: String? = null,
    val description: String = shortDescription,
    val categoryName: String = "",
    val imageUrl: String = "",
    val storeName: String = "Safety Partner",
    val affiliateUrl: String = "",
    val isFeatured: Boolean = false,
    val inStock: Boolean = true,
    val clickCount: Int = 0,
    val relatedProducts: List<StoreProduct> = emptyList()
)

/**
 * Maps a backend [ProductDto] to a domain [StoreProduct].
 */
fun ProductDto.toStoreProduct(): StoreProduct {
    val rawCategory = categoryId ?: category ?: ""
    val derivedEmoji = when (rawCategory.lowercase()) {
        "first_aid", "first aid" -> "🧰"
        "emergency_lighting", "emergency lighting", "lighting" -> "🔦"
        "personal_safety", "personal safety" -> "🛡️"
        "travel_safety", "travel safety" -> "🚗"
        "home_safety", "home safety" -> "🏠"
        "emergency_kits", "emergency kits" -> "🎒"
        else -> "🛡️"
    }

    val catId = categoryId ?: category?.lowercase()?.replace("[^a-z0-9]+".toRegex(), "_") ?: "other"
    val desc = description ?: ""

    return StoreProduct(
        id = id ?: "",
        name = name ?: "Safety Item",
        description = desc,
        shortDescription = if (desc.length > 90) desc.take(87) + "..." else desc,
        price = price,
        originalPrice = originalPrice,
        categoryId = catId,
        categoryName = category ?: "",
        imageEmoji = derivedEmoji,
        imageUrl = imageUrl ?: "",
        storeName = storeName ?: "Safety Partner",
        affiliateUrl = affiliateUrl ?: "",
        isFeatured = isFeatured,
        rating = rating ?: 4.5f,
        reviewCount = reviewCount ?: 0,
        isBestseller = isFeatured || badge?.equals("Bestseller", ignoreCase = true) == true,
        badge = badge ?: if (isFeatured) "Featured" else null,
        inStock = inStock ?: true,
        clickCount = clickCount ?: 0,
        relatedProducts = relatedProducts?.map { it.toStoreProduct() } ?: emptyList()
    )
}

/**
 * Maps a backend [CategoryDto] to a domain [StoreCategory].
 */
fun CategoryDto.toStoreCategory(): StoreCategory {
    val categoryIdStr = id ?: ""
    val icon = when (categoryIdStr.lowercase()) {
        "first_aid" -> Icons.Default.HealthAndSafety
        "emergency_lighting", "lighting" -> Icons.Default.FlashlightOn
        "personal_safety" -> Icons.Default.Shield
        "travel_safety" -> Icons.Default.Luggage
        "home_safety" -> Icons.Default.Home
        "emergency_kits" -> Icons.Default.Backpack
        else -> Icons.Default.Shield
    }

    return StoreCategory(
        id = categoryIdStr,
        name = name ?: "Category",
        icon = icon,
        productCount = productCount,
        emoji = emoji ?: "🛡️"
    )
}
