package com.samraksha.app.domain.repository

import com.samraksha.app.data.model.IncidentCategory
import com.samraksha.app.data.model.IncidentReportDto

sealed class IncidentSubmissionResult {
    data class Success(val report: IncidentReportDto, val message: String) : IncidentSubmissionResult()
    data class PotentialDuplicate(val message: String) : IncidentSubmissionResult()
    data class Error(val message: String) : IncidentSubmissionResult()
}

interface IncidentReportRepository {

    suspend fun submitReport(
        category: IncidentCategory,
        latitude: Double,
        longitude: Double,
        incidentDate: String,
        description: String? = null,
        confirmDuplicate: Boolean = false
    ): IncidentSubmissionResult

    suspend fun getMyReports(): Result<List<IncidentReportDto>>
}
