package com.samraksha.app.ui.screens.emergency

import android.content.Intent
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.EmergencyEventRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.data.local.SettingsPreferences
import com.samraksha.app.emergency.AlertDeliveryState
import com.samraksha.app.emergency.ContactAlertDelivery
import com.samraksha.app.emergency.EmergencyAlertDispatcher
import com.samraksha.app.emergency.ShakeDetector
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
class SosViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeEventRepo: FakeSosEventRepository
    private lateinit var fakeContactRepo: FakeSosContactRepository
    private lateinit var fakeLocationClient: FakeSosLocationClient
    private lateinit var fakeAlertDispatcher: FakeSosAlertDispatcher
    private lateinit var fakeUserRepo: FakeSosUserRepo
    private lateinit var viewModel: SosViewModel

    private val sampleContact1 = EmergencyContact(
        id = "c1",
        name = "Mother",
        phoneNumber = "+919876543210",
        relationship = "Parent"
    )

    private val sampleContact2 = EmergencyContact(
        id = "c2",
        name = "Brother",
        phoneNumber = "+919876543211",
        relationship = "Sibling"
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        fakeEventRepo = FakeSosEventRepository()
        fakeContactRepo = FakeSosContactRepository()
        fakeLocationClient = FakeSosLocationClient()
        fakeAlertDispatcher = FakeSosAlertDispatcher()
        fakeUserRepo = FakeSosUserRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        settingsPreferences: SettingsPreferences? = null,
        shakeDetectorProvider: (((onThreeShakes: () -> Unit) -> ShakeDetector))? = null
    ): SosViewModel {
        return SosViewModel(
            eventRepository = fakeEventRepo,
            contactRepository = fakeContactRepo,
            locationClient = fakeLocationClient,
            alertDispatcher = fakeAlertDispatcher,
            userRepository = fakeUserRepo,
            settingsPreferences = settingsPreferences,
            shakeDetectorProvider = shakeDetectorProvider
        )
    }

    @Test
    fun initialState_isIdleWithNoAutomaticActivation() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SosState.IDLE, state.state)
        assertEquals(0f, state.holdProgress)
        assertNull(state.activeEventId)
        assertNull(state.userLocation)
        assertTrue(state.contactDeliveries.isEmpty())
        assertFalse(state.isFetchingLocation)
    }

    @Test
    fun holdStart_andReleaseBeforeComplete_resetsToIdleWithoutTriggeringSos() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.onHoldStart()
        testScheduler.runCurrent()

        assertEquals(SosState.ARMING, viewModel.uiState.value.state)

        // Advance 1 second (1000ms < 3000ms)
        advanceTimeBy(1000L)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.holdProgress > 0f)
        assertTrue(viewModel.uiState.value.holdProgress < 1f)

        // Release early
        viewModel.onHoldRelease()
        testScheduler.runCurrent()

        assertEquals(SosState.IDLE, viewModel.uiState.value.state)
        assertEquals(0f, viewModel.uiState.value.holdProgress)
        assertEquals(0, fakeEventRepo.createdEvents.size)
    }

    @Test
    fun fullHold_armsCountdown_andCancelAbortsActivation() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.onHoldStart()
        advanceTimeBy(3100L)
        testScheduler.runCurrent()

        assertEquals(SosState.ARMED, viewModel.uiState.value.state)

        // Cancel during countdown
        viewModel.cancel()
        testScheduler.runCurrent()

        assertEquals(SosState.CANCELLED, viewModel.uiState.value.state)
        assertEquals(0, fakeEventRepo.createdEvents.size)
    }

    @Test
    fun activateSos_withValidLocationAndContacts_dispatchesAlertsAndRecordsEvent() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(listOf(sampleContact1, sampleContact2))
        fakeLocationClient.mockLocation = UserLocation(12.9716, 77.5946, 10f)
        fakeUserRepo.mockUser = User("u1", "Priya Sharma", "priya@example.com", "+919876543210")

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.activateSos()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SosState.ACTIVATED, state.state)
        assertNotNull(state.activeEventId)
        assertNotNull(state.userLocation)
        assertEquals(12.9716, state.userLocation?.latitude ?: 0.0, 0.0001)
        assertEquals(77.5946, state.userLocation?.longitude ?: 0.0, 0.0001)
        assertNull(state.locationError)

        // 2 alerts dispatched via FakeSosAlertDispatcher
        assertEquals(2, state.contactDeliveries.size)
        assertTrue(state.contactDeliveries.all { it.status == AlertDeliveryState.SENT })

        // 1 event written to repository
        assertEquals(1, fakeEventRepo.createdEvents.size)
        val created = fakeEventRepo.createdEvents.first()
        assertEquals(EmergencyEventStatus.ACTIVE, created.status)
        assertEquals(EmergencyEventType.MANUAL_SOS, created.eventType)
        assertEquals(12.9716, created.latitude, 0.0001)
    }

    @Test
    fun activateSos_withMissingLocation_handlesFallbackGracefullyWithoutCrashing() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(listOf(sampleContact1))
        fakeLocationClient.mockLocation = null // Location unavailable

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.activateSos()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SosState.ACTIVATED, state.state)
        assertNull(state.userLocation)
        assertNotNull(state.locationError)
        assertEquals(1, state.contactDeliveries.size)

        // Emergency event created with 0.0, 0.0 coordinates
        assertEquals(1, fakeEventRepo.createdEvents.size)
        assertEquals(0.0, fakeEventRepo.createdEvents.first().latitude, 0.001)
    }

    @Test
    fun activateSos_withNoConfiguredContacts_warnsUserWithoutCrashing() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(emptyList()) // No guardians configured
        fakeLocationClient.mockLocation = UserLocation(12.9716, 77.5946, 10f)

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.activateSos()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(SosState.ACTIVATED, state.state)
        assertTrue(state.contactDeliveries.isEmpty())
        assertTrue(state.deliverySummary?.contains("No emergency contacts") == true)
        assertEquals(1, fakeEventRepo.createdEvents.size)
    }

    @Test
    fun activateSos_duplicateCallsArePrevented() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(listOf(sampleContact1))
        fakeLocationClient.mockLocation = UserLocation(12.9716, 77.5946, 10f)

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.activateSos()
        testScheduler.runCurrent()

        assertEquals(1, fakeEventRepo.createdEvents.size)

        // Second tap while already activated should be ignored
        viewModel.activateSos()
        testScheduler.runCurrent()

        assertEquals(1, fakeEventRepo.createdEvents.size)
    }

    @Test
    fun cancel_activeSos_updatesEventStatusToCancelled() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(listOf(sampleContact1))
        fakeLocationClient.mockLocation = UserLocation(12.9716, 77.5946, 10f)

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.activateSos()
        testScheduler.runCurrent()

        val eventId = viewModel.uiState.value.activeEventId
        assertNotNull(eventId)

        viewModel.cancel()
        testScheduler.runCurrent()

        assertEquals(SosState.CANCELLED, viewModel.uiState.value.state)
        val updated = fakeEventRepo.createdEvents.firstOrNull { it.id == eventId }
        assertEquals(EmergencyEventStatus.CANCELLED, updated?.status)
    }

    @Test
    fun retryLocationAndResendAlert_acquiresLocationAndUpdateState() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(listOf(sampleContact1))
        fakeLocationClient.mockLocation = null // Initially null

        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.activateSos()
        testScheduler.runCurrent()

        assertNull(viewModel.uiState.value.userLocation)

        // GPS fix acquired
        fakeLocationClient.mockLocation = UserLocation(12.9800, 77.6000, 5f)
        viewModel.retryLocationAndResendAlert()
        testScheduler.runCurrent()

        val updatedState = viewModel.uiState.value
        assertNotNull(updatedState.userLocation)
        assertEquals(12.9800, updatedState.userLocation?.latitude ?: 0.0, 0.0001)
        assertNull(updatedState.locationError)
    }

    // ── Shake SOS Foreground Tests ────────────────────────────────────────────

    @Test
    fun shakeToSos_disabledByDefault() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isShakeToSosEnabled)
        assertFalse(viewModel.uiState.value.isShakeListening)
    }

    @Test
    fun threeShakesDetected_whenDisabled_doesNotTriggerArmingOrSos() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        viewModel.onThreeShakesDetected()
        testScheduler.runCurrent()

        assertEquals(SosState.IDLE, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.isShakeTriggered)
        assertTrue(fakeEventRepo.createdEvents.isEmpty())
    }

    @Test
    fun threeShakesDetected_whenEnabled_triggersArmedCountdownWithShakeFlag() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        prefs.setShakeToSos(true)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isShakeToSosEnabled)

        viewModel.onThreeShakesDetected()
        testScheduler.runCurrent()

        assertEquals(SosState.ARMED, viewModel.uiState.value.state)
        assertTrue(viewModel.uiState.value.isShakeTriggered)
        assertEquals(3, viewModel.uiState.value.countdownSeconds)
    }

    @Test
    fun shakeCountdown_cancel_abortsWithoutDispatchingSos() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        prefs.setShakeToSos(true)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        viewModel.onThreeShakesDetected()
        testScheduler.runCurrent()

        // Advance 1 second into the 3-second countdown
        advanceTimeBy(1000L)
        testScheduler.runCurrent()
        assertEquals(2, viewModel.uiState.value.countdownSeconds)

        // User hits cancel
        viewModel.cancel()
        testScheduler.runCurrent()

        assertEquals(SosState.CANCELLED, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.isShakeTriggered)
        assertTrue(fakeEventRepo.createdEvents.isEmpty())
    }

    @Test
    fun shakeCountdown_onScreenPaused_abortsWithoutDispatchingSos() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        prefs.setShakeToSos(true)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        viewModel.onThreeShakesDetected()
        testScheduler.runCurrent()

        // Advance 1 second into countdown
        advanceTimeBy(1000L)
        testScheduler.runCurrent()

        // App backgrounded or paused
        viewModel.onScreenPaused()
        testScheduler.runCurrent()

        assertEquals(SosState.IDLE, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.isShakeTriggered)
        assertFalse(viewModel.uiState.value.isShakeListening)
        assertTrue(fakeEventRepo.createdEvents.isEmpty())
    }

    @Test
    fun shakeCountdown_complete_dispatchesAlertsAndCreatesEvent() = runTest(testDispatcher) {
        fakeContactRepo.setContacts(listOf(sampleContact1))
        fakeLocationClient.mockLocation = UserLocation(12.9716, 77.5946, 5f)

        val prefs = SettingsPreferences(null)
        prefs.setShakeToSos(true)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        viewModel.onThreeShakesDetected()
        testScheduler.runCurrent()

        // Advance the full 3 seconds
        advanceTimeBy(3000L)
        testScheduler.runCurrent()

        assertEquals(SosState.ACTIVATED, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.isShakeListening)
        assertEquals(1, fakeEventRepo.createdEvents.size)
        val created = fakeEventRepo.createdEvents.first()
        assertEquals(EmergencyEventStatus.ACTIVE, created.status)
        assertTrue(created.notes?.contains("Three-Shake Foreground Trigger") == true)
    }

    @Test
    fun disablingSettingWhileActive_stopsShakeListening() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        prefs.setShakeToSos(true)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isShakeToSosEnabled)

        // Disable setting
        prefs.setShakeToSos(false)
        testScheduler.runCurrent()

        assertFalse(viewModel.uiState.value.isShakeToSosEnabled)
        assertFalse(viewModel.uiState.value.isShakeListening)
    }

    @Test
    fun manualHoldSos_operatesIndependentlyAndPreservesExistingFlow() = runTest(testDispatcher) {
        val prefs = SettingsPreferences(null)
        prefs.setShakeToSos(true)
        viewModel = createViewModel(settingsPreferences = prefs)
        testScheduler.runCurrent()

        // Manual hold start
        viewModel.onHoldStart()
        testScheduler.runCurrent()
        assertEquals(SosState.ARMING, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.isShakeTriggered)

        // Release early resets to IDLE
        viewModel.onHoldRelease()
        testScheduler.runCurrent()
        assertEquals(SosState.IDLE, viewModel.uiState.value.state)

        // Complete 3-second hold
        viewModel.onHoldStart()
        advanceTimeBy(3000L)
        testScheduler.runCurrent()

        assertEquals(SosState.ARMED, viewModel.uiState.value.state)
        assertFalse(viewModel.uiState.value.isShakeTriggered) // Manual hold, not shake triggered
    }
}

// ── Test Fakes ────────────────────────────────────────────────────────────────

private class FakeSosEventRepository : EmergencyEventRepository {
    val createdEvents = mutableListOf<EmergencyEvent>()

    override fun getAllEvents(): Flow<List<EmergencyEvent>> = MutableStateFlow(createdEvents)
    override fun getEventById(eventId: String): Flow<EmergencyEvent?> =
        MutableStateFlow(createdEvents.firstOrNull { it.id == eventId })
    override fun getUnsyncedEvents(): Flow<List<EmergencyEvent>> = MutableStateFlow(emptyList())

    override suspend fun createEvent(event: EmergencyEvent) {
        createdEvents.add(event)
    }

    override suspend fun updateStatus(eventId: String, status: EmergencyEventStatus, resolvedAt: Long?) {
        val index = createdEvents.indexOfFirst { it.id == eventId }
        if (index != -1) {
            createdEvents[index] = createdEvents[index].copy(status = status, resolvedAt = resolvedAt)
        }
    }

    override suspend fun markSynced(eventId: String) {}
    override suspend fun deleteEvent(eventId: String) {
        createdEvents.removeAll { it.id == eventId }
    }
    override suspend fun syncEvents(): Result<List<EmergencyEvent>> = Result.success(createdEvents)
}

private class FakeSosContactRepository : EmergencyContactRepository {
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

private class FakeSosLocationClient : LocationClient {
    var mockLocation: UserLocation? = null

    override fun hasPermission(): Boolean = mockLocation != null
    override fun isGpsEnabled(): Boolean = true
    override suspend fun getCurrentLocation(highAccuracy: Boolean): LocationState {
        return mockLocation?.let { LocationState.Success(it) } ?: LocationState.Error.Unavailable("No GPS lock")
    }
    override suspend fun getLastKnownLocation(): UserLocation? = mockLocation
    override fun getLocationUpdates(intervalMs: Long, minDistanceMeters: Float, highAccuracy: Boolean): Flow<LocationState> =
        MutableStateFlow(LocationState.Idle)
}

private class FakeSosAlertDispatcher : EmergencyAlertDispatcher {
    var hasPermission = true
    var hasService = true

    override fun hasSmsPermission(): Boolean = hasPermission
    override fun canSendSms(): Boolean = hasService

    override fun formatEmergencyMessage(userName: String?, location: UserLocation?, address: String?): String {
        return "SOS: ${userName ?: "User"} at ${location?.latitude ?: 0.0},${location?.longitude ?: 0.0}"
    }

    override suspend fun dispatchAlerts(contacts: List<EmergencyContact>, message: String): List<ContactAlertDelivery> {
        return contacts.map {
            ContactAlertDelivery(
                contactId = it.id,
                contactName = it.name,
                phoneNumber = it.phoneNumber,
                status = if (hasPermission && hasService) AlertDeliveryState.SENT else AlertDeliveryState.FAILED_PERMISSION_DENIED
            )
        }
    }

    override fun createMessagingIntent(phoneNumbers: List<String>, message: String): Intent {
        return Intent()
    }
}

private class FakeSosUserRepo : UserRepository {
    var mockUser: User? = null

    override fun getUser(userId: String): Flow<User?> = MutableStateFlow(mockUser)
    override fun getPrimaryUser(): Flow<User?> = MutableStateFlow(mockUser)
    override suspend fun getPrimaryUserDirect(): User? = mockUser
    override suspend fun saveUser(user: User) { mockUser = user }
    override suspend fun syncUser(userId: String?): Result<User> = Result.success(mockUser ?: User("u", "U", "e", "p"))
    override suspend fun updateProfile(user: User): Result<User> = Result.success(user)
    override suspend fun clearUsers() { mockUser = null }
}
