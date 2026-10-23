package com.samraksha.app.location.model

import android.location.Location

/**
 * Clean domain representation of a geographical location.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val bearing: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

fun Location.toUserLocation(): UserLocation {
    return UserLocation(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        speed = speed,
        bearing = bearing,
        timestamp = time
    )
}

fun UserLocation.toAndroidLocation(provider: String = "gps"): Location {
    return Location(provider).apply {
        latitude = this@toAndroidLocation.latitude
        longitude = this@toAndroidLocation.longitude
        accuracy = this@toAndroidLocation.accuracy
        altitude = this@toAndroidLocation.altitude
        speed = this@toAndroidLocation.speed
        bearing = this@toAndroidLocation.bearing
        time = this@toAndroidLocation.timestamp
    }
}
