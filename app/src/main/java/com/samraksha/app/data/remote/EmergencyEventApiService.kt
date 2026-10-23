package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.EmergencyEventDto
import com.samraksha.app.data.model.UpdateEventStatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Emergency Event backend endpoints.
 */
interface EmergencyEventApiService {

    @GET("api/emergency-events")
    suspend fun getEvents(
        @Query("userId") userId: String? = null
    ): Response<ApiResponse<List<EmergencyEventDto>>>

    @GET("api/emergency-events/{id}")
    suspend fun getEventById(
        @Path("id") id: String
    ): Response<ApiResponse<EmergencyEventDto>>

    @POST("api/emergency-events")
    suspend fun createEvent(
        @Query("userId") userId: String? = null,
        @Body event: EmergencyEventDto
    ): Response<ApiResponse<EmergencyEventDto>>

    @PATCH("api/emergency-events/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body body: UpdateEventStatusRequest
    ): Response<ApiResponse<EmergencyEventDto>>

    @POST("api/emergency-events/sync")
    suspend fun syncEvents(
        @Query("userId") userId: String? = null,
        @Body events: List<EmergencyEventDto>
    ): Response<ApiResponse<List<EmergencyEventDto>>>
}
