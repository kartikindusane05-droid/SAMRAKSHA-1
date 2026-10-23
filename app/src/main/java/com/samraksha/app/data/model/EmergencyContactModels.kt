package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing an emergency contact from the backend.
 */
data class EmergencyContactDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("relationship") val relationship: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

/**
 * Wrapper for batch contact sync payload.
 */
data class SyncContactsRequest(
    @SerializedName("contacts") val contacts: List<EmergencyContactDto>
)
