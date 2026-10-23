package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Categories of safety incidents that community members can report.
 */
enum class IncidentCategory(val key: String, val displayName: String) {
    HARASSMENT("HARASSMENT", "Harassment"),
    THEFT("THEFT", "Theft"),
    SUSPICIOUS_ACTIVITY("SUSPICIOUS_ACTIVITY", "Suspicious activity"),
    PHYSICAL_SAFETY("PHYSICAL_SAFETY", "Physical safety concern"),
    OTHER("OTHER", "Other");

    companion object {
        fun fromKey(key: String): IncidentCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OTHER
    }
}

/**
 * Request payload for creating a new incident report.
 */
data class CreateIncidentReportRequest(
    @SerializedName("category") val category: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("incidentDate") val incidentDate: String, // YYYY-MM-DD
    @SerializedName("description") val description: String? = null,
    @SerializedName("confirmDuplicate") val confirmDuplicate: Boolean = false
)

/**
 * Data representation of a stored incident report returned from backend.
 */
data class IncidentReportDto(
    @SerializedName("id") val id: String,
    @SerializedName("category") val category: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("incidentDate") val incidentDate: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String? = null
)
