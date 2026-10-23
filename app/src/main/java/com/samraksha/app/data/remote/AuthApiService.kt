package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.AuthData
import com.samraksha.app.data.model.LoginRequest
import com.samraksha.app.data.model.SignupRequest
import com.samraksha.app.data.model.UpdateProfileRequest
import com.samraksha.app.data.model.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Retrofit interface defining SAMRAKSHA backend authentication endpoints.
 */
interface AuthApiService {

    @POST("api/auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<ApiResponse<AuthData>>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthData>>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<ApiResponse<Unit>>

    @GET("api/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String? = null,
        @Query("userId") userId: String? = null
    ): Response<ApiResponse<UserDto>>

    @PUT("api/auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String? = null,
        @Query("userId") userId: String? = null,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserDto>>

    @POST("api/auth/sync")
    suspend fun syncProfile(
        @Header("Authorization") token: String? = null,
        @Query("userId") userId: String? = null,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserDto>>
}
