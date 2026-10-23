package com.samraksha.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.AuthRepository
import com.samraksha.app.domain.repository.SyncRepository
import com.samraksha.app.domain.repository.SyncState
import com.samraksha.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editPhone: String = "",
    val editBloodGroup: String = "",
    val editAllergies: String = "",
    val editOrganDonor: Boolean = false,
    val isSaving: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showLogoutConfirm: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,

    // Journey settings toggles (locally persisted in future)
    val journeyAutoStart: Boolean = false,
    val journeySpeedAlerts: Boolean = true,
    val journeyShareOnSos: Boolean = true,

    // Safety preferences
    val shakeToSos: Boolean = false,
    val backgroundTracking: Boolean = true,
    val soundAlerts: Boolean = true,
    val locationShareWithContacts: Boolean = true
)

/**
 * ViewModel for the SAMRAKSHA Profile screen.
 *
 * Manages:
 * - Profile display and editing (name, phone, medical info)
 * - Sync status observation
 * - Journey settings toggles
 * - Safety preference toggles
 * - Logout via AuthRepository (clears session + local user data)
 */
class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncRepository.syncState

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Seed from in-memory session first for instant display, then observe Room for live updates
        val sessionUser = authRepository.getCurrentUser()
        if (sessionUser != null) {
            _uiState.update { it.copy(user = sessionUser, isLoading = false) }
        } else if (!authRepository.isLoggedIn()) {
            _uiState.update { it.copy(user = null, isLoading = false) }
        }

        viewModelScope.launch {
            userRepository.getPrimaryUser().collect { user ->
                _uiState.update { current ->
                    current.copy(
                        user = user ?: if (authRepository.isLoggedIn()) current.user else null,
                        isLoading = false,
                        editName = current.editName.ifBlank { user?.fullName ?: "" },
                        editPhone = current.editPhone.ifBlank { user?.phone ?: "" },
                        editBloodGroup = current.editBloodGroup.ifBlank { user?.bloodGroup ?: "" },
                        editAllergies = current.editAllergies.ifBlank { user?.allergies ?: "" },
                        editOrganDonor = user?.isOrganDonor ?: current.editOrganDonor
                    )
                }
            }
        }

        refreshProfile()
    }

    // ── Edit Dialog ──────────────────────────────────────────────────────────

    fun showEditDialog() {
        val user = _uiState.value.user
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = user?.fullName ?: "",
                editPhone = user?.phone ?: "",
                editBloodGroup = user?.bloodGroup ?: "",
                editAllergies = user?.allergies ?: "",
                editOrganDonor = user?.isOrganDonor ?: false
            )
        }
    }

    fun dismissEditDialog() {
        _uiState.update { it.copy(isEditing = false, errorMessage = null) }
    }

    fun updateEditName(name: String) = _uiState.update { it.copy(editName = name) }
    fun updateEditPhone(phone: String) = _uiState.update { it.copy(editPhone = phone) }
    fun updateEditBloodGroup(group: String) = _uiState.update { it.copy(editBloodGroup = group) }
    fun updateEditAllergies(allergies: String) = _uiState.update { it.copy(editAllergies = allergies) }
    fun updateEditOrganDonor(donor: Boolean) = _uiState.update { it.copy(editOrganDonor = donor) }

    fun saveProfile() {
        val state = _uiState.value
        val name = state.editName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name cannot be empty") }
            return
        }

        val current = state.user ?: return
        val updated = current.copy(
            fullName = name,
            phone = state.editPhone.trim(),
            bloodGroup = state.editBloodGroup.trim().ifBlank { null },
            allergies = state.editAllergies.trim().ifBlank { null },
            isOrganDonor = state.editOrganDonor
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = userRepository.updateProfile(updated)
            result.fold(
                onSuccess = { savedUser ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            user = savedUser,
                            message = "Profile updated",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.localizedMessage ?: "Failed to save profile"
                        )
                    }
                }
            )
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null, errorMessage = null) }

    // ── Sync & Profile Refresh ───────────────────────────────────────────────

    fun refreshProfile() {
        viewModelScope.launch {
            try {
                userRepository.syncUser()
            } catch (_: Exception) {
                // Background sync errors fall back to local Room cache gracefully
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            refreshProfile()
            syncRepository.syncAll()
        }
    }

    // ── Journey Settings ──────────────────────────────────────────────────────

    fun toggleJourneyAutoStart(enabled: Boolean) =
        _uiState.update { it.copy(journeyAutoStart = enabled) }

    fun toggleJourneySpeedAlerts(enabled: Boolean) =
        _uiState.update { it.copy(journeySpeedAlerts = enabled) }

    fun toggleJourneyShareOnSos(enabled: Boolean) =
        _uiState.update { it.copy(journeyShareOnSos = enabled) }

    // ── Safety Preferences ────────────────────────────────────────────────────

    fun toggleShakeToSos(enabled: Boolean) =
        _uiState.update { it.copy(shakeToSos = enabled) }

    fun toggleBackgroundTracking(enabled: Boolean) =
        _uiState.update { it.copy(backgroundTracking = enabled) }

    fun toggleSoundAlerts(enabled: Boolean) =
        _uiState.update { it.copy(soundAlerts = enabled) }

    fun toggleLocationShare(enabled: Boolean) =
        _uiState.update { it.copy(locationShareWithContacts = enabled) }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun requestLogout() = _uiState.update { it.copy(showLogoutConfirm = true) }

    fun dismissLogoutConfirm() = _uiState.update { it.copy(showLogoutConfirm = false) }

    /**
     * Clears session via AuthRepository (purges JWT + SessionManager),
     * clears local user from Room, then invokes [onLoggedOut] to navigate
     * to the Login screen.
     */
    fun confirmLogout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirm = false) }
            try {
                authRepository.logout()          // purges token from SessionManager
                userRepository.clearUsers()       // wipes Room user cache
            } catch (_: Exception) {
                // Always proceed to logout screen even if network/DB call fails
            } finally {
                _uiState.update { it.copy(isLoggingOut = false, user = null) }
                onLoggedOut()
            }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val userRepository: UserRepository = SamrakshaApp.instance.userRepository,
        private val authRepository: AuthRepository = SamrakshaApp.instance.authRepository,
        private val syncRepository: SyncRepository = SamrakshaApp.instance.syncRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(userRepository, authRepository, syncRepository) as T
        }
    }
}
