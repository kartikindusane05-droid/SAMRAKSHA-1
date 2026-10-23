package com.samraksha.app.domain.model

import org.maplibre.android.geometry.LatLng
import java.util.Locale

/**
 * Domain representation of a calculated journey route between origin and destination.
 */
data class RouteInfo(
    val points: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val startPoint: LatLng,
    val destinationPoint: LatLng,
    val destinationName: String,
    val steps: List<RouteStepInfo> = emptyList()
) {
    val formattedDistance: String
        get() = if (distanceMeters >= 1000.0) {
            String.format(Locale.getDefault(), "%.1f km", distanceMeters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.0f m", distanceMeters)
        }

    val formattedDuration: String
        get() {
            val totalMins = Math.round(durationSeconds / 60.0).toInt()
            val hours = totalMins / 60
            val mins = totalMins % 60
            return when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                else -> "${mins.coerceAtLeast(1)} min"
            }
        }
}
