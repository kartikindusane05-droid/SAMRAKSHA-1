package com.samraksha.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.CreateIncidentReportRequest
import com.samraksha.app.data.model.IncidentCategory
import com.samraksha.app.data.model.IncidentReportDto
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.IncidentReportApiService
import com.samraksha.app.domain.repository.IncidentReportRepository
import com.samraksha.app.domain.repository.IncidentSubmissionResult
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class IncidentReportRepositoryImpl(
    private val apiService: IncidentReportApiService = ApiClient.incidentReportApiService
) : IncidentReportRepository {

    private val gson = Gson()

    override suspend fun submitReport(
        category: IncidentCategory,
        latitude: Double,
        longitude: Double,
        incidentDate: String,
        description: String?,
        confirmDuplicate: Boolean
    ): IncidentSubmissionResult {
        return try {
            val request = CreateIncidentReportRequest(
                category = category.key,
                latitude = latitude,
                longitude = longitude,
                incidentDate = incidentDate,
                description = description?.takeIf { it.isNotBlank() },
                confirmDuplicate = confirmDuplicate
            )

            val response = apiService.reportIncident(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    if (body.success && body.data != null) {
                        IncidentSubmissionResult.Success(
                            report = body.data,
                            message = body.message.ifBlank { "Incident report submitted successfully and is pending review." }
                        )
                    } else if (body.warning == "POTENTIAL_DUPLICATE" || !body.warning.isNullOrBlank()) {
                        IncidentSubmissionResult.PotentialDuplicate(
                            message = body.message.ifBlank { "A similar incident has already been reported near this location on this date." }
                        )
                    } else {
                        IncidentSubmissionResult.Error(
                            message = body.message.ifBlank { "Server rejected incident report." }
                        )
                    }
                } else {
                    IncidentSubmissionResult.Error("Empty response received from safety backend.")
                }
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string())
                    ?: "Server returned HTTP ${response.code()}"
                IncidentSubmissionResult.Error(errorMsg)
            }
        } catch (e: ConnectException) {
            IncidentSubmissionResult.Error("Cannot connect to SAMRAKSHA backend server. Please verify your connection.")
        } catch (e: SocketTimeoutException) {
            IncidentSubmissionResult.Error("Backend connection timed out. Please check your network and try again.")
        } catch (e: UnknownHostException) {
            IncidentSubmissionResult.Error("Network address unreachable. Please check your internet connection.")
        } catch (e: IOException) {
            IncidentSubmissionResult.Error("Network connection failed: ${e.localizedMessage ?: "Connection interrupted"}")
        } catch (e: Exception) {
            IncidentSubmissionResult.Error("Unexpected error submitting report: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun getMyReports(): Result<List<IncidentReportDto>> {
        return try {
            val response = apiService.getMyReports()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                val errorMsg = extractErrorMessage(response.errorBody()?.string()) ?: "Failed to retrieve incident reports"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<ApiResponse<Any>>() {}.type
            val parsed: ApiResponse<Any> = gson.fromJson(errorJson, type)
            parsed.error ?: parsed.message.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
