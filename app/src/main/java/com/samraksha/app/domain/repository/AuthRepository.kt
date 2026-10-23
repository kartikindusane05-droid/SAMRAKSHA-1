package com.samraksha.app.domain.repository

import com.samraksha.app.domain.model.AuthSession
import com.samraksha.app.domain.model.User

/**
 * Repository interface contract for authentication operations.
 */
interface AuthRepository {

    suspend fun signup(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Result<AuthSession>

    suspend fun login(
        identifier: String,
        password: String
    ): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    fun getSavedToken(): String?

    fun getCurrentUser(): User?

    fun isLoggedIn(): Boolean
}
