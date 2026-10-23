package com.samraksha.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.location.model.toUserLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Production implementation of [LocationClient] leveraging Google Play Services
 * [FusedLocationProviderClient] with robust permission, hardware availability,
 * and offline-safe fallback checks.
 */
class DefaultLocationClient(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) : LocationClient {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun hasPermission(): Boolean {
        return LocationPermissionHelper.hasLocationPermission(context)
    }

    override fun isGpsEnabled(): Boolean {
        val isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsOn || isNetworkOn
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(highAccuracy: Boolean): LocationState {
        if (!hasPermission()) {
            return LocationState.Error.PermissionDenied
        }

        if (!isGpsEnabled()) {
            return LocationState.Error.GpsDisabled
        }

        // Use HIGH_ACCURACY only if user granted FINE location; otherwise use BALANCED for approximate (COARSE) location
        val priority = if (highAccuracy && LocationPermissionHelper.hasFineLocationPermission(context)) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val cts = CancellationTokenSource()

        return try {
            val task = fusedLocationClient.getCurrentLocation(priority, cts.token)
            val location = task.awaitWithCancellation(cts)
            if (location != null) {
                LocationState.Success(location.toUserLocation())
            } else {
                // Fallback to last known location if fresh fix timed out
                val lastKnown = getLastKnownLocation()
                if (lastKnown != null) {
                    LocationState.Success(lastKnown)
                } else {
                    LocationState.Error.Unavailable("Could not retrieve current GPS coordinates.")
                }
            }
        } catch (e: Exception) {
            LocationState.Error.Unavailable(e.localizedMessage ?: "Unknown location retrieval error")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): UserLocation? {
        if (!hasPermission()) return null

        return try {
            val task = fusedLocationClient.lastLocation
            val location = task.await()
            location?.toUserLocation()
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(
        intervalMs: Long,
        minDistanceMeters: Float,
        highAccuracy: Boolean
    ): Flow<LocationState> = callbackFlow {
        if (!hasPermission()) {
            trySend(LocationState.Error.PermissionDenied)
            close()
            return@callbackFlow
        }

        if (!isGpsEnabled()) {
            trySend(LocationState.Error.GpsDisabled)
        }

        // Use HIGH_ACCURACY only if user granted FINE location; otherwise use BALANCED for approximate (COARSE) location
        val priority = if (highAccuracy && LocationPermissionHelper.hasFineLocationPermission(context)) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setWaitForAccurateLocation(false)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(LocationState.Success(location.toUserLocation()))
                } ?: run {
                    trySend(LocationState.Error.Unavailable("Empty location batch received"))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        ).addOnFailureListener { error ->
            trySend(LocationState.Error.Unavailable(error.localizedMessage ?: "Failed to start updates"))
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}

/**
 * Extension to await a Google Play Services [Task] using [suspendCancellableCoroutine].
 */
private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener {
        continuation.resume(null)
    }
    addOnCanceledListener {
        continuation.resume(null)
    }
}

/**
 * Extension to await a Google Play Services [Task] with a [CancellationTokenSource].
 */
private suspend fun <T> Task<T>.awaitWithCancellation(cts: CancellationTokenSource): T? =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            cts.cancel()
        }
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener {
            continuation.resume(null)
        }
        addOnCanceledListener {
            continuation.resume(null)
        }
    }
