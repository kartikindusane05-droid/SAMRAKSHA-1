package com.samraksha.app.domain.repository

import com.samraksha.app.ui.screens.location.model.SafetyZone

/**
 * Result state for loading safety zones.
 */
sealed class SafetyZoneLoadResult {
    /**
     * Successfully obtained safety zones.
     * @param zones The retrieved safety zones.
     * @param isFromCache True if these were loaded from offline local cache because the network was unavailable or before network fetch.
     * @param message Optional informational message.
     */
    data class Success(
        val zones: List<SafetyZone>,
        val isFromCache: Boolean = false,
        val message: String? = null
    ) : SafetyZoneLoadResult()

    /**
     * Failed to fetch safety zones from the backend.
     * @param message Human-readable error message describing the failure.
     * @param cachedZones Previously saved local zones retained despite network failure.
     * @param cause Underlying exception if available.
     */
    data class Error(
        val message: String,
        val cachedZones: List<SafetyZone> = emptyList(),
        val cause: Throwable? = null
    ) : SafetyZoneLoadResult()
}

/**
 * Repository interface for safety zone data retrieval and local caching.
 */
interface SafetyZoneRepository {

    /**
     * Fetch safety zones from the backend API, falling back to cached local data when offline.
     */
    suspend fun getSafetyZones(
        lat: Double? = null,
        lng: Double? = null,
        radius: Double? = null,
        riskLevel: String? = null,
        forceRefresh: Boolean = false
    ): SafetyZoneLoadResult

    /**
     * Synchronously access any currently available local cached zones.
     */
    fun getCachedSafetyZones(): List<SafetyZone>

    /**
     * Synchronize safety zones with backend, updating local offline cache.
     */
    suspend fun syncSafetyZones(): Result<List<SafetyZone>>
}
