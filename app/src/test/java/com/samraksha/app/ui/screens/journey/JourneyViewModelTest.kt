package com.samraksha.app.ui.screens.journey

import org.maplibre.android.geometry.LatLng
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.ActivityFactorScore
import com.samraksha.app.domain.model.ActivityFactorType
import com.samraksha.app.domain.model.ActivityLevel
import com.samraksha.app.domain.model.NightActivityScoreResult
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteOption
import com.samraksha.app.domain.model.ScoreConfidenceLevel
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.RouteOptionsResult
import com.samraksha.app.domain.repository.RoutingRepository
import com.samraksha.app.domain.repository.RoutingResult
import com.samraksha.app.domain.repository.SafetyZoneLoadResult
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.safety.JourneyRiskLevel
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.screens.location.model.SafetyZoneShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import com.samraksha.app.domain.model.AutocompleteResult
import com.samraksha.app.domain.model.PlaceSuggestion
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class JourneyViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeJourneyRepo: FakeJourneyRepository
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var fakeSafetyZoneRepo: FakeSafetyZoneRepository
    private lateinit var fakeRoutingRepo: FakeRoutingRepository
    private lateinit var viewModel: JourneyViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeJourneyRepo = FakeJourneyRepository()
        fakeLocationClient = FakeLocationClient()
        fakeSafetyZoneRepo = FakeSafetyZoneRepository()
        fakeRoutingRepo = FakeRoutingRepository()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.onCleared()
        }
        Dispatchers.resetMain()
    }

    private fun createViewModel(routingRepo: RoutingRepository? = fakeRoutingRepo): JourneyViewModel {
        return JourneyViewModel(
            journeyRepository = fakeJourneyRepo,
            locationClient = fakeLocationClient,
            safetyZoneRepository = fakeSafetyZoneRepo,
            routingRepository = routingRepo
        )
    }

    @Test
    fun initialState_loadsCorrectly() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.activeJourney)
        assertEquals(JourneyRiskLevel.SAFE, state.currentRiskLevel)
        assertFalse(state.isBatterySaverEnabled)
        assertFalse(state.isSafetyMonitoringActive)
    }

    @Test
    fun startJourney_initiatesActiveJourneyAndPeriodicTracking() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.onDestinationChange("Central Metro Station")
        viewModel.startJourney()
        testScheduler.runCurrent()

        val active = viewModel.uiState.value.activeJourney
        assertNotNull(active)
        assertEquals("Central Metro Station", active?.endLocationName)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun periodicLocationUpdate_evaluatesNearbyZonesAndRiskLevel() = runTest(testDispatcher) {
        // Seed an unsafe zone near target
        val unsafeZone = SafetyZone(
            id = "hazard-1",
            name = "Railway Underpass Hazard",
            level = SafetyZoneLevel.UNSAFE,
            shape = SafetyZoneShape.Circular(LatLng(12.9900, 77.6100), 200.0),
            centerPoint = LatLng(12.9900, 77.6100),
            safetyScore = 20,
            advisory = "Avoid after dark."
        )
        fakeSafetyZoneRepo.cachedZones = listOf(unsafeZone)

        // Add active journey
        val journey = JourneyEntity(
            id = "journey-1",
            startLocationName = "Start",
            endLocationName = "Dest",
            startTime = System.currentTimeMillis(),
            status = "IN_PROGRESS",
            startLatitude = 12.9900,
            startLongitude = 77.6100
        )
        fakeJourneyRepo.emitJourneys(listOf(journey))

        viewModel = createViewModel()
        testScheduler.runCurrent()

        // Emit location inside the unsafe zone
        val fix = UserLocation(
            latitude = 12.9900,
            longitude = 77.6100,
            accuracy = 5f,
            timestamp = System.currentTimeMillis()
        )
        fakeLocationClient.emitLocation(LocationState.Success(fix))
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(JourneyRiskLevel.HIGH, state.currentRiskLevel)
        assertTrue(state.activeWarnings.isNotEmpty())
        assertEquals(unsafeZone.name, state.safetyReport?.currentZone?.name)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun dismissWarning_removesWarningFromActiveList() = runTest(testDispatcher) {
        val unsafeZone = SafetyZone(
            id = "hazard-1",
            name = "Hazard Spot",
            level = SafetyZoneLevel.UNSAFE,
            shape = SafetyZoneShape.Circular(LatLng(12.9900, 77.6100), 200.0),
            centerPoint = LatLng(12.9900, 77.6100),
            safetyScore = 20,
            advisory = "Caution."
        )
        fakeSafetyZoneRepo.cachedZones = listOf(unsafeZone)

        val journey = JourneyEntity(
            id = "journey-1",
            startLocationName = "Start",
            endLocationName = "Dest",
            startTime = System.currentTimeMillis(),
            status = "IN_PROGRESS"
        )
        fakeJourneyRepo.emitJourneys(listOf(journey))

        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeLocationClient.emitLocation(LocationState.Success(UserLocation(12.9900, 77.6100, 5f)))
        testScheduler.runCurrent()

        val warnings = viewModel.uiState.value.activeWarnings
        assertTrue(warnings.isNotEmpty())
        val warnId = warnings.first().id

        viewModel.dismissWarning(warnId)
        testScheduler.runCurrent()

        val updatedWarnings = viewModel.uiState.value.activeWarnings
        assertTrue(updatedWarnings.none { it.id == warnId })

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun toggleBatterySaverMode_switchesTrackingConfiguration() = runTest(testDispatcher) {
        val journey = JourneyEntity(
            id = "journey-1",
            startLocationName = "Start",
            endLocationName = "Dest",
            startTime = System.currentTimeMillis(),
            status = "IN_PROGRESS"
        )
        fakeJourneyRepo.emitJourneys(listOf(journey))

        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isBatterySaverEnabled)

        viewModel.toggleBatterySaverMode(true)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isBatterySaverEnabled)
        // Verify battery-conscious parameters applied
        assertEquals(30_000L, fakeLocationClient.lastRequestedIntervalMs)
        assertEquals(25f, fakeLocationClient.lastMinDistanceMeters)
        assertEquals(false, fakeLocationClient.lastHighAccuracy)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun endJourney_stopsMonitoringAndResetsTrackingJobs() = runTest(testDispatcher) {
        val journey = JourneyEntity(
            id = "journey-1",
            startLocationName = "Start",
            endLocationName = "Dest",
            startTime = System.currentTimeMillis(),
            status = "IN_PROGRESS"
        )
        fakeJourneyRepo.emitJourneys(listOf(journey))

        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertNotNull(viewModel.uiState.value.activeJourney)

        viewModel.endJourney()
        testScheduler.runCurrent()

        // When journey is completed, active journey is cleared
        fakeJourneyRepo.emitJourneys(listOf(journey.copy(status = "COMPLETED")))
        testScheduler.runCurrent()

        assertNull(viewModel.uiState.value.activeJourney)
        assertFalse(viewModel.uiState.value.isSafetyMonitoringActive)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun previewRoute_withValidDestination_calculatesAndSetsRouteInfo() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val sampleRoute = RouteInfo(
            points = listOf(LatLng(12.9716, 77.5946), LatLng(12.9800, 77.6000)),
            distanceMeters = 2500.0,
            durationSeconds = 600.0,
            startPoint = LatLng(12.9716, 77.5946),
            destinationPoint = LatLng(12.9800, 77.6000),
            destinationName = "Indiranagar"
        )
        val scoreResult = NightActivityScoreResult(
            totalScore = 80,
            activityLevel = ActivityLevel.HIGH_ACTIVITY,
            confidenceLevel = ScoreConfidenceLevel.HIGH,
            confidenceDisclosure = "High confidence",
            factorScores = emptyList(),
            openPlacesCount = 4,
            unknownHoursCount = 0,
            totalPlacesEvaluated = 4
        )
        val option = RouteOption(
            id = "opt-1",
            label = "Route A (Balanced)",
            strategy = "balanced",
            routeInfo = sampleRoute,
            nightActivityScore = scoreResult,
            isSelected = true
        )

        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(option),
            isAlternativeAvailable = false,
            limitationNotice = "Single direct route available"
        )
        fakeRoutingRepo.routeResult = RoutingResult.Success(sampleRoute)
        fakeRoutingRepo.geocodeResult = LatLng(12.9800, 77.6000)

        viewModel.onDestinationChange("Indiranagar")
        viewModel.previewRoute()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.routeInfo)
        assertEquals(2500.0, state.routeInfo?.distanceMeters ?: 0.0, 0.1)
        assertEquals(1, state.routeOptions.size)
        assertEquals("opt-1", state.selectedRouteOption?.id)
        assertFalse(state.isCalculatingRoute)
        assertNull(state.routeError)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun selectRouteOption_switchesActiveRouteAndPolyline() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val routeA = RouteInfo(
            points = listOf(LatLng(12.9716, 77.5946), LatLng(12.9800, 77.6000)),
            distanceMeters = 2500.0,
            durationSeconds = 600.0,
            startPoint = LatLng(12.9716, 77.5946),
            destinationPoint = LatLng(12.9800, 77.6000),
            destinationName = "Destination"
        )
        val routeB = RouteInfo(
            points = listOf(LatLng(12.9716, 77.5946), LatLng(12.9850, 77.6050)),
            distanceMeters = 2100.0,
            durationSeconds = 540.0,
            startPoint = LatLng(12.9716, 77.5946),
            destinationPoint = LatLng(12.9850, 77.6050),
            destinationName = "Destination"
        )
        val dummyScore = NightActivityScoreResult(
            totalScore = 75,
            activityLevel = ActivityLevel.HIGH_ACTIVITY,
            confidenceLevel = ScoreConfidenceLevel.HIGH,
            confidenceDisclosure = "High confidence",
            factorScores = emptyList(),
            openPlacesCount = 3,
            unknownHoursCount = 0,
            totalPlacesEvaluated = 3
        )
        val optionA = RouteOption("opt-A", "Route A", "balanced", routeA, dummyScore, isSelected = true)
        val optionB = RouteOption("opt-B", "Route B", "short", routeB, dummyScore, isSelected = false)

        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(optionA, optionB),
            isAlternativeAvailable = true
        )

        viewModel.fetchRoute(12.9716, 77.5946, "Destination")
        testScheduler.runCurrent()

        assertEquals(2, viewModel.uiState.value.routeOptions.size)
        assertEquals("opt-A", viewModel.uiState.value.selectedRouteOption?.id)
        assertEquals(2500.0, viewModel.uiState.value.routeInfo?.distanceMeters ?: 0.0, 0.1)

        // Switch to Route B
        viewModel.selectRouteOption("opt-B")
        testScheduler.runCurrent()

        val updated = viewModel.uiState.value
        assertEquals("opt-B", updated.selectedRouteOption?.id)
        assertEquals(2100.0, updated.routeInfo?.distanceMeters ?: 0.0, 0.1)
        assertTrue(updated.routeOptions.first { it.id == "opt-B" }.isSelected)
        assertFalse(updated.routeOptions.first { it.id == "opt-A" }.isSelected)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun setTravelMode_updatesModeAndTriggersRecalculation() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.onDestinationChange("Central Park")
        viewModel.setTravelMode("bicycle")
        testScheduler.runCurrent()

        assertEquals("bicycle", viewModel.uiState.value.travelMode)
        assertEquals("bicycle", fakeRoutingRepo.lastMode)

        viewModel.setTravelMode("walk")
        testScheduler.runCurrent()

        assertEquals("walk", viewModel.uiState.value.travelMode)
        assertEquals("walk", fakeRoutingRepo.lastMode)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun toggleFactorBreakdown_togglesStateCorrectly() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isFactorBreakdownExpanded)
        viewModel.toggleFactorBreakdown()
        assertTrue(viewModel.uiState.value.isFactorBreakdownExpanded)
        viewModel.toggleFactorBreakdown()
        assertFalse(viewModel.uiState.value.isFactorBreakdownExpanded)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun previewRoute_withBlankDestination_setsError() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.onDestinationChange("")
        viewModel.previewRoute()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.routeInfo)
        assertEquals("Please enter a destination to preview route", state.routeError)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_successfulQuery_returnsSuggestionsWithCoordinates() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val suggestion1 = PlaceSuggestion(
            id = "place-1",
            name = "MG Road Metro Station",
            fullAddress = "MG Road, Bengaluru, Karnataka",
            locality = "Bengaluru",
            coordinates = LatLng(12.9750, 77.6050),
            distanceMeters = 1250.0
        )
        val suggestion2 = PlaceSuggestion(
            id = "place-2",
            name = "MG Road Boulevard",
            fullAddress = "MG Road Boulevard, Bengaluru, Karnataka",
            locality = "Bengaluru",
            coordinates = LatLng(12.9755, 77.6060),
            distanceMeters = 1400.0
        )
        fakeRoutingRepo.searchResult = AutocompleteResult.Success(listOf(suggestion1, suggestion2))

        viewModel.onDestinationChange("MG Road")
        testScheduler.advanceTimeBy(350L)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("MG Road", state.destinationInput)
        assertEquals(AutocompleteField.DESTINATION, state.activeAutocompleteField)
        assertTrue(state.autocompleteState is AutocompleteUiState.Success)

        val success = state.autocompleteState as AutocompleteUiState.Success
        assertEquals(2, success.suggestions.size)
        assertEquals("MG Road Metro Station", success.suggestions[0].name)
        assertEquals(LatLng(12.9750, 77.6050), success.suggestions[0].coordinates)
        assertEquals("1.3 km away", success.suggestions[0].formattedDistance)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_selectDestinationSuggestion_retainsCoordinatesAndTriggersRoute() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val suggestion = PlaceSuggestion(
            id = "place-dest",
            name = "Indiranagar 100ft Road",
            fullAddress = "100 Feet Rd, Indiranagar, Bengaluru",
            locality = "Indiranagar",
            coordinates = LatLng(12.9784, 77.6408),
            distanceMeters = 3200.0
        )

        viewModel.selectDestinationSuggestion(suggestion)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Indiranagar 100ft Road", state.destinationInput)
        assertEquals(LatLng(12.9784, 77.6408), state.selectedDestinationCoordinates)
        assertEquals(AutocompleteField.NONE, state.activeAutocompleteField)
        assertEquals(AutocompleteUiState.Idle, state.autocompleteState)

        // Route calculation was automatically invoked with destination coordinates
        assertEquals(12.9784, fakeRoutingRepo.lastDestLat ?: 0.0, 0.0001)
        assertEquals(77.6408, fakeRoutingRepo.lastDestLng ?: 0.0, 0.0001)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_selectOriginSuggestion_retainsCoordinates() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val originSuggestion = PlaceSuggestion(
            id = "place-orig",
            name = "Koramangala 5th Block",
            fullAddress = "Koramangala 5th Block, Bengaluru",
            locality = "Koramangala",
            coordinates = LatLng(12.9352, 77.6245),
            distanceMeters = 4500.0
        )

        viewModel.selectOriginSuggestion(originSuggestion)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Koramangala 5th Block", state.originInput)
        assertTrue(state.isOriginCustom)
        assertEquals(LatLng(12.9352, 77.6245), state.selectedOriginCoordinates)
        assertEquals(AutocompleteField.NONE, state.activeAutocompleteField)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_emptyResults_showsNoResultsState() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeRoutingRepo.searchResult = AutocompleteResult.Empty

        viewModel.onDestinationChange("NonExistentPlaceXYZ123")
        testScheduler.advanceTimeBy(350L)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.autocompleteState is AutocompleteUiState.NoResults)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_networkError_showsErrorState() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeRoutingRepo.searchResult = AutocompleteResult.Error("Unable to connect to Geocoding API", isNetworkError = true)

        viewModel.onDestinationChange("Central Station")
        testScheduler.advanceTimeBy(350L)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.autocompleteState is AutocompleteUiState.Error)
        val error = state.autocompleteState as AutocompleteUiState.Error
        assertEquals("Unable to connect to Geocoding API", error.message)
        assertTrue(error.isNetworkError)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_missingGpsPermission_allowsSearchWithoutDistances() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.setLocationPermissionGranted(false)
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isLocationPermissionGranted)

        val suggestion = PlaceSuggestion(
            id = "place-no-gps",
            name = "City Central Library",
            fullAddress = "Cubbon Park, Bengaluru",
            coordinates = LatLng(12.9733, 77.5912),
            distanceMeters = null
        )
        fakeRoutingRepo.searchResult = AutocompleteResult.Success(listOf(suggestion))

        viewModel.onDestinationChange("Central Library")
        testScheduler.advanceTimeBy(350L)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.autocompleteState is AutocompleteUiState.Success)
        val item = (state.autocompleteState as AutocompleteUiState.Success).suggestions.first()
        assertNull(item.distanceMeters)
        assertNull(item.formattedDistance)
        assertEquals("City Central Library", item.name)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun autocomplete_debounce_cancelsPendingSearchOnNewInput() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeRoutingRepo.searchResult = AutocompleteResult.Success(emptyList())

        // User types rapid keystrokes within debounce window (< 350ms)
        viewModel.onDestinationChange("Ai")
        testScheduler.advanceTimeBy(100L)
        viewModel.onDestinationChange("Air")
        testScheduler.advanceTimeBy(100L)
        viewModel.onDestinationChange("Airp")
        testScheduler.advanceTimeBy(100L)
        viewModel.onDestinationChange("Airport")
        testScheduler.runCurrent()

        // Before debounce fires, searchPlaces hasn't been called
        assertEquals(0, fakeRoutingRepo.searchCallCount)

        // Wait for debounce to complete
        testScheduler.advanceTimeBy(350L)
        testScheduler.runCurrent()

        // Exactly one search executed for the final query
        assertEquals(1, fakeRoutingRepo.searchCallCount)
        assertEquals("Airport", fakeRoutingRepo.lastSearchQuery)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun routeCalculation_usesSelectedCoordinatesWithoutRegeocoding() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        try {
            val selectedDest = PlaceSuggestion(
                id = "dest-1",
                name = "Kempegowda Airport",
                fullAddress = "Devanahalli, Bengaluru",
                coordinates = LatLng(13.1986, 77.7066),
                distanceMeters = 35000.0
            )
            viewModel.selectDestinationSuggestion(selectedDest)
            testScheduler.runCurrent()

            viewModel.startJourney()
            testScheduler.runCurrent()

            // Verify route calculation passed selected coordinates directly
            assertEquals(13.1986, fakeRoutingRepo.lastDestLat ?: 0.0, 0.0001)
            assertEquals(77.7066, fakeRoutingRepo.lastDestLng ?: 0.0, 0.0001)
        } finally {
            viewModel.onCleared()
            testScheduler.runCurrent()
        }
    }

    @Test
    fun autocomplete_cityFilter_passesCityToSearchPlaces() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeRoutingRepo.searchResult = AutocompleteResult.Empty

        viewModel.setCityFilter("Mumbai")
        viewModel.onDestinationChange("Marine Drive")
        testScheduler.advanceTimeBy(350L)
        testScheduler.runCurrent()

        assertEquals("Mumbai", fakeRoutingRepo.lastSearchCityFilter)
        assertEquals("Marine Drive", fakeRoutingRepo.lastSearchQuery)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    private fun createSampleRouteOption(
        id: String,
        label: String,
        strategy: String,
        score: Int,
        distanceMeters: Double = 5000.0,
        durationSeconds: Double = 600.0,
        isSelected: Boolean = false
    ): RouteOption {
        val points = listOf(
            LatLng(12.9716, 77.5946),
            LatLng(12.9750, 77.6000),
            LatLng(12.9800, 77.6100)
        )
        val routeInfo = RouteInfo(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            startPoint = LatLng(12.9716, 77.5946),
            destinationPoint = LatLng(12.9800, 77.6100),
            destinationName = "Sample Destination"
        )
        val factorScores = ActivityFactorType.entries.map { factor ->
            ActivityFactorScore(
                factor = factor,
                earnedPoints = (factor.maxWeight * (score / 100.0)),
                maxPoints = factor.maxWeight,
                rawMetricDisplay = "Sample Metric",
                detail = "Sample factor detail for ${factor.title}"
            )
        }
        val scoreResult = NightActivityScoreResult(
            totalScore = score,
            activityLevel = if (score >= 70) ActivityLevel.HIGH_ACTIVITY else if (score >= 40) ActivityLevel.MODERATE_ACTIVITY else ActivityLevel.LIMITED_ACTIVITY,
            confidenceLevel = ScoreConfidenceLevel.HIGH,
            confidenceDisclosure = "High confidence data available.",
            factorScores = factorScores,
            openPlacesCount = 12,
            unknownHoursCount = 2,
            totalPlacesEvaluated = 14
        )
        return RouteOption(
            id = id,
            label = label,
            strategy = strategy,
            routeInfo = routeInfo,
            nightActivityScore = scoreResult,
            isSelected = isSelected
        )
    }

    @Test
    fun previewRoute_loadsRouteOptions_withNightActivityScores() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val optionA = createSampleRouteOption("opt-1", "Route A (Balanced)", "balanced", 85, isSelected = true)
        val optionB = createSampleRouteOption("opt-2", "Route B (Shortest)", "short", 62, isSelected = false)
        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(optionA, optionB),
            isAlternativeAvailable = true
        )

        viewModel.previewRoute("Indiranagar")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(2, state.routeOptions.size)
        assertNotNull(state.selectedRouteOption)
        assertEquals("opt-1", state.selectedRouteOption?.id)
        assertEquals(85, state.selectedRouteOption?.nightActivityScore?.totalScore)
        assertEquals(ActivityLevel.HIGH_ACTIVITY, state.selectedRouteOption?.nightActivityScore?.activityLevel)
        assertEquals(ScoreConfidenceLevel.HIGH, state.selectedRouteOption?.nightActivityScore?.confidenceLevel)
        assertNotNull(state.routeInfo)
        assertFalse(state.isCalculatingRoute)
        assertNull(state.routeError)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun selectRouteOption_updatesSelectedOption_andSynchronizesRouteInfoAndPolyline() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val optionA = createSampleRouteOption("opt-1", "Route A (Balanced)", "balanced", 85, distanceMeters = 5200.0, isSelected = true)
        val optionB = createSampleRouteOption("opt-2", "Route B (Shortest)", "short", 62, distanceMeters = 4800.0, isSelected = false)
        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(optionA, optionB),
            isAlternativeAvailable = true
        )

        viewModel.previewRoute("Indiranagar")
        testScheduler.runCurrent()

        // User selects Route B
        viewModel.selectRouteOption("opt-2")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("opt-2", state.selectedRouteOption?.id)
        assertTrue(state.routeOptions.first { it.id == "opt-2" }.isSelected)
        assertFalse(state.routeOptions.first { it.id == "opt-1" }.isSelected)

        // Verifies route polyline, distance, duration are synchronized with selected route
        assertEquals(optionB.routeInfo, state.routeInfo)
        assertEquals(optionB.routeInfo.points, state.routeInfo?.points)
        assertEquals(4800.0, state.routeInfo?.distanceMeters ?: 0.0, 0.01)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun travelMode_change_triggersScoreRefreshWithSelectedMode() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val optionBicycle = createSampleRouteOption("bike-1", "Bicycle Route", "balanced", 78, isSelected = true)
        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(optionBicycle),
            isAlternativeAvailable = false
        )

        viewModel.onDestinationChange("MG Road")
        viewModel.setTravelMode("bicycle")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("bicycle", state.travelMode)
        assertEquals("bicycle", fakeRoutingRepo.lastMode)
        assertEquals("MG Road", fakeRoutingRepo.lastDestination)
        assertEquals("bike-1", state.selectedRouteOption?.id)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun calculateRouteOptions_singleRoute_displaysSingleRouteLimitationNotice() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val singleOption = createSampleRouteOption("single-1", "Direct Highway Path", "balanced", 74, isSelected = true)
        val notice = "Single direct route available for this transit corridor. Geoapify found no distinct alternative paths."
        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(singleOption),
            isAlternativeAvailable = false,
            limitationNotice = notice
        )

        viewModel.previewRoute("Hosur Highway")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, state.routeOptions.size)
        assertEquals(notice, state.routeLimitationNotice)
        assertEquals("single-1", state.selectedRouteOption?.id)
        assertFalse(state.isCalculatingRoute)
        assertNull(state.routeError)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun calculateRouteOptions_errorState_setsRouteErrorGracefully() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Error("Routing service quota exceeded.")
        fakeRoutingRepo.routeResult = RoutingResult.Error("Routing service quota exceeded.")

        viewModel.previewRoute("Remote Hill Station")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Routing service quota exceeded.", state.routeError)
        assertFalse(state.isCalculatingRoute)
        assertTrue(state.routeOptions.isEmpty())
        assertNull(state.selectedRouteOption)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun selectedRouteScore_matchesJourneyMapPolylineConsistency() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val option = createSampleRouteOption("opt-match", "Route Consistent", "balanced", 91, isSelected = true)
        fakeRoutingRepo.routeOptionsResult = RouteOptionsResult.Success(
            options = listOf(option),
            isAlternativeAvailable = false
        )

        viewModel.previewRoute("Brigade Road")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        // Ensure selected route option score and the journey route polyline are strictly identical
        assertEquals(91, state.selectedRouteOption?.nightActivityScore?.totalScore)
        assertEquals(state.selectedRouteOption?.routeInfo?.points, state.routeInfo?.points)
        assertEquals(state.selectedRouteOption?.routeInfo?.distanceMeters, state.routeInfo?.distanceMeters)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }

    @Test
    fun toggleFactorBreakdown_togglesUiState() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isFactorBreakdownExpanded)

        viewModel.toggleFactorBreakdown()
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isFactorBreakdownExpanded)

        viewModel.toggleFactorBreakdown()
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isFactorBreakdownExpanded)

        viewModel.onCleared()
        testScheduler.runCurrent()
    }
}

// ── Test Fakes ────────────────────────────────────────────────────────────────

private class FakeJourneyRepository : JourneyRepository {
    private val journeysFlow = MutableStateFlow<List<JourneyEntity>>(emptyList())

    fun emitJourneys(list: List<JourneyEntity>) {
        journeysFlow.value = list
    }

    override fun getAllJourneys(): Flow<List<JourneyEntity>> {
        return journeysFlow.asStateFlow()
    }

    override fun getActiveJourney(): Flow<JourneyEntity?> {
        return journeysFlow.map { list -> list.firstOrNull { it.status == "IN_PROGRESS" } }
    }

    override suspend fun startJourney(journey: JourneyEntity) {
        journeysFlow.value = journeysFlow.value + journey
    }

    override suspend fun updateJourneyStatus(journeyId: String, status: String) {
        journeysFlow.value = journeysFlow.value.map {
            if (it.id == journeyId) it.copy(status = status) else it
        }
    }

    override suspend fun endJourney(journeyId: String, status: String) {
        journeysFlow.value = journeysFlow.value.map {
            if (it.id == journeyId) it.copy(status = status) else it
        }
    }

    override suspend fun updateCurrentLocation(journeyId: String, latitude: Double, longitude: Double) {
        journeysFlow.value = journeysFlow.value.map {
            if (it.id == journeyId) it.copy(currentLatitude = latitude, currentLongitude = longitude) else it
        }
    }

    override suspend fun deleteJourney(journeyId: String) {
        journeysFlow.value = journeysFlow.value.filterNot { it.id == journeyId }
    }

    override suspend fun syncJourneys(): Result<List<JourneyEntity>> {
        return Result.success(journeysFlow.value)
    }
}

private class FakeLocationClient : LocationClient {
    private val locationFlow = MutableSharedFlow<LocationState>(replay = 1)
    var lastRequestedIntervalMs: Long? = null
    var lastMinDistanceMeters: Float? = null
    var lastHighAccuracy: Boolean? = null

    fun emitLocation(state: LocationState) {
        locationFlow.tryEmit(state)
    }

    override fun hasPermission(): Boolean = true
    override fun isGpsEnabled(): Boolean = true

    override suspend fun getCurrentLocation(highAccuracy: Boolean): LocationState {
        return LocationState.Success(UserLocation(12.9716, 77.5946, 10f))
    }

    override suspend fun getLastKnownLocation(): UserLocation? = null

    override fun getLocationUpdates(
        intervalMs: Long,
        minDistanceMeters: Float,
        highAccuracy: Boolean
    ): Flow<LocationState> {
        lastRequestedIntervalMs = intervalMs
        lastMinDistanceMeters = minDistanceMeters
        lastHighAccuracy = highAccuracy
        return locationFlow.asSharedFlow()
    }
}

private class FakeSafetyZoneRepository : SafetyZoneRepository {
    var cachedZones: List<SafetyZone> = emptyList()

    override suspend fun getSafetyZones(
        lat: Double?,
        lng: Double?,
        radius: Double?,
        riskLevel: String?,
        forceRefresh: Boolean
    ): SafetyZoneLoadResult {
        return SafetyZoneLoadResult.Success(cachedZones, isFromCache = true)
    }

    override fun getCachedSafetyZones(): List<SafetyZone> {
        return cachedZones
    }

    override suspend fun syncSafetyZones(): Result<List<SafetyZone>> {
        return Result.success(cachedZones)
    }
}

private class FakeRoutingRepository : RoutingRepository {
    var routeResult: RoutingResult = RoutingResult.Error("No route configured")
    var routeOptionsResult: RouteOptionsResult = RouteOptionsResult.Error("No route options configured")
    var geocodeResult: LatLng? = null
    var lastMode: String? = null
    var lastStartLat: Double? = null
    var lastStartLng: Double? = null
    var lastDestination: String? = null
    var lastDestLat: Double? = null
    var lastDestLng: Double? = null

    var searchResult: AutocompleteResult = AutocompleteResult.Empty
    var lastSearchQuery: String? = null
    var lastSearchUserLat: Double? = null
    var lastSearchUserLng: Double? = null
    var lastSearchCityFilter: String? = null
    var searchCallCount: Int = 0

    override suspend fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destination: String,
        destLat: Double?,
        destLng: Double?,
        mode: String
    ): RoutingResult {
        lastMode = mode
        lastStartLat = startLat
        lastStartLng = startLng
        lastDestination = destination
        lastDestLat = destLat
        lastDestLng = destLng
        return routeResult
    }

    override suspend fun calculateRouteOptions(
        startLat: Double,
        startLng: Double,
        destination: String,
        destLat: Double?,
        destLng: Double?,
        mode: String
    ): RouteOptionsResult {
        lastMode = mode
        lastStartLat = startLat
        lastStartLng = startLng
        lastDestination = destination
        lastDestLat = destLat
        lastDestLng = destLng
        return routeOptionsResult
    }

    override suspend fun geocodeAddress(address: String): LatLng? = geocodeResult

    override suspend fun searchPlaces(
        query: String,
        userLat: Double?,
        userLng: Double?,
        cityFilter: String?
    ): AutocompleteResult {
        searchCallCount++
        lastSearchQuery = query
        lastSearchUserLat = userLat
        lastSearchUserLng = userLng
        lastSearchCityFilter = cityFilter
        return searchResult
    }
}

