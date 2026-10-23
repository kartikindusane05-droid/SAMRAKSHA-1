package com.samraksha.app.ui.screens.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.maplibre.android.geometry.LatLng
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.RoutingRepository
import com.samraksha.app.domain.repository.RoutingResult
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.ui.screens.location.model.MapMarkerItem
import com.samraksha.app.ui.screens.location.model.MarkerCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SafetyMapUiState(
    val locationState: LocationState = LocationState.Idle,
    val userLocation: UserLocation? = null,
    val hasLocationPermission: Boolean = false,
    val isFineLocation: Boolean = false,
    val isGpsEnabled: Boolean = true,
    val isPermanentlyDenied: Boolean = false,
    val markers: List<MapMarkerItem> = emptyList(),
    val filteredMarkers: List<MapMarkerItem> = emptyList(),
    val selectedCategory: MarkerCategory? = null,
    val selectedMarker: MapMarkerItem? = null,
    val isTracking: Boolean = false,
    val activeRoute: RouteInfo? = null,
    val activeJourneyDestination: String? = null
)

class SafetyMapViewModel(
    private val locationClient: LocationClient,
    private val journeyRepository: JourneyRepository? = try { SamrakshaApp.instance.journeyRepository } catch (_: Exception) { null },
    private val routingRepository: RoutingRepository? = try { SamrakshaApp.instance.routingRepository } catch (_: Exception) { null }
) : ViewModel() {

    private val _uiState = MutableStateFlow(SafetyMapUiState())
    val uiState: StateFlow<SafetyMapUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null
    private var lastKnownLat: Double? = null
    private var lastKnownLng: Double? = null

    init {
        // Evaluate location permission and GPS availability
        checkPermissionAndRefresh()

        // Observe active journey and calculate route if present
        journeyRepository?.let { jRepo ->
            viewModelScope.launch {
                jRepo.getActiveJourney().collect { activeJourney ->
                    if (activeJourney != null && routingRepository != null) {
                        _uiState.update { it.copy(activeJourneyDestination = activeJourney.endLocationName) }
                        val startLat = if (activeJourney.startLatitude != 0.0) activeJourney.startLatitude else (lastKnownLat ?: activeJourney.startLatitude)
                        val startLng = if (activeJourney.startLongitude != 0.0) activeJourney.startLongitude else (lastKnownLng ?: activeJourney.startLongitude)
                        val routeResult = routingRepository.calculateRoute(
                            startLat = startLat,
                            startLng = startLng,
                            destination = activeJourney.endLocationName
                        )
                        if (routeResult is RoutingResult.Success) {
                            _uiState.update { it.copy(activeRoute = routeResult.route) }
                        }
                    } else {
                        _uiState.update { it.copy(activeRoute = null, activeJourneyDestination = null) }
                    }
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean = locationClient.hasPermission()
    fun isGpsEnabled(): Boolean = locationClient.isGpsEnabled()

    /**
     * Checks and updates current permission and GPS hardware status,
     * immediately starting or stopping location updates accordingly.
     */
    fun checkPermissionAndRefresh(
        isFineLocation: Boolean = true,
        isPermanentlyDenied: Boolean = false
    ) {
        val hasPerm = locationClient.hasPermission()
        val gpsOn = locationClient.isGpsEnabled()

        _uiState.update {
            it.copy(
                hasLocationPermission = hasPerm,
                isFineLocation = if (hasPerm) isFineLocation else false,
                isGpsEnabled = gpsOn,
                isPermanentlyDenied = if (hasPerm) false else isPermanentlyDenied
            )
        }

        if (hasPerm) {
            if (!gpsOn) {
                stopContinuousTracking()
                _uiState.update { it.copy(locationState = LocationState.Error.GpsDisabled) }
            } else {
                startContinuousTracking()
                refreshLocation()
            }
        } else {
            stopContinuousTracking()
            _uiState.update { it.copy(locationState = LocationState.Error.PermissionDenied) }
        }
    }

    /**
     * Called when the runtime permission dialog completes.
     */
    fun onPermissionResult(
        granted: Boolean,
        isFineLocation: Boolean = true,
        isPermanentlyDenied: Boolean = false
    ) {
        checkPermissionAndRefresh(
            isFineLocation = isFineLocation,
            isPermanentlyDenied = isPermanentlyDenied
        )
    }

    /**
     * Called when the screen enters RESUMED state.
     * Re-checks permissions in case the user toggled permissions or GPS in Android Settings.
     */
    fun onScreenResumed(
        isFineLocation: Boolean = true,
        isPermanentlyDenied: Boolean = false
    ) {
        checkPermissionAndRefresh(
            isFineLocation = isFineLocation,
            isPermanentlyDenied = isPermanentlyDenied
        )
    }

    /**
     * Called when the screen enters PAUSED state or is disposed.
     * Stops location subscription to prevent battery drain and duplicate listeners.
     */
    fun onScreenPaused() {
        stopContinuousTracking()
    }

    fun refreshLocation() {
        val hasPerm = locationClient.hasPermission()
        val gpsOn = locationClient.isGpsEnabled()

        if (!hasPerm) {
            _uiState.update {
                it.copy(
                    hasLocationPermission = false,
                    locationState = LocationState.Error.PermissionDenied,
                    userLocation = null
                )
            }
            return
        }

        if (!gpsOn) {
            _uiState.update {
                it.copy(
                    isGpsEnabled = false,
                    locationState = LocationState.Error.GpsDisabled,
                    userLocation = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(locationState = LocationState.Loading) }

            // Fast path: Immediately check for last-known location
            val lastKnown = locationClient.getLastKnownLocation()
            if (lastKnown != null && (lastKnown.latitude != 0.0 || lastKnown.longitude != 0.0)) {
                lastKnownLat = lastKnown.latitude
                lastKnownLng = lastKnown.longitude
                _uiState.update {
                    it.copy(
                        userLocation = lastKnown,
                        hasLocationPermission = true,
                        isGpsEnabled = true
                    )
                }
                setupPoiMarkers(lastKnown.latitude, lastKnown.longitude)
            }

            // High accuracy fresh GPS fix from shared LocationClient
            when (val result = locationClient.getCurrentLocation(highAccuracy = _uiState.value.isFineLocation)) {
                is LocationState.Success -> {
                    val newLoc = result.location
                    lastKnownLat = newLoc.latitude
                    lastKnownLng = newLoc.longitude
                    _uiState.update {
                        it.copy(
                            locationState = result,
                            userLocation = newLoc,
                            hasLocationPermission = true,
                            isGpsEnabled = true
                        )
                    }
                    setupPoiMarkers(newLoc.latitude, newLoc.longitude)
                }
                is LocationState.Error.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            locationState = result,
                            hasLocationPermission = false
                        )
                    }
                }
                is LocationState.Error.GpsDisabled -> {
                    _uiState.update {
                        it.copy(
                            locationState = result,
                            isGpsEnabled = false
                        )
                    }
                }
                is LocationState.Error.Unavailable -> {
                    _uiState.update {
                        it.copy(
                            locationState = result
                        )
                    }
                }
                LocationState.Idle, LocationState.Loading -> {
                    if (_uiState.value.userLocation != null) {
                        _uiState.update { it.copy(locationState = LocationState.Idle) }
                    }
                }
            }
        }
    }

    fun startContinuousTracking() {
        if (!locationClient.hasPermission()) return
        if (trackingJob?.isActive == true) return
        _uiState.update { it.copy(isTracking = true) }

        trackingJob = viewModelScope.launch {
            locationClient.getLocationUpdates(
                intervalMs = 4000L,
                highAccuracy = _uiState.value.isFineLocation
            ).collect { state ->
                when (state) {
                    is LocationState.Success -> {
                        val newLoc = state.location
                        val prevLat = lastKnownLat
                        val prevLng = lastKnownLng

                        _uiState.update {
                            it.copy(
                                locationState = state,
                                userLocation = newLoc,
                                hasLocationPermission = true,
                                isGpsEnabled = true
                            )
                        }

                        if (prevLat == null || prevLng == null) {
                            setupPoiMarkers(newLoc.latitude, newLoc.longitude)
                        }

                        lastKnownLat = newLoc.latitude
                        lastKnownLng = newLoc.longitude
                    }
                    is LocationState.Error.PermissionDenied -> {
                        _uiState.update {
                            it.copy(
                                locationState = state,
                                hasLocationPermission = false
                            )
                        }
                        stopContinuousTracking()
                    }
                    is LocationState.Error.GpsDisabled -> {
                        _uiState.update {
                            it.copy(
                                locationState = state,
                                isGpsEnabled = false
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(locationState = state) }
                    }
                }
            }
        }
    }

    fun stopContinuousTracking() {
        trackingJob?.cancel()
        _uiState.update { it.copy(isTracking = false) }
    }

    fun filterByCategory(category: MarkerCategory?) {
        _uiState.update { state ->
            val filtered = if (category == null) {
                state.markers
            } else {
                state.markers.filter { it.category == category }
            }
            state.copy(
                selectedCategory = category,
                filteredMarkers = filtered
            )
        }
    }

    fun selectMarker(marker: MapMarkerItem?) {
        _uiState.update { it.copy(selectedMarker = marker) }
    }

    private fun setupPoiMarkers(anchorLat: Double, anchorLng: Double) {
        val poiMarkers = listOf(
            MapMarkerItem(
                id = "poi-p1",
                position = LatLng(anchorLat + 0.0025, anchorLng + 0.0015),
                title = "Central Police Patrol Desk",
                snippet = "24x7 Emergency Patrol & First Response",
                category = MarkerCategory.POLICE_STATION
            ),
            MapMarkerItem(
                id = "poi-h1",
                position = LatLng(anchorLat - 0.004, anchorLng + 0.0045),
                title = "Metro Emergency Care Center",
                snippet = "Urgent Care & 24/7 Ambulance Fleet",
                category = MarkerCategory.HOSPITAL
            ),
            MapMarkerItem(
                id = "poi-sz1",
                position = LatLng(anchorLat + 0.004, anchorLng - 0.001),
                title = "Monitored Safe Haven",
                snippet = "Verified 24/7 Security Kiosk",
                category = MarkerCategory.SAFE_ZONE
            )
        )

        _uiState.update { state ->
            state.copy(
                markers = poiMarkers,
                filteredMarkers = if (state.selectedCategory != null) {
                    poiMarkers.filter { it.category == state.selectedCategory }
                } else {
                    poiMarkers
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopContinuousTracking()
    }

    class Factory(
        private val locationClient: LocationClient = SamrakshaApp.instance.locationClient,
        private val journeyRepository: JourneyRepository = SamrakshaApp.instance.journeyRepository,
        private val routingRepository: RoutingRepository? = SamrakshaApp.instance.routingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SafetyMapViewModel(locationClient, journeyRepository, routingRepository) as T
    }
}
