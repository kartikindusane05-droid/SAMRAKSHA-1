package com.samraksha.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request payload for user registration.
 */
data class SignupRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

/**
 * Request payload for user authentication.
 */
data class LoginRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("password") val password: String
)

/**
 * Generic API response envelope matching backend format.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?,
    @SerializedName("warning") val warning: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

/**
 * Data payload returned from successful login/signup.
 */
data class AuthData(
    @SerializedName("user") val user: UserDto,
    @SerializedName("token") val token: String
)

/**
 * User data transfer object.
 */
data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("medicalInfo") val medicalInfo: MedicalInfoDto?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

/**
 * Medical & emergency details.
 */
data class MedicalInfoDto(
    @SerializedName("bloodGroup") val bloodGroup: String?,
    @SerializedName("allergies") val allergies: String?,
    @SerializedName("isOrganDonor") val isOrganDonor: Boolean?
)

/**
 * Payload for updating user profile.
 */
data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("medicalInfo") val medicalInfo: MedicalInfoDto? = null
)
