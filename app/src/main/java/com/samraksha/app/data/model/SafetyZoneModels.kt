package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Coordinate pair for boundary polygon points.
 */
data class LatLngPointDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

/**
 * Safety zone data transfer object matching backend response schema.
 */
data class SafetyZoneDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius") val radius: Double,
    @SerializedName("riskLevel") val riskLevel: String,
    @SerializedName("description") val description: String,
    @SerializedName("safetyScore") val safetyScore: Int? = null,
    @SerializedName("advisory") val advisory: String? = null,
    @SerializedName("activePatrols") val activePatrols: Int? = null,
    @SerializedName("cctvCoverage") val cctvCoverage: Int? = null,
    @SerializedName("polygonPoints") val polygonPoints: List<LatLngPointDto>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

/**
 * Payload data envelope returned from GET /api/safety-zones.
 */
data class SafetyZoneListData(
    @SerializedName("count") val count: Int,
    @SerializedName("zones") val zones: List<SafetyZoneDto>
)
