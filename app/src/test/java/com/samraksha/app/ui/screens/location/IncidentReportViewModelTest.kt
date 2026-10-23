package com.samraksha.app.ui.screens.location

import com.samraksha.app.data.model.IncidentCategory
import com.samraksha.app.data.model.IncidentReportDto
import com.samraksha.app.domain.repository.IncidentReportRepository
import com.samraksha.app.domain.repository.IncidentSubmissionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentReportViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeRepository: FakeIncidentReportRepository
    private lateinit var viewModel: IncidentReportViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeIncidentReportRepository()
        viewModel = IncidentReportViewModel(repository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasSensibleDefaults() {
        val state = viewModel.uiState.value
        assertEquals(IncidentCategory.HARASSMENT, state.selectedCategory)
        assertNull(state.latitude)
        assertNull(state.longitude)
        assertFalse(state.isLocationSelected)
        assertFalse(state.canSubmit)
        assertFalse(state.isSubmitting)
        assertFalse(state.showDuplicateWarning)
        assertNull(state.validationError)
    }

    @Test
    fun setCategory_updatesSelectedCategory() {
        viewModel.setCategory(IncidentCategory.SUSPICIOUS_ACTIVITY)
        assertEquals(IncidentCategory.SUSPICIOUS_ACTIVITY, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun setLocation_validCoordinates_enablesSubmission() {
        viewModel.setLocation(12.9716, 77.5946, "MG Road")

        val state = viewModel.uiState.value
        assertEquals(12.9716, state.latitude!!, 0.0001)
        assertEquals(77.5946, state.longitude!!, 0.0001)
        assertEquals("MG Road", state.locationLabel)
        assertTrue(state.isLocationSelected)
        assertTrue(state.canSubmit)
        assertNull(state.validationError)
    }

    @Test
    fun setLocation_invalidCoordinates_setsValidationError() {
        viewModel.setLocation(95.0, 77.5946, "Out of range")

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("Invalid coordinates"))
        assertFalse(state.canSubmit)
    }

    @Test
    fun setDate_futureDate_rejectedWithValidationError() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        val futureDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        viewModel.setDate(futureDate)

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("Incident date cannot be in the future"))
    }

    @Test
    fun setDate_validDate_accepted() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

        viewModel.setDate(yesterday)

        val state = viewModel.uiState.value
        assertEquals(yesterday, state.incidentDate)
        assertNull(state.validationError)
    }

    @Test
    fun setDescription_tooLong_setsValidationError() {
        val longText = "A".repeat(501)
        viewModel.setDescription(longText)

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("cannot exceed 500 characters"))
    }

    @Test
    fun submitReport_withoutLocation_preventsSubmission() = runTest(testDispatcher) {
        viewModel.submitReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("Please select or confirm"))
        assertEquals(0, fakeRepository.submissionCount)
    }

    @Test
    fun submitReport_validInput_submitsAndConfirms() = runTest(testDispatcher) {
        viewModel.setLocation(12.9716, 77.5946, "Test Location")
        viewModel.setDescription("Street light broken and unsafe")

        viewModel.submitReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.isSuccessConfirmed)
        assertNotNull(state.submittedReport)
        assertEquals("inc_vm_101", state.submittedReport?.id)
        assertEquals("PENDING_REVIEW", state.submittedReport?.status)
        assertEquals(1, fakeRepository.submissionCount)
    }

    @Test
    fun submitReport_duplicateDetected_triggersWarningDialog() = runTest(testDispatcher) {
        fakeRepository.simulateDuplicateWarning = true

        viewModel.setLocation(12.9716, 77.5946, "Test Location")
        viewModel.submitReport(confirmDuplicate = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertTrue(state.showDuplicateWarning)
        assertNotNull(state.duplicateWarningMessage)
        assertFalse(state.isSuccessConfirmed)
    }

    @Test
    fun submitReport_confirmDuplicate_submitsSuccessfully() = runTest(testDispatcher) {
        fakeRepository.simulateDuplicateWarning = true

        viewModel.setLocation(12.9716, 77.5946, "Test Location")
        viewModel.submitReport(confirmDuplicate = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.showDuplicateWarning)
        assertTrue(state.isSuccessConfirmed)
        assertTrue(fakeRepository.lastConfirmDuplicate)
    }

    @Test
    fun submitReport_networkError_setsSubmissionError() = runTest(testDispatcher) {
        fakeRepository.simulateError = true
        fakeRepository.errorMessage = "Network connection failed"

        viewModel.setLocation(12.9716, 77.5946, "Test Location")
        viewModel.submitReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertFalse(state.isSuccessConfirmed)
        assertEquals("Network connection failed", state.submissionError)
    }

    @Test
    fun resetForm_clearsAllState() {
        viewModel.setLocation(12.9716, 77.5946, "Test Location")
        viewModel.setDescription("Some note")
        viewModel.setCategory(IncidentCategory.THEFT)

        viewModel.resetForm(defaultLat = 13.0, defaultLng = 77.6)

        val state = viewModel.uiState.value
        assertEquals(13.0, state.latitude!!, 0.0001)
        assertEquals(77.6, state.longitude!!, 0.0001)
        assertEquals("", state.description)
        assertEquals(IncidentCategory.HARASSMENT, state.selectedCategory)
    }
}

private class FakeIncidentReportRepository : IncidentReportRepository {
    var submissionCount = 0
    var lastConfirmDuplicate = false
    var simulateDuplicateWarning = false
    var simulateError = false
    var errorMessage = "Something went wrong"

    override suspend fun submitReport(
        category: IncidentCategory,
        latitude: Double,
        longitude: Double,
        incidentDate: String,
        description: String?,
        confirmDuplicate: Boolean
    ): IncidentSubmissionResult {
        submissionCount++
        lastConfirmDuplicate = confirmDuplicate

        if (simulateError) {
            return IncidentSubmissionResult.Error(errorMessage)
        }

        if (simulateDuplicateWarning && !confirmDuplicate) {
            return IncidentSubmissionResult.PotentialDuplicate(
                "A similar incident was reported nearby on this date."
            )
        }

        return IncidentSubmissionResult.Success(
            report = IncidentReportDto(
                id = "inc_vm_101",
                category = category.key,
                latitude = latitude,
                longitude = longitude,
                incidentDate = incidentDate,
                description = description,
                status = "PENDING_REVIEW",
                createdAt = "2026-10-20T10:00:00.000Z"
            ),
            message = "Incident reported successfully. Awaiting moderator review."
        )
    }

    override suspend fun getMyReports(): Result<List<IncidentReportDto>> {
        return Result.success(emptyList())
    }
}
