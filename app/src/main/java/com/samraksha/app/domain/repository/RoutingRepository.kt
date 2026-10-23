package com.samraksha.app.domain.repository

import com.samraksha.app.domain.model.AutocompleteResult
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteOption
import org.maplibre.android.geometry.LatLng

sealed class RoutingResult {
    data class Success(val route: RouteInfo) : RoutingResult()
    data class Error(val message: String, val cause: Throwable? = null) : RoutingResult()
}

sealed class RouteOptionsResult {
    data class Success(
        val options: List<RouteOption>,
        val isAlternativeAvailable: Boolean,
        val limitationNotice: String? = null
    ) : RouteOptionsResult()

    data class Error(val message: String, val cause: Throwable? = null) : RouteOptionsResult()
}

interface RoutingRepository {

    /**
     * Calculates driving, bicycling, or walking route from origin to destination coordinates.
     * If destLat/destLng are not provided, geocodes the destination string first.
     */
    suspend fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destination: String,
        destLat: Double? = null,
        destLng: Double? = null,
        mode: String = "drive"
    ): RoutingResult

    /**
     * Calculates up to 2-3 distinct route options using Geoapify routing strategies
     * (balanced, short, less_maneuvers) and computes the Night Activity Score for each.
     * If only 1 distinct route exists, returns it honestly without fabricating routes.
     */
    suspend fun calculateRouteOptions(
        startLat: Double,
        startLng: Double,
        destination: String,
        destLat: Double? = null,
        destLng: Double? = null,
        mode: String = "drive"
    ): RouteOptionsResult

    /**
     * Searches places and addresses via Geoapify Geocoding Autocomplete API.
     */
    suspend fun searchPlaces(
        query: String,
        userLat: Double? = null,
        userLng: Double? = null,
        cityFilter: String? = null
    ): AutocompleteResult

    /**
     * Resolves an address or landmark string to coordinates via Geocoding API.
     */
    suspend fun geocodeAddress(address: String): LatLng?
}
