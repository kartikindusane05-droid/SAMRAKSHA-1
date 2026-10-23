package com.samraksha.app.data.repository

import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.CreateIncidentReportRequest
import com.samraksha.app.data.model.IncidentCategory
import com.samraksha.app.data.model.IncidentReportDto
import com.samraksha.app.data.remote.IncidentReportApiService
import com.samraksha.app.domain.repository.IncidentSubmissionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentReportRepositoryTest {

    private lateinit var fakeApiService: FakeIncidentReportApiService
    private lateinit var repository: IncidentReportRepositoryImpl

    @Before
    fun setUp() {
        fakeApiService = FakeIncidentReportApiService()
        repository = IncidentReportRepositoryImpl(fakeApiService)
    }

    @Test
    fun submitReport_success_returnsSuccessResult() = runTest {
        val result = repository.submitReport(
            category = IncidentCategory.HARASSMENT,
            latitude = 12.9716,
            longitude = 77.5946,
            incidentDate = "2026-10-20",
            description = "Test description",
            confirmDuplicate = false
        )

        assertTrue(result is IncidentSubmissionResult.Success)
        val success = result as IncidentSubmissionResult.Success
        assertEquals("inc_test_123", success.report.id)
        assertEquals(IncidentCategory.HARASSMENT.key, success.report.category)
        assertEquals("PENDING_REVIEW", success.report.status)
        assertEquals(12.9716, success.report.latitude, 0.0001)
        assertEquals(77.5946, success.report.longitude, 0.0001)
    }

    @Test
    fun submitReport_duplicateWarning_returnsPotentialDuplicate() = runTest {
        fakeApiService.returnDuplicateWarning = true

        val result = repository.submitReport(
            category = IncidentCategory.THEFT,
            latitude = 12.9716,
            longitude = 77.5946,
            incidentDate = "2026-10-20",
            description = null,
            confirmDuplicate = false
        )

        assertTrue(result is IncidentSubmissionResult.PotentialDuplicate)
        val duplicate = result as IncidentSubmissionResult.PotentialDuplicate
        assertTrue(duplicate.message.contains("A similar incident"))
    }

    @Test
    fun submitReport_duplicateConfirmed_returnsSuccess() = runTest {
        fakeApiService.returnDuplicateWarning = false

        val result = repository.submitReport(
            category = IncidentCategory.THEFT,
            latitude = 12.9716,
            longitude = 77.5946,
            incidentDate = "2026-10-20",
            description = null,
            confirmDuplicate = true
        )

        assertTrue(result is IncidentSubmissionResult.Success)
    }

    @Test
    fun submitReport_serverError_returnsErrorResult() = runTest {
        fakeApiService.shouldReturnHttpError = true

        val result = repository.submitReport(
            category = IncidentCategory.SUSPICIOUS_ACTIVITY,
            latitude = 12.9716,
            longitude = 77.5946,
            incidentDate = "2026-10-20"
        )

        assertTrue(result is IncidentSubmissionResult.Error)
        val error = result as IncidentSubmissionResult.Error
        assertEquals("Validation failed: invalid input", error.message)
    }

    @Test
    fun submitReport_networkFailure_returnsNetworkErrorMessage() = runTest {
        fakeApiService.throwIoException = true

        val result = repository.submitReport(
            category = IncidentCategory.OTHER,
            latitude = 12.9716,
            longitude = 77.5946,
            incidentDate = "2026-10-20"
        )

        assertTrue(result is IncidentSubmissionResult.Error)
        val error = result as IncidentSubmissionResult.Error
        assertTrue(error.message.contains("Network connection failed"))
    }
}

private class FakeIncidentReportApiService : IncidentReportApiService {
    var returnDuplicateWarning = false
    var shouldReturnHttpError = false
    var throwIoException = false

    override suspend fun reportIncident(
        request: CreateIncidentReportRequest
    ): Response<ApiResponse<IncidentReportDto>> {
        if (throwIoException) {
            throw IOException("SocketTimeoutException: Connection timed out")
        }

        if (shouldReturnHttpError) {
            val errorBody = "{\"success\":false,\"error\":\"Validation failed: invalid input\"}"
                .toResponseBody("application/json".toMediaTypeOrNull())
            return Response.error(400, errorBody)
        }

        if (returnDuplicateWarning && !request.confirmDuplicate) {
            return Response.success(
                ApiResponse(
                    success = false,
                    data = null,
                    message = "A similar incident was reported in this area recently.",
                    warning = "POTENTIAL_DUPLICATE"
                )
            )
        }

        val reportDto = IncidentReportDto(
            id = "inc_test_123",
            category = request.category,
            latitude = request.latitude,
            longitude = request.longitude,
            incidentDate = request.incidentDate,
            description = request.description,
            status = "PENDING_REVIEW",
            createdAt = "2026-10-20T10:00:00.000Z"
        )

        return Response.success(
            ApiResponse(
                success = true,
                data = reportDto,
                message = "Incident reported successfully. Awaiting moderator review."
            )
        )
    }

    override suspend fun getMyReports(): Response<ApiResponse<List<IncidentReportDto>>> {
        return Response.success(
            ApiResponse(
                success = true,
                data = emptyList(),
                message = "Incident reports retrieved"
            )
        )
    }
}
