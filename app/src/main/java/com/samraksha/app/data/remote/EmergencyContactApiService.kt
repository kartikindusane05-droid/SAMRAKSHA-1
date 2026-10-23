package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.EmergencyContactDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Emergency Contact backend endpoints.
 */
interface EmergencyContactApiService {

    @GET("api/contacts")
    suspend fun getContacts(
        @Query("userId") userId: String? = null
    ): Response<ApiResponse<List<EmergencyContactDto>>>

    @GET("api/contacts/{id}")
    suspend fun getContactById(
        @Path("id") id: String
    ): Response<ApiResponse<EmergencyContactDto>>

    @POST("api/contacts")
    suspend fun addContact(
        @Query("userId") userId: String? = null,
        @Body contact: EmergencyContactDto
    ): Response<ApiResponse<EmergencyContactDto>>

    @PUT("api/contacts/{id}")
    suspend fun updateContact(
        @Path("id") id: String,
        @Body contact: EmergencyContactDto
    ): Response<ApiResponse<EmergencyContactDto>>

    @DELETE("api/contacts/{id}")
    suspend fun deleteContact(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @POST("api/contacts/sync")
    suspend fun syncContacts(
        @Query("userId") userId: String? = null,
        @Body contacts: List<EmergencyContactDto>
    ): Response<ApiResponse<List<EmergencyContactDto>>>
}
