package com.samraksha.app.ui.screens.location

import org.maplibre.android.geometry.LatLng
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.RouteOptionsResult
import com.samraksha.app.domain.repository.RoutingRepository
import com.samraksha.app.domain.repository.RoutingResult
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.ui.screens.location.model.MarkerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class SafetyMapViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeLocationClient: FakeMapLocationClient
    private lateinit var fakeJourneyRepo: FakeMapJourneyRepo
    private lateinit var fakeRoutingRepo: FakeMapRoutingRepo
    private lateinit var viewModel: SafetyMapViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeLocationClient = FakeMapLocationClient()
        fakeJourneyRepo = FakeMapJourneyRepo()
        fakeRoutingRepo = FakeMapRoutingRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SafetyMapViewModel {
        return SafetyMapViewModel(
            locationClient = fakeLocationClient,
            journeyRepository = fakeJourneyRepo,
            routingRepository = fakeRoutingRepo
        )
    }

    @Test
    fun activeJourney_calculatesAndDisplaysActiveRoute() = runTest(testDispatcher) {
        val testRoute = RouteInfo(
            points = listOf(LatLng(12.9716, 77.5946), LatLng(12.9800, 77.6000)),
            distanceMeters = 3200.0,
            durationSeconds = 750.0,
            startPoint = LatLng(12.9716, 77.5946),
            destinationPoint = LatLng(12.9800, 77.6000),
            destinationName = "Whitefield Safe Hub"
        )
        fakeRoutingRepo.result = RoutingResult.Success(testRoute)

        val activeJourney = JourneyEntity(
            id = "j1",
            startLatitude = 12.9716,
            startLongitude = 77.5946,
            startLocationName = "Current Location",
            destinationLatitude = 12.9800,
            destinationLongitude = 77.6000,
            endLocationName = "Whitefield Safe Hub",
            status = "IN_PROGRESS",
            startTime = System.currentTimeMillis()
        )
        fakeJourneyRepo.emitActiveJourney(activeJourney)

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.activeRoute)
        assertEquals(3200.0, state.activeRoute?.distanceMeters ?: 0.0, 0.1)
        assertEquals("Whitefield Safe Hub", state.activeJourneyDestination)
    }

    @Test
    fun activeJourney_whenNull_activeRouteIsNull() = runTest(testDispatcher) {
        fakeJourneyRepo.emitActiveJourney(null)

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.activeRoute)
        assertNull(state.activeJourneyDestination)
    }

    // ── Location Permission & Lifecycle Verification Tests ─────────────────────

    @Test
    fun permissionInitiallyDenied_stateIsPermissionDenied_trackingNotStarted() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = false

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.hasLocationPermission)
        assertTrue(state.locationState is LocationState.Error.PermissionDenied)
        assertFalse(state.isTracking)
        assertNull(state.userLocation)
    }

    @Test
    fun permissionGrantedWhileAppIsOpen_refreshesLocationAndStartsTracking() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = false

        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.hasLocationPermission)

        // User grants permission in runtime dialog
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.currentLocationResult = LocationState.Success(UserLocation(12.9716, 77.5946, 5f))

        viewModel.onPermissionResult(granted = true, isFineLocation = true)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasLocationPermission)
        assertTrue(state.isFineLocation)
        assertTrue(state.isTracking)
        assertTrue(state.locationState is LocationState.Success)
        assertEquals(12.9716, state.userLocation?.latitude ?: 0.0, 0.001)
    }

    @Test
    fun permissionGrantedInAndroidSettingsWhilePaused_resumesWithTracking() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = false

        viewModel = createViewModel()
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.hasLocationPermission)

        // Screen is paused as user navigates to Android Settings
        viewModel.onScreenPaused()
        assertFalse(viewModel.uiState.value.isTracking)

        // User enables location permission in Settings
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.currentLocationResult = LocationState.Success(UserLocation(12.9716, 77.5946, 5f))

        // Screen resumed
        viewModel.onScreenResumed(isFineLocation = true)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasLocationPermission)
        assertTrue(state.isTracking)
        assertTrue(state.locationState is LocationState.Success)
        assertEquals(12.9716, state.userLocation?.latitude ?: 0.0, 0.001)
    }

    @Test
    fun approximateLocation_grantedCoarseOnly_handledGracefully() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = true

        viewModel = createViewModel()
        testScheduler.runCurrent()

        // User grants only Approximate (COARSE) location
        viewModel.onPermissionResult(granted = true, isFineLocation = false)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasLocationPermission)
        assertFalse(state.isFineLocation)
        assertTrue(state.isTracking)
        assertTrue(state.locationState is LocationState.Success)
    }

    @Test
    fun locationServicesDisabled_stateIsGpsDisabled() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.gpsEnabled = false

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasLocationPermission)
        assertFalse(state.isGpsEnabled)
        assertTrue(state.locationState is LocationState.Error.GpsDisabled)
        assertFalse(state.isTracking)
    }

    @Test
    fun locationRequestFailureOrTimeout_stateIsUnavailableWithRetry() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.gpsEnabled = true
        fakeLocationClient.currentLocationResult = LocationState.Error.Unavailable("Satellite timeout")
        fakeLocationClient.lastKnownLocationResult = null

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasLocationPermission)
        assertTrue(state.locationState is LocationState.Error.Unavailable)
        assertEquals("Satellite timeout", (state.locationState as LocationState.Error.Unavailable).reason)

        // User hits Retry after GPS lock becomes available
        fakeLocationClient.currentLocationResult = LocationState.Success(UserLocation(12.9800, 77.6000, 8f))
        viewModel.refreshLocation()
        testScheduler.runCurrent()

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.locationState is LocationState.Success)
        assertEquals(12.9800, updatedState.userLocation?.latitude ?: 0.0, 0.001)
    }

    @Test
    fun leavingAndReturningToScreen_stopsAndResumesTrackingWithoutDuplicateListeners() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = true

        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isTracking)
        val initialSubCount = fakeLocationClient.locationUpdatesSubscriptionCount

        // Screen paused
        viewModel.onScreenPaused()
        assertFalse(viewModel.uiState.value.isTracking)

        // Screen resumed
        viewModel.onScreenResumed(isFineLocation = true)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isTracking)
        assertEquals(initialSubCount + 1, fakeLocationClient.locationUpdatesSubscriptionCount)

        // Multiple resume calls do not duplicate tracking job
        viewModel.onScreenResumed(isFineLocation = true)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isTracking)
        assertEquals(initialSubCount + 1, fakeLocationClient.locationUpdatesSubscriptionCount)
    }

    @Test
    fun permissionPermanentlyDenied_updatesPermanentlyDeniedFlag() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = false

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.onPermissionResult(granted = false, isPermanentlyDenied = true)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.hasLocationPermission)
        assertTrue(state.isPermanentlyDenied)
        assertTrue(state.locationState is LocationState.Error.PermissionDenied)
    }

    @Test
    fun locationUnavailable_doesNotSubstituteHardcodedCoordinates() = runTest(testDispatcher) {
        fakeLocationClient.permissionGranted = false
        fakeLocationClient.lastKnownLocationResult = null

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.userLocation)
        assertFalse(state.hasLocationPermission)
    }

    @Test
    fun poiMarkers_setupAndFilterByCategory() = runTest(testDispatcher) {
        val userLat = 13.0000
        val userLng = 77.6000
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.currentLocationResult = LocationState.Success(UserLocation(userLat, userLng, 5f))

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(3, state.markers.size)
        assertEquals(3, state.filteredMarkers.size)

        // Filter by POLICE_STATION
        viewModel.filterByCategory(MarkerCategory.POLICE_STATION)
        assertEquals(1, viewModel.uiState.value.filteredMarkers.size)
        assertEquals(MarkerCategory.POLICE_STATION, viewModel.uiState.value.filteredMarkers.first().category)

        // Select marker
        val selected = viewModel.uiState.value.filteredMarkers.first()
        viewModel.selectMarker(selected)
        assertEquals(selected, viewModel.uiState.value.selectedMarker)

        // Clear filter
        viewModel.filterByCategory(null)
        assertEquals(3, viewModel.uiState.value.filteredMarkers.size)

        // Deselect marker
        viewModel.selectMarker(null)
        assertNull(viewModel.uiState.value.selectedMarker)
    }
}

// ── Test Fakes ────────────────────────────────────────────────────────────────

private class FakeMapLocationClient : LocationClient {
    private val locationFlow = MutableSharedFlow<LocationState>(replay = 1)
    var permissionGranted: Boolean = true
    var gpsEnabled: Boolean = true
    var currentLocationResult: LocationState = LocationState.Success(UserLocation(12.9716, 77.5946, 10f))
    var lastKnownLocationResult: UserLocation? = UserLocation(12.9716, 77.5946, 10f)
    var locationUpdatesSubscriptionCount: Int = 0

    fun emitLocationUpdate(state: LocationState) {
        locationFlow.tryEmit(state)
    }

    override fun hasPermission(): Boolean = permissionGranted
    override fun isGpsEnabled(): Boolean = gpsEnabled
    override suspend fun getCurrentLocation(highAccuracy: Boolean): LocationState =
        if (!permissionGranted) LocationState.Error.PermissionDenied
        else if (!gpsEnabled) LocationState.Error.GpsDisabled
        else currentLocationResult

    override suspend fun getLastKnownLocation(): UserLocation? =
        if (!permissionGranted) null else lastKnownLocationResult

    override fun getLocationUpdates(intervalMs: Long, minDistanceMeters: Float, highAccuracy: Boolean): Flow<LocationState> {
        locationUpdatesSubscriptionCount++
        return locationFlow.asSharedFlow()
    }
}

private class FakeMapJourneyRepo : JourneyRepository {
    private val activeFlow = MutableStateFlow<JourneyEntity?>(null)
    fun emitActiveJourney(journey: JourneyEntity?) {
        activeFlow.value = journey
    }
    override fun getAllJourneys(): Flow<List<JourneyEntity>> = MutableStateFlow(emptyList())
    override fun getActiveJourney(): Flow<JourneyEntity?> = activeFlow.asStateFlow()
    override suspend fun startJourney(journey: JourneyEntity) {}
    override suspend fun updateJourneyStatus(journeyId: String, status: String) {}
    override suspend fun endJourney(journeyId: String, status: String) {}
    override suspend fun updateCurrentLocation(journeyId: String, latitude: Double, longitude: Double) {}
    override suspend fun deleteJourney(journeyId: String) {}
    override suspend fun syncJourneys(): Result<List<JourneyEntity>> = Result.success(emptyList())
}

private class FakeMapRoutingRepo : RoutingRepository {
    var result: RoutingResult = RoutingResult.Error("No route")
    var optionsResult: RouteOptionsResult = RouteOptionsResult.Error("No route options")
    override suspend fun calculateRoute(startLat: Double, startLng: Double, destination: String, destLat: Double?, destLng: Double?, mode: String): RoutingResult = result
    override suspend fun calculateRouteOptions(startLat: Double, startLng: Double, destination: String, destLat: Double?, destLng: Double?, mode: String): RouteOptionsResult = optionsResult
    override suspend fun geocodeAddress(address: String): LatLng? = null
    override suspend fun searchPlaces(query: String, userLat: Double?, userLng: Double?, cityFilter: String?): com.samraksha.app.domain.model.AutocompleteResult = com.samraksha.app.domain.model.AutocompleteResult.Empty
}
