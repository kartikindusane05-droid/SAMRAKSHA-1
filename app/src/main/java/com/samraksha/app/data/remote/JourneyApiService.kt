package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.CreateJourneyRequest
import com.samraksha.app.data.model.EndJourneyRequest
import com.samraksha.app.data.model.JourneyDto
import com.samraksha.app.data.model.UpdateJourneyLocationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Journey backend endpoints.
 */
interface JourneyApiService {

    @GET("api/journeys")
    suspend fun getAllJourneys(
        @Query("userId") userId: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<ApiResponse<List<JourneyDto>>>

    @GET("api/journeys/active")
    suspend fun getActiveJourney(
        @Query("userId") userId: String? = null
    ): Response<ApiResponse<JourneyDto?>>

    @GET("api/journeys/{id}")
    suspend fun getJourneyById(
        @Path("id") id: String
    ): Response<ApiResponse<JourneyDto>>

    @POST("api/journeys")
    suspend fun startJourney(
        @Body journey: CreateJourneyRequest
    ): Response<ApiResponse<JourneyDto>>

    @PATCH("api/journeys/{id}/location")
    suspend fun updateLocation(
        @Path("id") id: String,
        @Body body: UpdateJourneyLocationRequest
    ): Response<ApiResponse<JourneyDto>>

    @PATCH("api/journeys/{id}/end")
    suspend fun endJourney(
        @Path("id") id: String,
        @Body body: EndJourneyRequest
    ): Response<ApiResponse<JourneyDto>>

    @DELETE("api/journeys/{id}")
    suspend fun deleteJourney(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @POST("api/journeys/sync")
    suspend fun syncJourneys(
        @Query("userId") userId: String? = null,
        @Body journeys: List<JourneyDto>
    ): Response<ApiResponse<List<JourneyDto>>>
}
