package com.samraksha.app.domain.model

import org.maplibre.android.geometry.LatLng
import java.util.Locale

/**
 * Domain representation of an autocomplete place suggestion from Geoapify.
 */
data class PlaceSuggestion(
    val id: String,
    val name: String,
    val fullAddress: String,
    val locality: String? = null,
    val coordinates: LatLng,
    val distanceMeters: Double? = null
) {
    /**
     * User-facing formatted distance string (e.g., "1.8 km away" or "650 m away").
     */
    val formattedDistance: String?
        get() = distanceMeters?.let {
            if (it >= 1000.0) {
                String.format(Locale.getDefault(), "%.1f km away", it / 1000.0)
            } else {
                String.format(Locale.getDefault(), "%.0f m away", it)
            }
        }
}

/**
 * Result state container for geocoding autocomplete operations.
 */
sealed class AutocompleteResult {
    data class Success(val suggestions: List<PlaceSuggestion>) : AutocompleteResult()
    data object Empty : AutocompleteResult()
    data class Error(
        val message: String,
        val isNetworkError: Boolean = false,
        val isRateLimited: Boolean = false
    ) : AutocompleteResult()
}
