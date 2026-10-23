package com.samraksha.app.data.repository

import org.maplibre.android.geometry.LatLng
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.EmergencyEventRepository
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.domain.repository.SafetyZoneLoadResult
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.screens.location.model.SafetyZoneShape
import com.samraksha.app.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var repoJob: Job
    private lateinit var repoScope: CoroutineScope

    private lateinit var fakeUserRepo: FakeUserRepository
    private lateinit var fakeContactsRepo: FakeEmergencyContactRepository
    private lateinit var fakeEventsRepo: FakeEmergencyEventRepository
    private lateinit var fakeJourneyRepo: FakeJourneyRepository
    private lateinit var fakeSafetyZoneRepo: FakeSafetyZoneRepository
    private lateinit var fakeNetworkMonitor: FakeNetworkMonitor

    private lateinit var syncRepository: SyncRepositoryImpl

    @Before
    fun setUp() {
        repoJob = Job()
        repoScope = CoroutineScope(repoJob + testDispatcher)
        fakeUserRepo = FakeUserRepository()
        fakeContactsRepo = FakeEmergencyContactRepository()
        fakeEventsRepo = FakeEmergencyEventRepository()
        fakeJourneyRepo = FakeJourneyRepository()
        fakeSafetyZoneRepo = FakeSafetyZoneRepository()
        fakeNetworkMonitor = FakeNetworkMonitor(initialOnline = true)

        syncRepository = SyncRepositoryImpl(
            userRepository = fakeUserRepo,
            emergencyContactRepository = fakeContactsRepo,
            emergencyEventRepository = fakeEventsRepo,
            journeyRepository = fakeJourneyRepo,
            safetyZoneRepository = fakeSafetyZoneRepo,
            networkMonitor = fakeNetworkMonitor,
            scope = repoScope,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        repoJob.cancel()
    }

    @Test
    fun syncAll_whenOnline_syncsAllFiveModulesSuccessfully() = testScope.runTest {
        val result = syncRepository.syncAll()

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        assertTrue(summary!!.userSuccess)
        assertEquals(2, summary.contactsSynced)
        assertEquals(1, summary.eventsSynced)
        assertEquals(1, summary.journeysSynced)
        assertEquals(1, summary.safetyZonesSynced)
        assertTrue(summary.isFullySynced)

        val state = syncRepository.syncState.value
        assertFalse(state.isSyncing)
        assertTrue(state.isOnline)
        assertNotNull(state.lastSyncTimestamp)
        assertTrue(state.statusMessage.contains("Synced: 2 contacts"))
    }

    @Test
    fun syncAll_whenOffline_preservesLocalDataGracefully() = testScope.runTest {
        fakeNetworkMonitor.setConnected(false)

        val result = syncRepository.syncAll()

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull(summary)
        // Local records are preserved
        assertEquals(2, summary!!.contactsSynced)
        assertEquals(1, summary.eventsSynced)
        assertEquals(1, summary.journeysSynced)
        assertEquals(1, summary.safetyZonesSynced)
        assertFalse(summary.isFullySynced)

        val state = syncRepository.syncState.value
        assertFalse(state.isSyncing)
        assertFalse(state.isOnline)
        assertTrue(state.statusMessage.contains("Offline") || state.statusMessage.contains("retained"))
    }

    @Test
    fun syncAll_whenRepositoryThrows_handlesGracefullyWithoutCrashing() = testScope.runTest {
        fakeContactsRepo.shouldThrowOnSync = true

        val result = syncRepository.syncAll()

        assertTrue(result.isSuccess)
        val state = syncRepository.syncState.value
        assertFalse(state.isSyncing)
        assertNotNull(state.error)
        assertTrue(state.statusMessage.contains("Offline storage preserved"))
    }

    @Test
    fun individualModuleSync_syncUserData_delegatesToUserRepository() = testScope.runTest {
        val userResult = syncRepository.syncUserData()
        assertTrue(userResult.isSuccess)
        assertEquals("user_001", userResult.getOrNull()?.id)
        assertEquals("Karthik R.", userResult.getOrNull()?.fullName)
    }

    @Test
    fun individualModuleSync_syncContacts_delegatesToContactRepository() = testScope.runTest {
        val contactsResult = syncRepository.syncContacts()
        assertTrue(contactsResult.isSuccess)
        assertEquals(2, contactsResult.getOrNull()?.size)
    }

    @Test
    fun individualModuleSync_syncEmergencyEvents_delegatesToEventRepository() = testScope.runTest {
        val eventsResult = syncRepository.syncEmergencyEvents()
        assertTrue(eventsResult.isSuccess)
        assertEquals(1, eventsResult.getOrNull()?.size)
    }

    @Test
    fun individualModuleSync_syncJourneys_delegatesToJourneyRepository() = testScope.runTest {
        val journeyResult = syncRepository.syncJourneys()
        assertTrue(journeyResult.isSuccess)
        assertEquals(1, journeyResult.getOrNull()?.size)
    }

    @Test
    fun individualModuleSync_syncSafetyZones_delegatesToSafetyZoneRepository() = testScope.runTest {
        val zoneResult = syncRepository.syncSafetyZones()
        assertTrue(zoneResult.isSuccess)
        assertEquals(1, zoneResult.getOrNull()?.size)
    }

    @Test
    fun networkReconnect_triggersAutomaticSynchronization() = testScope.runTest {
        // Start offline
        fakeNetworkMonitor.emitOnline(false)
        advanceUntilIdle()
        assertFalse(syncRepository.syncState.value.isOnline)

        // Reconnect online
        fakeNetworkMonitor.emitOnline(true)
        advanceUntilIdle()

        assertTrue(syncRepository.syncState.value.isOnline)
        assertNotNull(syncRepository.syncState.value.lastSyncTimestamp)
    }

    // =========================================================================
    // Test Doubles (Fakes)
    // =========================================================================

    private class FakeNetworkMonitor(initialOnline: Boolean) : NetworkMonitor() {
        private var connected = initialOnline
        private val _isOnline = MutableSharedFlow<Boolean>(replay = 1).apply {
            tryEmit(initialOnline)
        }
        override val isOnline: Flow<Boolean> = _isOnline.asSharedFlow()

        override fun isConnected(): Boolean = connected

        fun setConnected(value: Boolean) {
            connected = value
            _isOnline.tryEmit(value)
        }

        suspend fun emitOnline(value: Boolean) {
            connected = value
            _isOnline.emit(value)
        }
    }

    private class FakeUserRepository : UserRepository {
        private val demoUser = User(
            id = "user_001",
            fullName = "Karthik R.",
            email = "karthik@samraksha.app",
            phone = "+919876543210",
            bloodGroup = "O+",
            allergies = "None",
            isOrganDonor = true
        )

        override fun getUser(userId: String): Flow<User?> = flowOf(demoUser)
        override fun getPrimaryUser(): Flow<User?> = flowOf(demoUser)
        override suspend fun getPrimaryUserDirect(): User? = demoUser
        override suspend fun saveUser(user: User) {}
        override suspend fun updateProfile(user: User): Result<User> = Result.success(user)
        override suspend fun syncUser(userId: String?): Result<User> = Result.success(demoUser)
        override suspend fun clearUsers() {}
    }

    private class FakeEmergencyContactRepository : EmergencyContactRepository {
        var shouldThrowOnSync = false
        private val contacts = mutableListOf(
            EmergencyContact("c1", "Priya R", "+919876543211", "Sister"),
            EmergencyContact("c2", "Dr. Ramesh", "+919876543212", "Doctor")
        )

        override fun getEmergencyContacts(): Flow<List<EmergencyContact>> = flowOf(contacts)
        override suspend fun addContact(contact: EmergencyContact) { contacts.add(contact) }
        override suspend fun updateContact(contact: EmergencyContact) {}
        override suspend fun deleteContact(contact: EmergencyContact) { contacts.remove(contact) }
        override suspend fun syncContacts(): Result<List<EmergencyContact>> {
            if (shouldThrowOnSync) throw RuntimeException("Simulated sync error")
            return Result.success(contacts)
        }
    }

    private class FakeEmergencyEventRepository : EmergencyEventRepository {
        private val events = mutableListOf(
            EmergencyEvent(
                id = "ev_001",
                timestamp = System.currentTimeMillis(),
                latitude = 12.9716,
                longitude = 77.5946,
                status = EmergencyEventStatus.ACTIVE,
                eventType = EmergencyEventType.MANUAL_SOS,
                address = "Bangalore MG Road",
                isSynced = false
            )
        )

        override fun getAllEvents(): Flow<List<EmergencyEvent>> = flowOf(events)
        override fun getEventById(eventId: String): Flow<EmergencyEvent?> = flowOf(events.find { it.id == eventId })
        override fun getUnsyncedEvents(): Flow<List<EmergencyEvent>> = flowOf(events.filter { !it.isSynced })
        override suspend fun createEvent(event: EmergencyEvent) { events.add(event) }
        override suspend fun updateStatus(eventId: String, status: EmergencyEventStatus, resolvedAt: Long?) {}
        override suspend fun markSynced(eventId: String) {}
        override suspend fun deleteEvent(eventId: String) { events.removeAll { it.id == eventId } }
        override suspend fun syncEvents(): Result<List<EmergencyEvent>> = Result.success(events)
    }

    private class FakeJourneyRepository : JourneyRepository {
        private val journeys = mutableListOf(
            JourneyEntity(
                id = "j_001",
                startLocationName = "Start Location",
                endLocationName = "Destination",
                startTime = System.currentTimeMillis(),
                status = "COMPLETED",
                startLatitude = 12.9716,
                startLongitude = 77.5946,
                currentLatitude = 12.9352,
                currentLongitude = 77.6245,
                destinationLatitude = 12.9352,
                destinationLongitude = 77.6245,
                isSynced = true
            )
        )

        override fun getAllJourneys(): Flow<List<JourneyEntity>> = flowOf(journeys)
        override fun getActiveJourney(): Flow<JourneyEntity?> = flowOf(null)
        override suspend fun startJourney(journey: JourneyEntity) { journeys.add(journey) }
        override suspend fun updateJourneyStatus(journeyId: String, status: String) {}
        override suspend fun endJourney(journeyId: String, status: String) {}
        override suspend fun updateCurrentLocation(journeyId: String, latitude: Double, longitude: Double) {}
        override suspend fun deleteJourney(journeyId: String) { journeys.removeAll { it.id == journeyId } }
        override suspend fun syncJourneys(): Result<List<JourneyEntity>> = Result.success(journeys)
    }

    private class FakeSafetyZoneRepository : SafetyZoneRepository {
        private val zones = listOf(
            SafetyZone(
                id = "zone_1",
                name = "Brigade Safe Zone",
                level = SafetyZoneLevel.SAFE,
                shape = SafetyZoneShape.Circular(LatLng(12.9716, 77.5946), 500.0),
                centerPoint = LatLng(12.9716, 77.5946),
                safetyScore = 95,
                advisory = "High surveillance zone"
            )
        )

        override suspend fun getSafetyZones(
            lat: Double?,
            lng: Double?,
            radius: Double?,
            riskLevel: String?,
            forceRefresh: Boolean
        ): SafetyZoneLoadResult = SafetyZoneLoadResult.Success(zones = zones, isFromCache = false)

        override fun getCachedSafetyZones(): List<SafetyZone> = zones
        override suspend fun syncSafetyZones(): Result<List<SafetyZone>> = Result.success(zones)
    }
}
