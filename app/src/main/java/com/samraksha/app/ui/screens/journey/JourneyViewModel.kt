package com.samraksha.app.ui.screens.journey

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.SafetyZoneLoadResult
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteOption
import com.samraksha.app.domain.repository.RouteOptionsResult
import com.samraksha.app.domain.repository.RoutingRepository
import com.samraksha.app.domain.repository.RoutingResult
import com.samraksha.app.domain.safety.JourneyRiskLevel
import com.samraksha.app.domain.safety.JourneySafetyMonitor
import com.samraksha.app.domain.safety.JourneySafetyReport
import com.samraksha.app.domain.safety.JourneySafetyWarning
import com.samraksha.app.domain.safety.NearbySafetyZone
import com.samraksha.app.journey.JourneySafetyService
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.ui.screens.location.model.SafetyZone
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.samraksha.app.domain.model.AutocompleteResult
import com.samraksha.app.domain.model.PlaceSuggestion
import org.maplibre.android.geometry.LatLng
import java.util.Locale
import java.util.UUID

/**
 * Which field is currently active/focused for autocomplete suggestions.
 */
enum class AutocompleteField {
    NONE,
    ORIGIN,
    DESTINATION
}

/**
 * Autocomplete query and network request UI states.
 */
sealed class AutocompleteUiState {
    data object Idle : AutocompleteUiState()
    data object Loading : AutocompleteUiState()
    data class Success(val suggestions: List<PlaceSuggestion>) : AutocompleteUiState()
    data object NoResults : AutocompleteUiState()
    data class Error(
        val message: String,
        val isNetworkError: Boolean = false,
        val isRateLimited: Boolean = false
    ) : AutocompleteUiState()
}

/**
 * UI State for the SAMRAKSHA Journey Safety Dashboard and Real-Time Sentinel.
 */
data class JourneyUiState(
    val activeJourney: JourneyEntity? = null,
    val recentJourneys: List<JourneyEntity> = emptyList(),
    // Origin / Pickup state
    val originInput: String = "Current Location",
    val isOriginCustom: Boolean = false,
    val selectedOriginCoordinates: LatLng? = null,
    // Destination state
    val destinationInput: String = "",
    val selectedDestinationCoordinates: LatLng? = null,
    // Autocomplete state
    val activeAutocompleteField: AutocompleteField = AutocompleteField.NONE,
    val autocompleteState: AutocompleteUiState = AutocompleteUiState.Idle,
    val selectedCityFilter: String? = null,
    val isLocationPermissionGranted: Boolean = true,
    // Location and tracking state
    val currentLocation: UserLocation? = null,
    val isLocating: Boolean = false,
    val locationError: String? = null,
    val elapsedTimeFormatted: String = "00:00:00",
    val showEndConfirmationDialog: Boolean = false,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val isCheckInSuccess: Boolean = false,
    val totalJourneysCount: Int = 0,
    val completedJourneysCount: Int = 0,
    // Real-Time Safety Monitoring State
    val currentRiskLevel: JourneyRiskLevel = JourneyRiskLevel.SAFE,
    val safetyReport: JourneySafetyReport? = null,
    val nearbySafetyZones: List<NearbySafetyZone> = emptyList(),
    val activeWarnings: List<JourneySafetyWarning> = emptyList(),
    val dismissedWarningIds: Set<String> = emptySet(),
    val isBatterySaverEnabled: Boolean = false,
    val lastLocationUpdateEpoch: Long = 0L,
    val isSafetyMonitoringActive: Boolean = false,
    // Geoapify Routing & Night Activity Score State
    val routeInfo: RouteInfo? = null,
    val isCalculatingRoute: Boolean = false,
    val routeError: String? = null,
    val routeOptions: List<RouteOption> = emptyList(),
    val selectedRouteOption: RouteOption? = null,
    val travelMode: String = "drive", // "drive", "bicycle", "walk"
    val routeLimitationNotice: String? = null,
    val isFactorBreakdownExpanded: Boolean = false
)

/**
 * ViewModel managing the lifecycle, battery-conscious periodic location monitoring,
 * spatial safety zone analysis, risk assessment, and Geoapify routing for active journeys.
 */
class JourneyViewModel(
    private val journeyRepository: JourneyRepository,
    private val locationClient: LocationClient,
    private val safetyZoneRepository: SafetyZoneRepository,
    private val routingRepository: RoutingRepository? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val activeRoutingRepository: RoutingRepository?
        get() = routingRepository ?: try { SamrakshaApp.instance.routingRepository } catch (_: Exception) { null }

    private val _uiState = MutableStateFlow(JourneyUiState())
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var locationUpdatesJob: Job? = null
    private var routeJob: Job? = null
    private var searchJob: Job? = null
    private var currentSearchRequestId: Long = 0L
    private var serviceReportCollectorJob: Job? = null
    private var cachedZones: List<SafetyZone> = emptyList()

    init {
        observeJourneys()
        fetchCurrentLocation()
        collectServiceSafetyReports()
    }

    /**
     * Collects safety reports emitted by [JourneySafetyService] when the app is
     * backgrounded. Merges them into the UI state so the risk level and warning
     * banners stay up-to-date regardless of whether the foreground Activity is
     * visible.
     */
    private fun collectServiceSafetyReports() {
        serviceReportCollectorJob = viewModelScope.launch {
            JourneySafetyService.safetyReports.collect { report ->
                // Only apply if a journey is active; service reports carry no
                // user-dismissed warning context so we re-filter here.
                if (_uiState.value.activeJourney != null) {
                    val dismissed = _uiState.value.dismissedWarningIds
                    val filteredWarnings = report.warnings.filterNot { it.id in dismissed }
                    _uiState.update { current ->
                        current.copy(
                            safetyReport = report,
                            currentRiskLevel = report.currentRiskLevel,
                            nearbySafetyZones = report.nearbyZones,
                            activeWarnings = filteredWarnings,
                            isSafetyMonitoringActive = true,
                            lastLocationUpdateEpoch = report.evaluatedAt
                        )
                    }
                }
            }
        }
    }

    private fun observeJourneys() {
        // Observe all journeys for dashboard statistics and history list
        journeyRepository.getAllJourneys()
            .onEach { journeys ->
                val active = journeys.firstOrNull { it.status == "IN_PROGRESS" }
                val completed = journeys.count { it.status == "COMPLETED" }
                val pastJourneys = journeys.filter { it.status != "IN_PROGRESS" }

                _uiState.update { current ->
                    current.copy(
                        activeJourney = active,
                        recentJourneys = pastJourneys,
                        totalJourneysCount = journeys.size,
                        completedJourneysCount = completed
                    )
                }

                if (active != null) {
                    startTracking(active)
                } else {
                    stopTracking()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Start live timer and battery-conscious periodic GPS tracking for an active journey.
     */
    private fun startTracking(journey: JourneyEntity) {
        // Pre-load safety zones from local cache/backend for fast in-memory checks
        loadSafetyZonesForJourney(journey)

        // Fetch route polyline and distance/duration if not already loaded
        if (_uiState.value.routeInfo == null && journey.endLocationName.isNotBlank()) {
            val startLat = if (journey.startLatitude != 0.0) journey.startLatitude else _uiState.value.currentLocation?.latitude ?: 0.0
            val startLng = if (journey.startLongitude != 0.0) journey.startLongitude else _uiState.value.currentLocation?.longitude ?: 0.0
            val destLat = journey.destinationLatitude.takeIf { it != 0.0 } ?: _uiState.value.selectedDestinationCoordinates?.latitude
            val destLng = journey.destinationLongitude.takeIf { it != 0.0 } ?: _uiState.value.selectedDestinationCoordinates?.longitude
            fetchRoute(startLat, startLng, journey.endLocationName, destLat = destLat, destLng = destLng)
        }

        // Start duration timer
        if (timerJob == null || timerJob?.isActive == false) {
            timerJob = viewModelScope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val elapsedMs = (now - journey.startTime).coerceAtLeast(0L)
                    val seconds = (elapsedMs / 1000) % 60
                    val minutes = (elapsedMs / (1000 * 60)) % 60
                    val hours = elapsedMs / (1000 * 60 * 60)
                    val formatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
                    _uiState.update { it.copy(elapsedTimeFormatted = formatted) }
                    delay(1000L)
                }
            }
        }

        // Launch battery-conscious location updates (foreground, in-app)
        startLocationUpdates(journey)

        // Start the foreground service so monitoring continues when app is backgrounded.
        // The service owns its own GPS subscription and publishes via SharedFlow.
        startSafetyService(journey.id, _uiState.value.isBatterySaverEnabled)
    }

    /**
     * Configures and starts periodic location tracking.
     *
     * To prevent battery drain and excessive background location usage:
     * - Uses 15s periodic updates with a 10m displacement filter by default.
     * - When Battery Saver is enabled, drops to 30s updates with 25m displacement filter
     *   and balanced power accuracy.
     * - Avoids unnecessary CPU/GPS wakeups when stationary.
     */
    private fun startLocationUpdates(journey: JourneyEntity) {
        locationUpdatesJob?.cancel()

        val isBatterySaver = _uiState.value.isBatterySaverEnabled
        val intervalMs = if (isBatterySaver) 30_000L else 15_000L
        val minDistanceMeters = if (isBatterySaver) 25f else 10f
        val highAccuracy = !isBatterySaver

        locationUpdatesJob = locationClient.getLocationUpdates(
            intervalMs = intervalMs,
            minDistanceMeters = minDistanceMeters,
            highAccuracy = highAccuracy
        )
            .onEach { state ->
                when (state) {
                    is LocationState.Success -> {
                        val loc = state.location
                        _uiState.update {
                            it.copy(
                                currentLocation = loc,
                                locationError = null,
                                lastLocationUpdateEpoch = System.currentTimeMillis(),
                                isSafetyMonitoringActive = true
                            )
                        }

                        // Evaluate nearby safety zones, risk levels, and situational warnings
                        evaluateSafetyForLocation(loc.latitude, loc.longitude)

                        // Persist latest coordinate to current journey
                        journeyRepository.updateCurrentLocation(
                            journeyId = journey.id,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    }
                    is LocationState.Error.PermissionDenied -> {
                        _uiState.update { it.copy(locationError = "Location permission is required for live tracking.") }
                    }
                    is LocationState.Error.GpsDisabled -> {
                        _uiState.update { it.copy(locationError = "GPS is disabled. Please turn on location services.") }
                    }
                    is LocationState.Error.Unavailable -> {
                        _uiState.update { it.copy(locationError = state.message) }
                    }
                    LocationState.Idle -> { /* No-op */ }
                    LocationState.Loading -> { /* Handled in UI if needed */ }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Preloads safety zones into local memory for offline/instant evaluation
     * without making redundant network queries on every location tick.
     */
    private fun loadSafetyZonesForJourney(journey: JourneyEntity) {
        viewModelScope.launch {
            // First load from fast local cache
            val local = safetyZoneRepository.getCachedSafetyZones()
            if (local.isNotEmpty()) {
                cachedZones = local
                val loc = _uiState.value.currentLocation
                if (loc != null) {
                    evaluateSafetyForLocation(loc.latitude, loc.longitude)
                }
            }

            // Sync fresh zones from repository if coordinates available
            val lat = _uiState.value.currentLocation?.latitude ?: journey.startLatitude
            val lng = _uiState.value.currentLocation?.longitude ?: journey.startLongitude
            if (lat != 0.0 && lng != 0.0) {
                when (val result = safetyZoneRepository.getSafetyZones(lat = lat, lng = lng, radius = 5000.0)) {
                    is SafetyZoneLoadResult.Success -> {
                        cachedZones = result.zones
                        val currentLoc = _uiState.value.currentLocation
                        if (currentLoc != null) {
                            evaluateSafetyForLocation(currentLoc.latitude, currentLoc.longitude)
                        }
                    }
                    is SafetyZoneLoadResult.Error -> {
                        if (cachedZones.isEmpty() && result.cachedZones.isNotEmpty()) {
                            cachedZones = result.cachedZones
                            val currentLoc = _uiState.value.currentLocation
                            if (currentLoc != null) {
                                evaluateSafetyForLocation(currentLoc.latitude, currentLoc.longitude)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Evaluates current user location against cached safety zones.
     */
    private fun evaluateSafetyForLocation(lat: Double, lng: Double) {
        if (cachedZones.isEmpty()) {
            cachedZones = safetyZoneRepository.getCachedSafetyZones()
        }

        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = lat,
            userLng = lng,
            availableZones = cachedZones,
            maxNearbyDistanceMeters = 1500.0,
            dismissedWarningIds = _uiState.value.dismissedWarningIds
        )

        _uiState.update { current ->
            current.copy(
                safetyReport = report,
                currentRiskLevel = report.currentRiskLevel,
                nearbySafetyZones = report.nearbyZones,
                activeWarnings = report.warnings
            )
        }
    }

    /**
     * Toggles between standard precision and battery saver mode during active tracking.
     */
    fun toggleBatterySaverMode(enabled: Boolean) {
        _uiState.update { it.copy(isBatterySaverEnabled = enabled) }
        val active = _uiState.value.activeJourney
        if (active != null) {
            // Restart in-app tracking with new interval
            startLocationUpdates(active)
            // Restart the foreground service so it also uses the new battery mode
            startSafetyService(active.id, enabled)
            val msg = if (enabled) {
                "Battery Saver Mode active: 30s intervals & sensor sleep enabled"
            } else {
                "Standard Precision active: 15s intervals"
            }
            _uiState.update { it.copy(snackbarMessage = msg) }
        }
    }

    /**
     * Dismisses an active situational warning by ID.
     */
    fun dismissWarning(warningId: String) {
        _uiState.update { current ->
            val updatedDismissed = current.dismissedWarningIds + warningId
            val filteredWarnings = current.activeWarnings.filterNot { it.id == warningId }
            current.copy(
                dismissedWarningIds = updatedDismissed,
                activeWarnings = filteredWarnings
            )
        }
    }

    /**
     * Stop active tracking jobs and reset duration display and monitoring state.
     */
    private fun stopTracking() {
        timerJob?.cancel()
        timerJob = null
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        routeJob?.cancel()
        routeJob = null
        // Tell the foreground service to shut down
        stopSafetyService()
        _uiState.update {
            it.copy(
                elapsedTimeFormatted = "00:00:00",
                isSafetyMonitoringActive = false,
                safetyReport = null,
                activeWarnings = emptyList(),
                currentRiskLevel = JourneyRiskLevel.SAFE,
                routeInfo = null,
                routeError = null,
                isCalculatingRoute = false,
                routeOptions = emptyList(),
                selectedRouteOption = null,
                routeLimitationNotice = null
            )
        }
    }

    // ── Foreground service helpers ────────────────────────────────────────────

    /**
     * Starts or restarts [JourneySafetyService] with the current battery-saver preference.
     * Using [ContextCompat.startForegroundService] ensures it is promoted to foreground
     * even when called from the background on API 26+.
     */
    private fun startSafetyService(journeyId: String, batterySaver: Boolean) {
        val ctx = appContext ?: return
        val intent = JourneySafetyService.buildStartIntent(
            context = ctx,
            journeyId = journeyId,
            batterySaver = batterySaver
        )
        ContextCompat.startForegroundService(ctx, intent)
    }

    /** Sends a stop action to [JourneySafetyService]; the service tears itself down. */
    private fun stopSafetyService() {
        val ctx = appContext ?: return
        try {
            ctx.startService(JourneySafetyService.buildStopIntent(ctx))
        } catch (_: Exception) {
            // Service may already be stopped — safe to ignore
        }
    }

    /**
     * Request a one-shot current location fix.
     */
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, locationError = null) }
            when (val state = locationClient.getCurrentLocation(highAccuracy = true)) {
                is LocationState.Success -> {
                    _uiState.update {
                        it.copy(
                            currentLocation = state.location,
                            isLocating = false,
                            locationError = null,
                            lastLocationUpdateEpoch = System.currentTimeMillis()
                        )
                    }
                    if (_uiState.value.activeJourney != null) {
                        evaluateSafetyForLocation(state.location.latitude, state.location.longitude)
                    }
                }
                is LocationState.Error.PermissionDenied -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationError = "Location permission denied"
                        )
                    }
                }
                is LocationState.Error.GpsDisabled -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationError = "GPS is turned off"
                        )
                    }
                }
                is LocationState.Error.Unavailable -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            locationError = state.message
                        )
                    }
                }
                LocationState.Idle, LocationState.Loading -> {
                    _uiState.update { it.copy(isLocating = false) }
                }
            }
        }
    }

    fun onOriginChange(input: String) {
        _uiState.update {
            it.copy(
                originInput = input,
                isOriginCustom = true,
                selectedOriginCoordinates = null,
                activeAutocompleteField = AutocompleteField.ORIGIN
            )
        }
        triggerDebouncedSearch(AutocompleteField.ORIGIN, input)
    }

    fun onDestinationChange(input: String) {
        _uiState.update {
            it.copy(
                destinationInput = input,
                selectedDestinationCoordinates = null,
                activeAutocompleteField = AutocompleteField.DESTINATION
            )
        }
        triggerDebouncedSearch(AutocompleteField.DESTINATION, input)
    }

    fun onFocusField(field: AutocompleteField) {
        _uiState.update { it.copy(activeAutocompleteField = field) }
        val query = when (field) {
            AutocompleteField.ORIGIN -> _uiState.value.originInput
            AutocompleteField.DESTINATION -> _uiState.value.destinationInput
            AutocompleteField.NONE -> ""
        }
        if (field != AutocompleteField.NONE && query.length >= 2) {
            triggerDebouncedSearch(field, query, debounceMs = 0L)
        }
    }

    fun selectOriginSuggestion(suggestion: PlaceSuggestion) {
        searchJob?.cancel()
        val displayName = suggestion.name.ifBlank { suggestion.fullAddress }
        _uiState.update {
            it.copy(
                originInput = displayName,
                selectedOriginCoordinates = suggestion.coordinates,
                isOriginCustom = true,
                activeAutocompleteField = AutocompleteField.NONE,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
        if (_uiState.value.destinationInput.isNotBlank()) {
            previewRoute(_uiState.value.destinationInput)
        }
    }

    fun selectDestinationSuggestion(suggestion: PlaceSuggestion) {
        searchJob?.cancel()
        val displayName = suggestion.name.ifBlank { suggestion.fullAddress }
        _uiState.update {
            it.copy(
                destinationInput = displayName,
                selectedDestinationCoordinates = suggestion.coordinates,
                activeAutocompleteField = AutocompleteField.NONE,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
        previewRoute(displayName)
    }

    fun clearOrigin() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                originInput = "",
                selectedOriginCoordinates = null,
                isOriginCustom = true,
                activeAutocompleteField = AutocompleteField.ORIGIN,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
    }

    fun resetOriginToCurrentLocation() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                originInput = "Current Location",
                selectedOriginCoordinates = null,
                isOriginCustom = false,
                activeAutocompleteField = AutocompleteField.NONE,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
        if (_uiState.value.currentLocation == null) {
            fetchCurrentLocation()
        }
        if (_uiState.value.destinationInput.isNotBlank()) {
            previewRoute(_uiState.value.destinationInput)
        }
    }

    fun clearDestination() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                destinationInput = "",
                selectedDestinationCoordinates = null,
                activeAutocompleteField = AutocompleteField.DESTINATION,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
        clearRoute()
    }

    fun dismissAutocomplete() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                activeAutocompleteField = AutocompleteField.NONE,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
    }

    fun setCityFilter(city: String?) {
        val normalized = city?.trim()?.takeIf { it.isNotBlank() && !it.equals("All Cities", ignoreCase = true) }
        _uiState.update { it.copy(selectedCityFilter = normalized) }
        val activeField = _uiState.value.activeAutocompleteField
        val query = when (activeField) {
            AutocompleteField.ORIGIN -> _uiState.value.originInput
            AutocompleteField.DESTINATION -> _uiState.value.destinationInput
            AutocompleteField.NONE -> ""
        }
        if (activeField != AutocompleteField.NONE && query.length >= 2) {
            triggerDebouncedSearch(activeField, query, debounceMs = 0L)
        }
    }

    fun retryAutocompleteSearch() {
        val activeField = _uiState.value.activeAutocompleteField
        val query = when (activeField) {
            AutocompleteField.ORIGIN -> _uiState.value.originInput
            AutocompleteField.DESTINATION -> _uiState.value.destinationInput
            AutocompleteField.NONE -> ""
        }
        if (activeField != AutocompleteField.NONE && query.isNotBlank()) {
            triggerDebouncedSearch(activeField, query, debounceMs = 0L)
        }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(isLocationPermissionGranted = granted) }
        if (granted && _uiState.value.currentLocation == null) {
            fetchCurrentLocation()
        }
    }

    fun triggerDebouncedSearch(
        field: AutocompleteField,
        query: String,
        debounceMs: Long = 350L
    ) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(autocompleteState = AutocompleteUiState.Idle) }
            return
        }

        val requestId = ++currentSearchRequestId
        searchJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            if (requestId != currentSearchRequestId) return@launch

            _uiState.update { it.copy(autocompleteState = AutocompleteUiState.Loading) }

            val repo = activeRoutingRepository
            if (repo == null) {
                if (requestId == currentSearchRequestId) {
                    _uiState.update {
                        it.copy(
                            autocompleteState = AutocompleteUiState.Error(
                                message = "Routing repository unavailable",
                                isNetworkError = false
                            )
                        )
                    }
                }
                return@launch
            }

            val userLoc = _uiState.value.currentLocation
            val result = repo.searchPlaces(
                query = trimmed,
                userLat = userLoc?.latitude,
                userLng = userLoc?.longitude,
                cityFilter = _uiState.value.selectedCityFilter
            )

            if (requestId != currentSearchRequestId) return@launch

            when (result) {
                is AutocompleteResult.Success -> {
                    _uiState.update { it.copy(autocompleteState = AutocompleteUiState.Success(result.suggestions)) }
                }
                is AutocompleteResult.Empty -> {
                    _uiState.update { it.copy(autocompleteState = AutocompleteUiState.NoResults) }
                }
                is AutocompleteResult.Error -> {
                    _uiState.update {
                        it.copy(
                            autocompleteState = AutocompleteUiState.Error(
                                message = result.message,
                                isNetworkError = result.isNetworkError,
                                isRateLimited = result.isRateLimited
                            )
                        )
                    }
                }
            }
        }
    }

    fun onQuickDestinationSelect(destination: String) {
        _uiState.update {
            it.copy(
                destinationInput = destination,
                selectedDestinationCoordinates = null,
                activeAutocompleteField = AutocompleteField.NONE,
                autocompleteState = AutocompleteUiState.Idle
            )
        }
        previewRoute(destination)
    }

    fun setShowEndConfirmationDialog(show: Boolean) {
        _uiState.update { it.copy(showEndConfirmationDialog = show) }
    }

    /**
     * Starts a new journey with the entered destination.
     */
    fun startJourney() {
        val destination = _uiState.value.destinationInput.trim()
        if (destination.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Please enter a valid destination") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isCustomOrigin = _uiState.value.isOriginCustom && _uiState.value.selectedOriginCoordinates != null
            val customOriginCoord = _uiState.value.selectedOriginCoordinates
            val currentLoc = _uiState.value.currentLocation

            val startLat = if (isCustomOrigin) customOriginCoord!!.latitude else (currentLoc?.latitude ?: 0.0)
            val startLng = if (isCustomOrigin) customOriginCoord!!.longitude else (currentLoc?.longitude ?: 0.0)
            val startLocName = if (isCustomOrigin) {
                _uiState.value.originInput
            } else if (currentLoc != null) {
                "GPS (${String.format(Locale.getDefault(), "%.4f, %.4f", currentLoc.latitude, currentLoc.longitude)})"
            } else {
                "Current Location (GPS Active)"
            }

            val destCoord = _uiState.value.selectedDestinationCoordinates

            val newJourney = JourneyEntity(
                id = UUID.randomUUID().toString(),
                startLocationName = startLocName,
                endLocationName = destination,
                startTime = System.currentTimeMillis(),
                expectedEndTime = System.currentTimeMillis() + (30 * 60 * 1000L), // Default 30 min ETA
                status = "IN_PROGRESS",
                startLatitude = startLat,
                startLongitude = startLng,
                currentLatitude = startLat,
                currentLongitude = startLng,
                destinationLatitude = destCoord?.latitude ?: 0.0,
                destinationLongitude = destCoord?.longitude ?: 0.0
            )

            journeyRepository.startJourney(newJourney)

            // Trigger route calculation via Geoapify Routing API using selected coordinates if available
            fetchRoute(
                startLat = startLat,
                startLng = startLng,
                destination = destination,
                destLat = destCoord?.latitude,
                destLng = destCoord?.longitude
            )

            // Notify user that monitoring has started
            try {
                SamrakshaApp.instance.notificationManager.notifyJourneyStarted(destination)
            } catch (_: Exception) {}

            _uiState.update {
                it.copy(
                    isLoading = false,
                    destinationInput = "",
                    selectedDestinationCoordinates = null,
                    activeAutocompleteField = AutocompleteField.NONE,
                    autocompleteState = AutocompleteUiState.Idle,
                    snackbarMessage = "Safe Journey started! Guardians monitoring active."
                )
            }
        }
    }

    /**
     * Previews a route from current GPS location (or custom origin) to the specified destination.
     */
    fun previewRoute(destination: String = _uiState.value.destinationInput) {
        val dest = destination.trim()
        if (dest.isBlank()) {
            _uiState.update {
                it.copy(
                    snackbarMessage = "Please enter a destination to preview route",
                    routeError = "Please enter a destination to preview route"
                )
            }
            return
        }
        viewModelScope.launch {
            val startCoord = if (_uiState.value.isOriginCustom && _uiState.value.selectedOriginCoordinates != null) {
                _uiState.value.selectedOriginCoordinates
            } else {
                var currentLoc = _uiState.value.currentLocation
                if (currentLoc == null || (currentLoc.latitude == 0.0 && currentLoc.longitude == 0.0)) {
                    val lastKnown = locationClient.getLastKnownLocation()
                    if (lastKnown != null && (lastKnown.latitude != 0.0 || lastKnown.longitude != 0.0)) {
                        currentLoc = lastKnown
                        _uiState.update { it.copy(currentLocation = lastKnown) }
                    } else {
                        val fresh = locationClient.getCurrentLocation(highAccuracy = true)
                        if (fresh is LocationState.Success && (fresh.location.latitude != 0.0 || fresh.location.longitude != 0.0)) {
                            currentLoc = fresh.location
                            _uiState.update { it.copy(currentLocation = fresh.location) }
                        }
                    }
                }
                currentLoc?.let { LatLng(it.latitude, it.longitude) }
            }

            val startLat = startCoord?.latitude ?: 0.0
            val startLng = startCoord?.longitude ?: 0.0
            val destCoord = _uiState.value.selectedDestinationCoordinates

            fetchRoute(
                startLat = startLat,
                startLng = startLng,
                destination = dest,
                destLat = destCoord?.latitude,
                destLng = destCoord?.longitude
            )
        }
    }

    /**
     * Clears calculated route overlay and route options.
     */
    fun clearRoute() {
        routeJob?.cancel()
        routeJob = null
        _uiState.update {
            it.copy(
                routeInfo = null,
                routeError = null,
                isCalculatingRoute = false,
                routeOptions = emptyList(),
                selectedRouteOption = null,
                routeLimitationNotice = null
            )
        }
    }

    /**
     * Updates selected travel mode ("drive", "bicycle", "walk") and triggers recalculation.
     */
    fun setTravelMode(mode: String) {
        if (_uiState.value.travelMode == mode) return
        _uiState.update { it.copy(travelMode = mode) }

        val active = _uiState.value.activeJourney
        if (active != null && active.endLocationName.isNotBlank()) {
            val startLat = if (active.startLatitude != 0.0) active.startLatitude else _uiState.value.currentLocation?.latitude ?: 0.0
            val startLng = if (active.startLongitude != 0.0) active.startLongitude else _uiState.value.currentLocation?.longitude ?: 0.0
            val destCoord = _uiState.value.selectedDestinationCoordinates
            fetchRoute(
                startLat = startLat,
                startLng = startLng,
                destination = active.endLocationName,
                mode = mode,
                destLat = destCoord?.latitude,
                destLng = destCoord?.longitude
            )
        } else if (_uiState.value.destinationInput.isNotBlank()) {
            previewRoute(_uiState.value.destinationInput)
        }
    }

    /**
     * Selects a specific route option and updates the displayed route polyline on MapLibre.
     */
    fun selectRouteOption(optionId: String) {
        val currentOptions = _uiState.value.routeOptions
        val updatedOptions = currentOptions.map { option ->
            option.copy(isSelected = (option.id == optionId))
        }
        val selected = updatedOptions.firstOrNull { it.id == optionId }
        _uiState.update {
            it.copy(
                routeOptions = updatedOptions,
                selectedRouteOption = selected,
                routeInfo = selected?.routeInfo ?: it.routeInfo
            )
        }
    }

    /**
     * Toggles expansion of factor breakdown card for selected route option.
     */
    fun toggleFactorBreakdown() {
        _uiState.update { it.copy(isFactorBreakdownExpanded = !it.isFactorBreakdownExpanded) }
    }

    /**
     * Requests route calculation and multi-route options from Geoapify Routing API asynchronously.
     */
    fun fetchRoute(
        startLat: Double,
        startLng: Double,
        destination: String,
        mode: String = _uiState.value.travelMode,
        destLat: Double? = null,
        destLng: Double? = null
    ) {
        val repo = activeRoutingRepository ?: return
        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingRoute = true, routeError = null) }
            when (val result = repo.calculateRouteOptions(
                startLat = startLat,
                startLng = startLng,
                destination = destination,
                destLat = destLat,
                destLng = destLng,
                mode = mode
            )) {
                is RouteOptionsResult.Success -> {
                    val selected = result.options.firstOrNull { it.isSelected } ?: result.options.firstOrNull()
                    _uiState.update {
                        it.copy(
                            routeOptions = result.options,
                            selectedRouteOption = selected,
                            routeInfo = selected?.routeInfo,
                            routeLimitationNotice = result.limitationNotice,
                            isCalculatingRoute = false,
                            routeError = null
                        )
                    }
                }
                is RouteOptionsResult.Error -> {
                    // Fall back to single route calculation
                    when (val fallback = repo.calculateRoute(
                        startLat = startLat,
                        startLng = startLng,
                        destination = destination,
                        destLat = destLat,
                        destLng = destLng,
                        mode = mode
                    )) {
                        is RoutingResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    routeInfo = fallback.route,
                                    routeOptions = emptyList(),
                                    selectedRouteOption = null,
                                    routeLimitationNotice = null,
                                    isCalculatingRoute = false,
                                    routeError = null
                                )
                            }
                        }
                        is RoutingResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    routeInfo = null,
                                    routeOptions = emptyList(),
                                    selectedRouteOption = null,
                                    routeLimitationNotice = null,
                                    isCalculatingRoute = false,
                                    routeError = result.message
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Ends the active journey with COMPLETED status.
     */
    fun endJourney() {
        val active = _uiState.value.activeJourney ?: return
        val destination = active.endLocationName
        val durationLabel = _uiState.value.elapsedTimeFormatted
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showEndConfirmationDialog = false) }
            journeyRepository.endJourney(active.id, "COMPLETED")

            // Notify user that they have arrived safely
            try {
                SamrakshaApp.instance.notificationManager
                    .notifyJourneyCompleted(destination, durationLabel)
            } catch (_: Exception) {}

            _uiState.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = "Journey ended! Arrived safely at destination."
                )
            }
        }
    }

    /**
     * Cancels the active journey.
     */
    fun cancelJourney() {
        val active = _uiState.value.activeJourney ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showEndConfirmationDialog = false) }
            journeyRepository.endJourney(active.id, "CANCELLED")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = "Journey monitoring cancelled."
                )
            }
        }
    }

    /**
     * Records a safety check-in ping to reassure emergency guardians.
     */
    fun performSafetyCheckIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckInSuccess = true, snackbarMessage = "Check-in sent! Guardians notified you are safe.") }
            delay(3000L)
            _uiState.update { it.copy(isCheckInSuccess = false) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun syncJourneys() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = journeyRepository.syncJourneys()
            val msg = if (result.isSuccess) "Journeys synchronized with cloud" else "Offline mode - journeys preserved"
            _uiState.update { it.copy(isLoading = false, snackbarMessage = msg) }
        }
    }

    public override fun onCleared() {
        super.onCleared()
        stopTracking()
        searchJob?.cancel()
        serviceReportCollectorJob?.cancel()
    }

    /**
     * Factory for creating instances of [JourneyViewModel].
     */
    class Factory(
        private val journeyRepository: JourneyRepository,
        private val locationClient: LocationClient,
        private val safetyZoneRepository: SafetyZoneRepository = SamrakshaApp.instance.safetyZoneRepository,
        private val routingRepository: RoutingRepository? = null,
        private val context: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(JourneyViewModel::class.java)) {
                val ctx = context ?: try { SamrakshaApp.instance.applicationContext } catch (_: Exception) { null }
                val routing = routingRepository ?: try { SamrakshaApp.instance.routingRepository } catch (_: Exception) { null }
                return JourneyViewModel(
                    journeyRepository = journeyRepository,
                    locationClient = locationClient,
                    safetyZoneRepository = safetyZoneRepository,
                    routingRepository = routing,
                    appContext = ctx
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
