package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing an emergency event from the backend.
 */
data class EmergencyEventDto(
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("status") val status: String,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("address") val address: String? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("resolvedAt") val resolvedAt: Long? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

/**
 * Payload for updating the status of an emergency event.
 */
data class UpdateEventStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("resolvedAt") val resolvedAt: Long? = null
)

/**
 * Wrapper for batch event sync payload.
 */
data class SyncEventsRequest(
    @SerializedName("events") val events: List<EmergencyEventDto>
)
