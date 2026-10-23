package com.samraksha.app.location

import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Reusable contract for querying and tracking user location in SAMRAKSHA.
 * Keeps all Android platform and hardware sensor details isolated from UI.
 */
interface LocationClient {

    /** Returns true if the app has required runtime location permissions. */
    fun hasPermission(): Boolean

    /** Returns true if device location hardware / GPS provider is enabled. */
    fun isGpsEnabled(): Boolean

    /**
     * Fetches a one-shot current location fix.
     *
     * @param highAccuracy If true, requests high accuracy GPS fix; otherwise balanced power.
     */
    suspend fun getCurrentLocation(highAccuracy: Boolean = true): LocationState

    /**
     * Retrieves the most recently cached location on the device if available.
     */
    suspend fun getLastKnownLocation(): UserLocation?

    /**
     * Returns a reactive stream emitting periodic location updates.
     *
     * @param intervalMs Desired update interval in milliseconds.
     * @param minDistanceMeters Minimum distance displacement required before emitting updates.
     *                          Filtering small displacements prevents GPS/CPU wakeups when stationary.
     * @param highAccuracy Whether to use high accuracy GNSS or balanced power accuracy.
     */
    fun getLocationUpdates(
        intervalMs: Long = 5000L,
        minDistanceMeters: Float = 0f,
        highAccuracy: Boolean = true
    ): Flow<LocationState>
}
