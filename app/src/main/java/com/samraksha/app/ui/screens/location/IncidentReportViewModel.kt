package com.samraksha.app.ui.screens.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.model.IncidentCategory
import com.samraksha.app.data.model.IncidentReportDto
import com.samraksha.app.domain.repository.IncidentReportRepository
import com.samraksha.app.domain.repository.IncidentSubmissionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IncidentReportUiState(
    val selectedCategory: IncidentCategory = IncidentCategory.HARASSMENT,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String = "No location selected",
    val incidentDate: String = getTodayDateString(),
    val description: String = "",
    val isSubmitting: Boolean = false,
    val validationError: String? = null,
    val submissionError: String? = null,
    val showDuplicateWarning: Boolean = false,
    val duplicateWarningMessage: String? = null,
    val submittedReport: IncidentReportDto? = null,
    val isSuccessConfirmed: Boolean = false
) {
    val isLocationSelected: Boolean
        get() = latitude != null && longitude != null && (latitude != 0.0 || longitude != 0.0)

    val canSubmit: Boolean
        get() = isLocationSelected && incidentDate.isNotBlank() && validationError == null && !isSubmitting
}

private fun getTodayDateString(): String {
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  return sdf.format(Date())
}

class IncidentReportViewModel(
    private val repository: IncidentReportRepository = SamrakshaApp.instance.incidentReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentReportUiState())
    val uiState: StateFlow<IncidentReportUiState> = _uiState.asStateFlow()

    fun setCategory(category: IncidentCategory) {
        _uiState.update { it.copy(selectedCategory = category, validationError = null) }
    }

    fun setLocation(lat: Double, lng: Double, label: String = "Selected incident location") {
        if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) {
            _uiState.update {
                it.copy(validationError = "Invalid coordinates. Latitude must be between -90 and 90.")
            }
            return
        }
        _uiState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                locationLabel = label,
                validationError = null
            )
        }
    }

    fun setDate(dateStr: String) {
        val today = getTodayDateString()
        if (dateStr > today) {
            _uiState.update {
                it.copy(
                    incidentDate = dateStr,
                    validationError = "Incident date cannot be in the future."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                incidentDate = dateStr,
                validationError = null
            )
        }
    }

    fun setDescription(desc: String) {
        if (desc.length > 500) {
            _uiState.update {
                it.copy(
                    description = desc.take(500),
                    validationError = "Description cannot exceed 500 characters."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                description = desc,
                validationError = null
            )
        }
    }

    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(showDuplicateWarning = false, duplicateWarningMessage = null) }
    }

    fun dismissSubmissionError() {
        _uiState.update { it.copy(submissionError = null) }
    }

    fun dismissSuccessConfirmation() {
        _uiState.update { it.copy(isSuccessConfirmed = false, submittedReport = null) }
    }

    fun resetForm(defaultLat: Double? = null, defaultLng: Double? = null) {
        _uiState.update {
            IncidentReportUiState(
                latitude = defaultLat,
                longitude = defaultLng,
                locationLabel = if (defaultLat != null && defaultLng != null) "Current GPS position" else "No location selected"
            )
        }
    }

    fun submitReport(confirmDuplicate: Boolean = false) {
        val state = _uiState.value

        // 1. Client-side validation
        if (state.latitude == null || state.longitude == null) {
            _uiState.update { it.copy(validationError = "Please select or confirm an approximate incident location.") }
            return
        }

        val today = getTodayDateString()
        if (state.incidentDate > today) {
            _uiState.update { it.copy(validationError = "Incident date cannot be in the future.") }
            return
        }

        if (state.description.length > 500) {
            _uiState.update { it.copy(validationError = "Description cannot exceed 500 characters.") }
            return
        }

        _uiState.update {
            it.copy(
                isSubmitting = true,
                validationError = null,
                submissionError = null,
                showDuplicateWarning = false
            )
        }

        viewModelScope.launch {
            when (val result = repository.submitReport(
                category = state.selectedCategory,
                latitude = state.latitude,
                longitude = state.longitude,
                incidentDate = state.incidentDate,
                description = state.description.takeIf { it.isNotBlank() },
                confirmDuplicate = confirmDuplicate
            )) {
                is IncidentSubmissionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submittedReport = result.report,
                            isSuccessConfirmed = true,
                            submissionError = null,
                            showDuplicateWarning = false
                        )
                    }
                }
                is IncidentSubmissionResult.PotentialDuplicate -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            showDuplicateWarning = true,
                            duplicateWarningMessage = result.message
                        )
                    }
                }
                is IncidentSubmissionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submissionError = result.message
                        )
                    }
                }
            }
        }
    }

    class Factory(
        private val repository: IncidentReportRepository = SamrakshaApp.instance.incidentReportRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            IncidentReportViewModel(repository) as T
    }
}
