package com.samraksha.app.data.repository

import com.samraksha.app.data.local.SessionManager
import com.samraksha.app.data.local.dao.UserDao
import com.samraksha.app.data.local.entity.toDomainModel
import com.samraksha.app.data.local.entity.toEntity
import com.samraksha.app.data.model.MedicalInfoDto
import com.samraksha.app.data.model.UpdateProfileRequest
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.AuthApiService
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first repository for User Data.
 *
 * Local Room database and SessionManager serve as the immediate source of truth.
 * Changes made while offline are saved locally and synchronized with the backend
 * whenever connectivity is established.
 */
class RoomUserRepository(
    private val userDao: UserDao,
    private val apiService: AuthApiService = ApiClient.authApiService,
    private val sessionManager: SessionManager = SessionManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    override fun getUser(userId: String): Flow<User?> {
        return userDao.getUser(userId).map { entity ->
            entity?.toDomainModel() ?: sessionManager.getUser()?.takeIf { it.id == userId }
        }
    }

    override fun getPrimaryUser(): Flow<User?> {
        return userDao.getPrimaryUser().map { entity ->
            entity?.toDomainModel() ?: sessionManager.getUser()
        }
    }

    override suspend fun getPrimaryUserDirect(): User? = withContext(ioDispatcher) {
        userDao.getPrimaryUserSync()?.toDomainModel()
            ?: sessionManager.getUser()
    }

    override suspend fun saveUser(user: User) {
        withContext(ioDispatcher) {
            userDao.insertUser(user.toEntity(isSynced = true))
            val token = sessionManager.getToken() ?: "offline_demo_token"
            sessionManager.saveSession(token, user)
        }
    }

    override suspend fun updateProfile(user: User): Result<User> = withContext(ioDispatcher) {
        // 1. Local-first immediate write so app remains fully responsive offline
        userDao.insertUser(user.toEntity(isSynced = false))
        val currentToken = sessionManager.getToken()
        if (currentToken != null) {
            sessionManager.saveSession(currentToken, user)
        }

        // 2. Attempt remote push to backend
        try {
            val tokenHeader = currentToken?.let { "Bearer $it" }
            val request = UpdateProfileRequest(
                fullName = user.fullName,
                phone = user.phone,
                medicalInfo = MedicalInfoDto(
                    bloodGroup = user.bloodGroup,
                    allergies = user.allergies,
                    isOrganDonor = user.isOrganDonor
                )
            )

            val response = apiService.updateProfile(
                token = tokenHeader,
                userId = user.id,
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                // Mark as synchronized
                userDao.insertUser(user.toEntity(isSynced = true))
                Result.success(user)
            } else {
                // Kept safely in offline storage
                Result.success(user)
            }
        } catch (_: Exception) {
            // Offline gracefully — local changes are preserved and will be synced later
            Result.success(user)
        }
    }

    override suspend fun syncUser(userId: String?): Result<User> = withContext(ioDispatcher) {
        val targetUser = sessionManager.getUser()
            ?: userDao.getPrimaryUserSync()?.toDomainModel()
            ?: return@withContext Result.failure(IllegalStateException("No active authenticated user"))

        val targetId = userId ?: targetUser.id
        val token = sessionManager.getToken()
        val tokenHeader = token?.let { "Bearer $it" }

        try {
            // First fetch latest profile from backend GET /api/auth/profile
            val profileResponse = apiService.getProfile(
                token = tokenHeader,
                userId = targetId
            )

            if (profileResponse.isSuccessful && profileResponse.body()?.data != null) {
                val remoteDto = profileResponse.body()!!.data!!
                val mergedUser = User(
                    id = remoteDto.id,
                    fullName = remoteDto.fullName,
                    email = remoteDto.email,
                    phone = remoteDto.phone,
                    bloodGroup = remoteDto.medicalInfo?.bloodGroup ?: targetUser.bloodGroup,
                    allergies = remoteDto.medicalInfo?.allergies ?: targetUser.allergies,
                    isOrganDonor = remoteDto.medicalInfo?.isOrganDonor ?: targetUser.isOrganDonor
                )

                userDao.insertUser(mergedUser.toEntity(isSynced = true))
                if (token != null) {
                    sessionManager.saveSession(token, mergedUser)
                }
                return@withContext Result.success(mergedUser)
            }

            // Fallback sync attempt: push local changes and fetch latest merged state
            val syncRequest = UpdateProfileRequest(
                fullName = targetUser.fullName,
                phone = targetUser.phone,
                medicalInfo = MedicalInfoDto(
                    bloodGroup = targetUser.bloodGroup,
                    allergies = targetUser.allergies,
                    isOrganDonor = targetUser.isOrganDonor
                )
            )

            val syncResponse = apiService.syncProfile(
                token = tokenHeader,
                userId = targetId,
                request = syncRequest
            )

            if (syncResponse.isSuccessful && syncResponse.body()?.data != null) {
                val remoteDto = syncResponse.body()!!.data!!
                val mergedUser = User(
                    id = remoteDto.id,
                    fullName = remoteDto.fullName,
                    email = remoteDto.email,
                    phone = remoteDto.phone,
                    bloodGroup = remoteDto.medicalInfo?.bloodGroup ?: targetUser.bloodGroup,
                    allergies = remoteDto.medicalInfo?.allergies ?: targetUser.allergies,
                    isOrganDonor = remoteDto.medicalInfo?.isOrganDonor ?: targetUser.isOrganDonor
                )

                userDao.insertUser(mergedUser.toEntity(isSynced = true))
                if (token != null) {
                    sessionManager.saveSession(token, mergedUser)
                }
                Result.success(mergedUser)
            } else {
                // Fallback to local cache without crashing
                Result.success(targetUser)
            }
        } catch (_: Exception) {
            // Offline fallback to local cache without crashing
            Result.success(targetUser)
        }
    }

    override suspend fun clearUsers() {
        withContext(ioDispatcher) {
            userDao.clearUsers()
        }
    }
}
