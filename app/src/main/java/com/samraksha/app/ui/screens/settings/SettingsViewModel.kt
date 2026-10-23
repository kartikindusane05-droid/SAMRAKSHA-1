package com.samraksha.app.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.local.SettingsPreferences
import com.samraksha.app.data.local.ThemeMode
import com.samraksha.app.domain.repository.AuthRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.notifications.NotificationPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Modals and sheets displayed from the Settings screen.
 */
enum class SettingsModal {
    NONE,
    PRIVACY_INFO,
    TERMS_OF_SERVICE,
    AFFILIATE_DISCLOSURE,
    ABOUT_SAMRAKSHA,
    COUNTDOWN_PICKER,
    LOGOUT_CONFIRM
}

/**
 * UI State for the SAMRAKSHA Settings screen.
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val emergencyAlerts: Boolean = true,
    val zoneAlerts: Boolean = true,
    val journeyReminders: Boolean = true,
    val soundAlerts: Boolean = true,
    val vibration: Boolean = true,
    val shakeToSos: Boolean = false,
    val sosCountdownSeconds: Int = 5,
    val autoRecordAudio: Boolean = true,
    val smsFallback: Boolean = true,
    val batterySaverSafeMode: Boolean = false,
    val hasFineLocationPermission: Boolean = false,
    val hasCoarseLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val activeModal: SettingsModal = SettingsModal.NONE,
    val isLoggingOut: Boolean = false
) {
    val hasAnyLocationPermission: Boolean get() = hasFineLocationPermission || hasCoarseLocationPermission
}

/**
 * ViewModel managing settings state, persistent preferences, system permission checks,
 * and user logout flow.
 */
class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            themeMode = settingsPreferences.themeMode.value,
            emergencyAlerts = settingsPreferences.emergencyAlerts.value,
            zoneAlerts = settingsPreferences.zoneAlerts.value,
            journeyReminders = settingsPreferences.journeyReminders.value,
            soundAlerts = settingsPreferences.soundAlerts.value,
            vibration = settingsPreferences.vibration.value,
            shakeToSos = settingsPreferences.shakeToSos.value,
            sosCountdownSeconds = settingsPreferences.sosCountdownSeconds.value,
            autoRecordAudio = settingsPreferences.autoRecordAudio.value,
            smsFallback = settingsPreferences.smsFallback.value,
            batterySaverSafeMode = settingsPreferences.batterySaverSafeMode.value
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect reactive preference streams
        viewModelScope.launch {
            settingsPreferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.emergencyAlerts.collect { alerts ->
                _uiState.update { it.copy(emergencyAlerts = alerts) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.zoneAlerts.collect { alerts ->
                _uiState.update { it.copy(zoneAlerts = alerts) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.journeyReminders.collect { reminders ->
                _uiState.update { it.copy(journeyReminders = reminders) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.soundAlerts.collect { sounds ->
                _uiState.update { it.copy(soundAlerts = sounds) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.vibration.collect { vib ->
                _uiState.update { it.copy(vibration = vib) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.shakeToSos.collect { shake ->
                _uiState.update { it.copy(shakeToSos = shake) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.sosCountdownSeconds.collect { sec ->
                _uiState.update { it.copy(sosCountdownSeconds = sec) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.autoRecordAudio.collect { rec ->
                _uiState.update { it.copy(autoRecordAudio = rec) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.smsFallback.collect { sms ->
                _uiState.update { it.copy(smsFallback = sms) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.batterySaverSafeMode.collect { saver ->
                _uiState.update { it.copy(batterySaverSafeMode = saver) }
            }
        }
    }

    // ── Theme Mode ────────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        settingsPreferences.setThemeMode(mode)
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun toggleEmergencyAlerts(enabled: Boolean) = settingsPreferences.setEmergencyAlerts(enabled)
    fun toggleZoneAlerts(enabled: Boolean) = settingsPreferences.setZoneAlerts(enabled)
    fun toggleJourneyReminders(enabled: Boolean) = settingsPreferences.setJourneyReminders(enabled)
    fun toggleSoundAlerts(enabled: Boolean) = settingsPreferences.setSoundAlerts(enabled)
    fun toggleVibration(enabled: Boolean) = settingsPreferences.setVibration(enabled)

    // ── Emergency Preferences ─────────────────────────────────────────────────

    fun toggleShakeToSos(enabled: Boolean) = settingsPreferences.setShakeToSos(enabled)
    fun setSosCountdownSeconds(seconds: Int) = settingsPreferences.setSosCountdownSeconds(seconds)
    fun toggleAutoRecordAudio(enabled: Boolean) = settingsPreferences.setAutoRecordAudio(enabled)
    fun toggleSmsFallback(enabled: Boolean) = settingsPreferences.setSmsFallback(enabled)
    fun toggleBatterySaverSafeMode(enabled: Boolean) = settingsPreferences.setBatterySaverSafeMode(enabled)

    // ── Permissions Check ─────────────────────────────────────────────────────

    fun checkPermissions(context: Context) {
        val fineLocationGranted = LocationPermissionHelper.hasFineLocationPermission(context)
        val coarseLocationGranted = LocationPermissionHelper.hasCoarseLocationPermission(context)
        val notificationGranted = NotificationPermissionHelper.canPostNotifications(context)

        _uiState.update {
            it.copy(
                hasFineLocationPermission = fineLocationGranted,
                hasCoarseLocationPermission = coarseLocationGranted,
                hasNotificationPermission = notificationGranted
            )
        }
    }

    // ── Modals / Dialogs ──────────────────────────────────────────────────────

    fun openModal(modal: SettingsModal) {
        _uiState.update { it.copy(activeModal = modal) }
    }

    fun dismissModal() {
        _uiState.update { it.copy(activeModal = SettingsModal.NONE) }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun confirmLogout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, activeModal = SettingsModal.NONE) }
            try {
                authRepository.logout()
                userRepository.clearUsers()
            } catch (_: Exception) {
                // Ensure logout navigation occurs even if cleanup encounters network/db issue
            } finally {
                _uiState.update { it.copy(isLoggingOut = false) }
                onLoggedOut()
            }
        }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val authRepository: AuthRepository = SamrakshaApp.instance.authRepository,
        private val userRepository: UserRepository = SamrakshaApp.instance.userRepository,
        private val settingsPreferences: SettingsPreferences = SamrakshaApp.instance.settingsPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(authRepository, userRepository, settingsPreferences) as T
        }
    }
}
