package com.samraksha.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.UserLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocationStatus {
    READY,
    GPS_DISABLED,
    PERMISSION_NEEDED,
    SEARCHING
}

data class HomeUiState(
    val userName: String = "Guardian",
    val contactCount: Int = 0,
    val locationStatus: LocationStatus = LocationStatus.SEARCHING,
    val lastKnownLocation: UserLocation? = null,
    val isJourneyActive: Boolean = false,
    val activeJourneyDestination: String? = null,
    val isLoading: Boolean = false
) {
    val isLocationReady: Boolean
        get() = locationStatus == LocationStatus.READY

    val isProtected: Boolean
        get() = contactCount > 0 && isLocationReady
}

/**
 * ViewModel for the SAMRAKSHA Home screen.
 *
 * Exposes real application state:
 * - Current user display name
 * - Configured emergency contacts count
 * - Location & GPS readiness state
 * - Active journey monitoring state
 */
class HomeViewModel(
    private val userRepository: UserRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val journeyRepository: JourneyRepository,
    private val locationClient: LocationClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        observeContacts()
        observeActiveJourney()
        refreshLocationState()
    }

    private fun observeUser() {
        viewModelScope.launch {
            userRepository.getPrimaryUser().collect { user ->
                val displayName = user?.fullName?.takeIf { it.isNotBlank() } ?: "Guardian"
                _uiState.update { it.copy(userName = displayName) }
            }
        }
    }

    private fun observeContacts() {
        viewModelScope.launch {
            emergencyContactRepository.getEmergencyContacts().collect { contacts ->
                _uiState.update { it.copy(contactCount = contacts.size) }
            }
        }
    }

    private fun observeActiveJourney() {
        viewModelScope.launch {
            journeyRepository.getActiveJourney().collect { activeJourney ->
                _uiState.update {
                    it.copy(
                        isJourneyActive = activeJourney != null,
                        activeJourneyDestination = activeJourney?.endLocationName
                    )
                }
            }
        }
    }

    fun refreshLocationState() {
        viewModelScope.launch {
            val hasPerm = locationClient.hasPermission()
            val gpsEnabled = locationClient.isGpsEnabled()

            val status = when {
                !hasPerm -> LocationStatus.PERMISSION_NEEDED
                !gpsEnabled -> LocationStatus.GPS_DISABLED
                else -> LocationStatus.READY
            }

            val lastLoc = if (hasPerm && gpsEnabled) {
                locationClient.getLastKnownLocation()
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    locationStatus = status,
                    lastKnownLocation = lastLoc
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            userRepository: UserRepository = SamrakshaApp.instance.userRepository,
            emergencyContactRepository: EmergencyContactRepository = SamrakshaApp.instance.emergencyContactRepository,
            journeyRepository: JourneyRepository = SamrakshaApp.instance.journeyRepository,
            locationClient: LocationClient = SamrakshaApp.instance.locationClient
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    userRepository = userRepository,
                    emergencyContactRepository = emergencyContactRepository,
                    journeyRepository = journeyRepository,
                    locationClient = locationClient
                ) as T
            }
        }
    }
}
