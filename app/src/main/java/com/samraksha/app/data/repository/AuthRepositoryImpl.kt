package com.samraksha.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.local.SessionManager
import com.samraksha.app.data.local.dao.UserDao
import com.samraksha.app.data.local.entity.toEntity
import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.AuthData
import com.samraksha.app.data.model.LoginRequest
import com.samraksha.app.data.model.SignupRequest
import com.samraksha.app.data.model.UserDto
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.AuthApiService
import com.samraksha.app.domain.model.AuthSession
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.AuthRepository
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Implementation of AuthRepository communicating with the SAMRAKSHA backend API.
 */
class AuthRepositoryImpl(
    private val apiService: AuthApiService = ApiClient.authApiService,
    private val sessionManager: SessionManager = try { SamrakshaApp.instance.sessionManager } catch (_: Exception) { SessionManager() },
    private val userDao: UserDao? = try { SamrakshaApp.instance.database.userDao() } catch (_: Exception) { null }
) : AuthRepository {

    private val gson = Gson()

    override suspend fun signup(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Result<AuthSession> {
        return try {
            val request = SignupRequest(
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.trim(),
                password = password
            )

            val response = apiService.signup(request)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse != null && apiResponse.success && apiResponse.data != null) {
                    val authData = apiResponse.data
                    val user = mapDtoToUser(authData.user)
                    val session = AuthSession(user = user, token = authData.token)

                    // Persist session locally in encrypted prefs & Room database
                    sessionManager.saveSession(authData.token, user)
                    try { userDao?.insertUser(user.toEntity(isSynced = true)) } catch (_: Exception) {}
                    Result.success(session)
                } else {
                    val message = apiResponse?.message ?: "Signup failed"
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = extractErrorMessage(response.errorBody()?.string())
                    ?: "Server returned error: ${response.code()}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: ConnectException) {
            Result.failure(Exception("Cannot reach SAMRAKSHA server. Ensure backend is running."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Please try again."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Check your connection"}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(
        identifier: String,
        password: String
    ): Result<AuthSession> {
        return try {
            val request = LoginRequest(
                identifier = identifier.trim(),
                password = password
            )

            val response = apiService.login(request)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse != null && apiResponse.success && apiResponse.data != null) {
                    val authData = apiResponse.data
                    val user = mapDtoToUser(authData.user)
                    val session = AuthSession(user = user, token = authData.token)

                    // Persist session locally
                    sessionManager.saveSession(authData.token, user)
                    try { userDao?.insertUser(user.toEntity(isSynced = true)) } catch (_: Exception) {}
                    Result.success(session)
                } else {
                    val message = apiResponse?.message ?: "Invalid credentials"
                    Result.failure(Exception(message))
                }
            } else {
                val errorMessage = extractErrorMessage(response.errorBody()?.string())
                    ?: "Invalid credentials (Code ${response.code()})"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: ConnectException) {
            Result.failure(Exception("Cannot reach SAMRAKSHA server. Ensure backend is running."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Please try again."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.localizedMessage ?: "Check your connection"}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        val token = sessionManager.getToken()
        if (token != null) {
            try {
                apiService.logout("Bearer $token")
            } catch (_: Exception) {
                // Ignore network error on logout; local purge takes priority
            }
        }
        sessionManager.clearSession()
        try { userDao?.clearUsers() } catch (_: Exception) {}
        return Result.success(Unit)
    }

    override fun getSavedToken(): String? = sessionManager.getToken()

    override fun getCurrentUser(): User? = sessionManager.getUser()

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    private fun mapDtoToUser(dto: UserDto): User {
        return User(
            id = dto.id,
            fullName = dto.fullName,
            email = dto.email,
            phone = dto.phone,
            bloodGroup = dto.medicalInfo?.bloodGroup,
            allergies = dto.medicalInfo?.allergies,
            isOrganDonor = dto.medicalInfo?.isOrganDonor ?: false
        )
    }

    private fun extractErrorMessage(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<ApiResponse<Any>>() {}.type
            val parsed: ApiResponse<Any> = gson.fromJson(errorJson, type)
            parsed.message
        } catch (_: Exception) {
            null
        }
    }
}
