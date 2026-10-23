package com.samraksha.app.domain.model

/**
 * Domain entity representing an authenticated user.
 */
data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val bloodGroup: String? = null,
    val allergies: String? = null,
    val isOrganDonor: Boolean = false
)

/**
 * Domain entity representing an active authenticated session.
 */
data class AuthSession(
    val user: User,
    val token: String
)
