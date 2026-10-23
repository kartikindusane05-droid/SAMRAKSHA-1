package com.samraksha.app.ui.screens.home

import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeUserRepository: FakeHomeUserRepository
    private lateinit var fakeContactRepository: FakeHomeEmergencyContactRepository
    private lateinit var fakeJourneyRepository: FakeHomeJourneyRepository
    private lateinit var fakeLocationClient: FakeHomeLocationClient
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeUserRepository = FakeHomeUserRepository()
        fakeContactRepository = FakeHomeEmergencyContactRepository()
        fakeJourneyRepository = FakeHomeJourneyRepository()
        fakeLocationClient = FakeHomeLocationClient()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            userRepository = fakeUserRepository,
            emergencyContactRepository = fakeContactRepository,
            journeyRepository = fakeJourneyRepository,
            locationClient = fakeLocationClient
        )
    }

    @Test
    fun initialState_loadsUserAndContactCount() = runTest {
        val testUser = User(
            id = "user_123",
            fullName = "Priya Sharma",
            email = "priya@example.com",
            phone = "+919876543210"
        )
        fakeUserRepository.setUser(testUser)

        val contact1 = EmergencyContact(id = "c1", name = "Mother", phoneNumber = "+919876543211", relationship = "Parent")
        val contact2 = EmergencyContact(id = "c2", name = "Friend", phoneNumber = "+919876543212", relationship = "Friend")
        fakeContactRepository.setContacts(listOf(contact1, contact2))

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Priya Sharma", state.userName)
        assertEquals(2, state.contactCount)
    }

    @Test
    fun observeContacts_updatesContactCountReactively() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.contactCount)

        val newContact = EmergencyContact(id = "c1", name = "Guardian", phoneNumber = "+919876543211", relationship = "Parent")
        fakeContactRepository.addContact(newContact)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.contactCount)
    }

    @Test
    fun observeActiveJourney_updatesActiveJourneyState() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isJourneyActive)
        assertNull(viewModel.uiState.value.activeJourneyDestination)

        val activeJourney = JourneyEntity(
            id = "j_100",
            startLocationName = "Home",
            endLocationName = "Koramangala Metro",
            startTime = System.currentTimeMillis(),
            startLatitude = 12.9716,
            startLongitude = 77.5946,
            destinationLatitude = 12.9352,
            destinationLongitude = 77.6245,
            status = "IN_PROGRESS"
        )
        fakeJourneyRepository.setActiveJourney(activeJourney)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isJourneyActive)
        assertEquals("Koramangala Metro", viewModel.uiState.value.activeJourneyDestination)
    }

    @Test
    fun refreshLocationState_whenPermissionDenied_updatesLocationStatusToPermissionNeeded() = runTest {
        fakeLocationClient.permissionGranted = false
        fakeLocationClient.gpsEnabled = true

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(LocationStatus.PERMISSION_NEEDED, viewModel.uiState.value.locationStatus)
        assertFalse(viewModel.uiState.value.isLocationReady)
    }

    @Test
    fun refreshLocationState_whenGpsDisabled_updatesLocationStatusToGpsDisabled() = runTest {
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.gpsEnabled = false

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(LocationStatus.GPS_DISABLED, viewModel.uiState.value.locationStatus)
        assertFalse(viewModel.uiState.value.isLocationReady)
    }

    @Test
    fun refreshLocationState_whenLocationReady_updatesLocationStatusToReadyAndSetsCoordinates() = runTest {
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.gpsEnabled = true
        val mockLoc = UserLocation(
            latitude = 12.9716,
            longitude = 77.5946,
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )
        fakeLocationClient.mockLocation = mockLoc

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(LocationStatus.READY, viewModel.uiState.value.locationStatus)
        assertTrue(viewModel.uiState.value.isLocationReady)
        assertNotNull(viewModel.uiState.value.lastKnownLocation)
        assertEquals(12.9716, viewModel.uiState.value.lastKnownLocation!!.latitude, 0.0001)
        assertEquals(77.5946, viewModel.uiState.value.lastKnownLocation!!.longitude, 0.0001)
    }

    @Test
    fun isProtected_isTrueOnlyWhenContactsExistAndLocationIsReady() = runTest {
        fakeLocationClient.permissionGranted = true
        fakeLocationClient.gpsEnabled = true

        viewModel = createViewModel()
        advanceUntilIdle()

        // 0 contacts -> not protected
        assertFalse(viewModel.uiState.value.isProtected)

        // Add contact -> protected
        fakeContactRepository.setContacts(
            listOf(EmergencyContact(id = "c1", name = "Guardian", phoneNumber = "+919876543210", relationship = "Family"))
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isProtected)

        // Turn off GPS -> not protected
        fakeLocationClient.gpsEnabled = false
        viewModel.refreshLocationState()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isProtected)
    }
}

// ── Test Fakes ─────────────────────────────────────────────────────────────

private class FakeHomeUserRepository : UserRepository {
    private val userFlow = MutableStateFlow<User?>(null)

    fun setUser(user: User?) {
        userFlow.value = user
    }

    override fun getUser(userId: String): Flow<User?> = userFlow.asStateFlow()
    override fun getPrimaryUser(): Flow<User?> = userFlow.asStateFlow()
    override suspend fun getPrimaryUserDirect(): User? = userFlow.value
    override suspend fun saveUser(user: User) { userFlow.value = user }
    override suspend fun syncUser(userId: String?): Result<User> =
        userFlow.value?.let { Result.success(it) } ?: Result.failure(Exception("No user"))
    override suspend fun updateProfile(user: User): Result<User> {
        userFlow.value = user
        return Result.success(user)
    }
    override suspend fun clearUsers() { userFlow.value = null }
}

private class FakeHomeEmergencyContactRepository : EmergencyContactRepository {
    private val contactsFlow = MutableStateFlow<List<EmergencyContact>>(emptyList())

    fun setContacts(contacts: List<EmergencyContact>) {
        contactsFlow.value = contacts
    }

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> = contactsFlow.asStateFlow()
    override suspend fun addContact(contact: EmergencyContact) {
        contactsFlow.value = contactsFlow.value + contact
    }
    override suspend fun updateContact(contact: EmergencyContact) {
        contactsFlow.value = contactsFlow.value.map { if (it.id == contact.id) contact else it }
    }
    override suspend fun deleteContact(contact: EmergencyContact) {
        contactsFlow.value = contactsFlow.value.filterNot { it.id == contact.id }
    }
    override suspend fun syncContacts(): Result<List<EmergencyContact>> = Result.success(contactsFlow.value)
}

private class FakeHomeJourneyRepository : JourneyRepository {
    private val activeJourneyFlow = MutableStateFlow<JourneyEntity?>(null)

    fun setActiveJourney(journey: JourneyEntity?) {
        activeJourneyFlow.value = journey
    }

    override fun getAllJourneys(): Flow<List<JourneyEntity>> = MutableStateFlow(emptyList())
    override fun getActiveJourney(): Flow<JourneyEntity?> = activeJourneyFlow.asStateFlow()
    override suspend fun startJourney(journey: JourneyEntity) { activeJourneyFlow.value = journey }
    override suspend fun updateJourneyStatus(journeyId: String, status: String) {
        if (status == "COMPLETED") activeJourneyFlow.value = null
    }
    override suspend fun endJourney(journeyId: String, status: String) { activeJourneyFlow.value = null }
    override suspend fun updateCurrentLocation(journeyId: String, latitude: Double, longitude: Double) {}
    override suspend fun deleteJourney(journeyId: String) {}
    override suspend fun syncJourneys(): Result<List<JourneyEntity>> = Result.success(emptyList())
}

private class FakeHomeLocationClient : LocationClient {
    var permissionGranted: Boolean = true
    var gpsEnabled: Boolean = true
    var mockLocation: UserLocation? = null

    override fun hasPermission(): Boolean = permissionGranted
    override fun isGpsEnabled(): Boolean = gpsEnabled
    override suspend fun getCurrentLocation(highAccuracy: Boolean): LocationState {
        return if (!permissionGranted) {
            LocationState.Error.PermissionDenied
        } else if (!gpsEnabled) {
            LocationState.Error.GpsDisabled
        } else {
            mockLocation?.let { LocationState.Success(it) } ?: LocationState.Error.Unavailable("No fix")
        }
    }
    override suspend fun getLastKnownLocation(): UserLocation? = mockLocation
    override fun getLocationUpdates(intervalMs: Long, minDistanceMeters: Float, highAccuracy: Boolean): Flow<LocationState> =
        MutableStateFlow(LocationState.Idle)
}
