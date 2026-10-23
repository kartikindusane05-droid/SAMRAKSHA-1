package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.SafetyZoneDto
import com.samraksha.app.data.model.SafetyZoneListData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for SAMRAKSHA Safety Zone endpoints.
 */
interface SafetyZoneApiService {

    /**
     * Retrieve all safety zones with optional query parameters.
     */
    @GET("api/safety-zones")
    suspend fun getSafetyZones(
        @Query("riskLevel") riskLevel: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radius: Double? = null
    ): Response<ApiResponse<SafetyZoneListData>>

    /**
     * Retrieve a specific safety zone by ID.
     */
    @GET("api/safety-zones/{id}")
    suspend fun getSafetyZoneById(
        @Path("id") id: String
    ): Response<ApiResponse<SafetyZoneDto>>

    /**
     * Batch synchronize safety zones from client cache.
     */
    @retrofit2.http.POST("api/safety-zones/sync")
    suspend fun syncSafetyZones(
        @retrofit2.http.Body zones: List<SafetyZoneDto>
    ): Response<ApiResponse<SafetyZoneListData>>
}
