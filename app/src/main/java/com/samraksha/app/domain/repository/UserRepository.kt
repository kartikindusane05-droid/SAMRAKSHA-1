package com.samraksha.app.domain.repository

import com.samraksha.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user profile data with offline-first synchronization.
 */
interface UserRepository {
    /** Reactive observation of user by ID from local storage. */
    fun getUser(userId: String): Flow<User?>

    /** Reactive observation of the currently active primary user. */
    fun getPrimaryUser(): Flow<User?>

    /** Direct non-reactive fetch of primary user. */
    suspend fun getPrimaryUserDirect(): User?

    /** Persist user data to local cache / Room. */
    suspend fun saveUser(user: User)

    /** Synchronize user profile between backend and local storage. */
    suspend fun syncUser(userId: String? = null): Result<User>

    /** Update user profile both locally and on backend. */
    suspend fun updateProfile(user: User): Result<User>

    /** Clear local user cache (e.g. on logout). */
    suspend fun clearUsers()
}
