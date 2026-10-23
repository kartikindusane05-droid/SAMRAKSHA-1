package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing a journey record from the backend.
 */
data class JourneyDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("startLocationName") val startLocationName: String,
    @SerializedName("endLocationName") val endLocationName: String,
    @SerializedName("startTime") val startTime: String? = null,
    @SerializedName("expectedEndTime") val expectedEndTime: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("startLatitude") val startLatitude: Double = 0.0,
    @SerializedName("startLongitude") val startLongitude: Double = 0.0,
    @SerializedName("currentLatitude") val currentLatitude: Double = 0.0,
    @SerializedName("currentLongitude") val currentLongitude: Double = 0.0,
    @SerializedName("destinationLatitude") val destinationLatitude: Double = 0.0,
    @SerializedName("destinationLongitude") val destinationLongitude: Double = 0.0,
    @SerializedName("completedTime") val completedTime: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

/**
 * Request payload for starting a new journey on backend.
 */
data class CreateJourneyRequest(
    @SerializedName("id") val id: String? = null,
    @SerializedName("startLocationName") val startLocationName: String,
    @SerializedName("endLocationName") val endLocationName: String,
    @SerializedName("startLatitude") val startLatitude: Double,
    @SerializedName("startLongitude") val startLongitude: Double,
    @SerializedName("destinationLatitude") val destinationLatitude: Double,
    @SerializedName("destinationLongitude") val destinationLongitude: Double,
    @SerializedName("expectedEndTime") val expectedEndTime: String? = null
)

/**
 * Request payload for reporting GPS location along a journey.
 */
data class UpdateJourneyLocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

/**
 * Request payload for completing or cancelling a journey.
 */
data class EndJourneyRequest(
    @SerializedName("status") val status: String
)

/**
 * Wrapper for batch journey sync payload.
 */
data class SyncJourneysRequest(
    @SerializedName("journeys") val journeys: List<JourneyDto>
)
