package com.samraksha.app.ui.screens.profile

import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.AuthSession
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.AuthRepository
import com.samraksha.app.domain.repository.SyncRepository
import com.samraksha.app.domain.repository.SyncState
import com.samraksha.app.domain.repository.SyncSummary
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.ui.screens.location.model.SafetyZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ProfileViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeUserRepository: FakeUserRepository
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var fakeSyncRepository: FakeSyncRepository
    private lateinit var viewModel: ProfileViewModel

    private val sampleUser = User(
        id = "user-123",
        fullName = "Sneha Patel",
        email = "sneha@example.com",
        phone = "+919876543210",
        bloodGroup = "O+",
        allergies = "Penicillin",
        isOrganDonor = true
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeUserRepository = FakeUserRepository()
        fakeAuthRepository = FakeAuthRepository()
        fakeSyncRepository = FakeSyncRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            userRepository = fakeUserRepository,
            authRepository = fakeAuthRepository,
            syncRepository = fakeSyncRepository
        )
    }

    @Test
    fun initialState_loadsUserFromAuthSessionImmediately() = runTest(testDispatcher) {
        fakeAuthRepository.activeUser = sampleUser
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.user)
        assertEquals("Sneha Patel", state.user?.fullName)
        assertEquals("sneha@example.com", state.user?.email)
        assertEquals("+919876543210", state.user?.phone)
        assertEquals("O+", state.user?.bloodGroup)
        assertEquals("Penicillin", state.user?.allergies)
        assertTrue(state.user?.isOrganDonor == true)
        assertFalse(state.isLoading)
    }

    @Test
    fun initialState_whenNotLoggedIn_hasNullUser() = runTest(testDispatcher) {
        fakeAuthRepository.activeUser = null
        fakeAuthRepository.loggedIn = false
        fakeUserRepository.emitUser(null)

        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.user)
        assertFalse(state.isLoading)
    }

    @Test
    fun observesRoomUserUpdates_updatesUiStateAndEditDefaults() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeUserRepository.emitUser(sampleUser)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Sneha Patel", state.user?.fullName)
        assertEquals("Sneha Patel", state.editName)
        assertEquals("+919876543210", state.editPhone)
        assertEquals("O+", state.editBloodGroup)
    }

    @Test
    fun saveProfile_withValidInput_persistsToRepository() = runTest(testDispatcher) {
        fakeAuthRepository.activeUser = sampleUser
        viewModel = createViewModel()
        testScheduler.runCurrent()

        fakeUserRepository.emitUser(sampleUser)
        testScheduler.runCurrent()

        viewModel.showEditDialog()
        viewModel.updateEditName("Sneha Sharma")
        viewModel.updateEditBloodGroup("AB+")
        viewModel.saveProfile()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isEditing)
        assertEquals("Sneha Sharma", state.user?.fullName)
        assertEquals("AB+", state.user?.bloodGroup)
        assertEquals("Profile updated", state.message)
    }

    @Test
    fun saveProfile_withBlankName_setsErrorMessage() = runTest(testDispatcher) {
        fakeAuthRepository.activeUser = sampleUser
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showEditDialog()
        viewModel.updateEditName("   ")
        viewModel.saveProfile()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals("Name cannot be empty", state.errorMessage)
    }

    @Test
    fun confirmLogout_clearsSessionAndRoomUser() = runTest(testDispatcher) {
        fakeAuthRepository.activeUser = sampleUser
        fakeUserRepository.emitUser(sampleUser)
        viewModel = createViewModel()
        testScheduler.runCurrent()

        var loggedOutNavigated = false
        viewModel.confirmLogout {
            loggedOutNavigated = true
        }
        testScheduler.runCurrent()

        assertTrue(loggedOutNavigated)
        assertNull(fakeAuthRepository.activeUser)
        assertFalse(fakeAuthRepository.loggedIn)
        assertNull(viewModel.uiState.value.user)
    }

    @Test
    fun settingsToggles_updatePreferencesCorrectly() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.toggleJourneyAutoStart(true)
        assertTrue(viewModel.uiState.value.journeyAutoStart)

        assertFalse(viewModel.uiState.value.shakeToSos)
        viewModel.toggleShakeToSos(true)
        assertTrue(viewModel.uiState.value.shakeToSos)
        viewModel.toggleShakeToSos(false)
        assertFalse(viewModel.uiState.value.shakeToSos)

        viewModel.toggleBackgroundTracking(false)
        assertFalse(viewModel.uiState.value.backgroundTracking)
    }
}

// ── Test Fakes ────────────────────────────────────────────────────────────────

private class FakeUserRepository : UserRepository {
    private val userFlow = MutableStateFlow<User?>(null)
    var syncCalled = false

    fun emitUser(user: User?) {
        userFlow.value = user
    }

    override fun getUser(userId: String): Flow<User?> = userFlow.asStateFlow()
    override fun getPrimaryUser(): Flow<User?> = userFlow.asStateFlow()
    override suspend fun getPrimaryUserDirect(): User? = userFlow.value

    override suspend fun saveUser(user: User) {
        userFlow.value = user
    }

    override suspend fun syncUser(userId: String?): Result<User> {
        syncCalled = true
        val current = userFlow.value ?: User("test", "Test User", "test@test.com", "1234567890")
        return Result.success(current)
    }

    override suspend fun updateProfile(user: User): Result<User> {
        userFlow.value = user
        return Result.success(user)
    }

    override suspend fun clearUsers() {
        userFlow.value = null
    }
}

private class FakeAuthRepository : AuthRepository {
    var activeUser: User? = null
    var loggedIn: Boolean = true
    var activeToken: String? = "fake-jwt-token"

    override fun isLoggedIn(): Boolean = loggedIn
    override fun getCurrentUser(): User? = activeUser
    override fun getSavedToken(): String? = activeToken

    override suspend fun login(identifier: String, password: String): Result<AuthSession> {
        val user = User("u1", "User", identifier, "1234567890")
        activeUser = user
        loggedIn = true
        return Result.success(AuthSession(user, "token"))
    }

    override suspend fun signup(fullName: String, email: String, phone: String, password: String): Result<AuthSession> {
        val user = User("u1", fullName, email, phone)
        activeUser = user
        loggedIn = true
        return Result.success(AuthSession(user, "token"))
    }

    override suspend fun logout(): Result<Unit> {
        activeUser = null
        loggedIn = false
        activeToken = null
        return Result.success(Unit)
    }
}

private class FakeSyncRepository : SyncRepository {
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState = _syncState.asStateFlow()

    override suspend fun syncAll(): Result<SyncSummary> = Result.success(SyncSummary())
    override suspend fun syncUserData(): Result<User> = Result.success(User("u1", "User", "e", "p"))
    override suspend fun syncContacts(): Result<List<EmergencyContact>> = Result.success(emptyList())
    override suspend fun syncEmergencyEvents(): Result<List<EmergencyEvent>> = Result.success(emptyList())
    override suspend fun syncJourneys(): Result<List<JourneyEntity>> = Result.success(emptyList())
    override suspend fun syncSafetyZones(): Result<List<SafetyZone>> = Result.success(emptyList())
}
