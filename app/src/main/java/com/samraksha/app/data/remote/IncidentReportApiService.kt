package com.samraksha.app.data.remote

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.CreateIncidentReportRequest
import com.samraksha.app.data.model.IncidentReportDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IncidentReportApiService {

    @POST("api/incidents")
    suspend fun reportIncident(
        @Body request: CreateIncidentReportRequest
    ): Response<ApiResponse<IncidentReportDto>>

    @GET("api/incidents/my-reports")
    suspend fun getMyReports(): Response<ApiResponse<List<IncidentReportDto>>>
}
