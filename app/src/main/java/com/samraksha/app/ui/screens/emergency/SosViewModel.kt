package com.samraksha.app.ui.screens.emergency

import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.local.SettingsPreferences
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.EmergencyEventRepository
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.emergency.AlertDeliveryState
import com.samraksha.app.emergency.ContactAlertDelivery
import com.samraksha.app.emergency.EmergencyAlertDispatcher
import com.samraksha.app.emergency.ShakeDetector
import com.samraksha.app.location.LocationClient
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SosState {
    IDLE,       // Default — ready for manual hold
    ARMING,     // User is holding the button — hold progress running
    ARMED,      // Hold complete — final 3-second countdown before activation
    ACTIVATING, // In-flight: obtaining fresh GPS and dispatching alerts
    ACTIVATED,  // SOS is live; alerts dispatched; event stored in Room & backend
    CANCELLED   // User cancelled — event marked CANCELLED in Room
}

data class SosUiState(
    val state: SosState = SosState.IDLE,
    val holdProgress: Float = 0f,
    val countdownSeconds: Int = COUNTDOWN_SECONDS,
    val activeEventId: String? = null,
    val userLocation: UserLocation? = null,
    val locationError: String? = null,
    val isFetchingLocation: Boolean = false,
    val contactDeliveries: List<ContactAlertDelivery> = emptyList(),
    val deliverySummary: String? = null,
    val activeAlertMessage: String? = null,
    val generalError: String? = null,
    val isShakeToSosEnabled: Boolean = false,
    val isShakeListening: Boolean = false,
    val isShakeTriggered: Boolean = false
)

private const val HOLD_DURATION_MS = 3000L
private const val COUNTDOWN_SECONDS = 3

class SosViewModel(
    private val eventRepository: EmergencyEventRepository,
    private val contactRepository: EmergencyContactRepository? = null,
    private val locationClient: LocationClient? = null,
    private val alertDispatcher: EmergencyAlertDispatcher? = null,
    private val userRepository: UserRepository? = null,
    private val settingsPreferences: SettingsPreferences? = null,
    shakeDetectorProvider: ((onThreeShakes: () -> Unit) -> ShakeDetector)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    val shakeDetector: ShakeDetector = shakeDetectorProvider?.invoke { onThreeShakesDetected() }
        ?: ShakeDetector { onThreeShakesDetected() }

    private var holdJob: Job? = null
    private var countdownJob: Job? = null

    init {
        val activePrefs = settingsPreferences ?: try {
            SamrakshaApp.instance.settingsPreferences
        } catch (_: Exception) {
            null
        }

        if (activePrefs != null) {
            viewModelScope.launch {
                activePrefs.shakeToSos.collect { enabled ->
                    _uiState.update { it.copy(isShakeToSosEnabled = enabled) }
                    if (!enabled && shakeDetector.isListening) {
                        shakeDetector.stopListening()
                        _uiState.update { it.copy(isShakeListening = false) }
                    }
                }
            }
        }
    }

    // ── Shake SOS Detection (Foreground Only) ─────────────────────────────────

    /**
     * Triggered when exactly 3 deliberate qualifying shakes are detected while foreground-active.
     * Starts the visible 3-second activation countdown with immediate abort capability.
     */
    fun onThreeShakesDetected() {
        if (!_uiState.value.isShakeToSosEnabled) return
        val currentState = _uiState.value.state
        if (currentState != SosState.IDLE && currentState != SosState.ARMING) return

        holdJob?.cancel()
        _uiState.update {
            it.copy(
                state = SosState.ARMED,
                holdProgress = 1f,
                countdownSeconds = COUNTDOWN_SECONDS,
                isShakeTriggered = true,
                generalError = null
            )
        }
        startActivationCountdown()
    }

    /**
     * Called when the SOS screen becomes visible and the Activity is in the RESUMED state.
     * Begins listening to accelerometer sensor events strictly in the foreground.
     */
    fun onScreenResumed(context: Context?) {
        val enabled = _uiState.value.isShakeToSosEnabled
        if (!enabled) {
            _uiState.update { it.copy(isShakeListening = false) }
            return
        }
        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val registered = shakeDetector.startListening(sensorManager)
        _uiState.update { it.copy(isShakeListening = registered) }
    }

    /**
     * Called when the SOS screen is paused, stopped, or obscured.
     * Immediately unregisters sensor listener and cancels any uncommitted countdown/hold.
     * Never dispatches an SOS in the background.
     */
    fun onScreenPaused() {
        shakeDetector.stopListening()
        _uiState.update { it.copy(isShakeListening = false) }

        val currentState = _uiState.value.state
        if (currentState == SosState.ARMED || currentState == SosState.ARMING) {
            countdownJob?.cancel()
            holdJob?.cancel()
            shakeDetector.reset()
            _uiState.update {
                it.copy(
                    state = SosState.IDLE,
                    holdProgress = 0f,
                    countdownSeconds = COUNTDOWN_SECONDS,
                    isShakeTriggered = false
                )
            }
        }
    }

    // ── Manual Hold gesture ───────────────────────────────────────────────────

    fun onHoldStart() {
        if (_uiState.value.state != SosState.IDLE) return
        _uiState.update { it.copy(state = SosState.ARMING, holdProgress = 0f, generalError = null) }

        holdJob = viewModelScope.launch {
            val steps = 60
            val stepMs = HOLD_DURATION_MS / steps
            for (i in 1..steps) {
                delay(stepMs)
                _uiState.update { it.copy(holdProgress = i.toFloat() / steps) }
            }
            onHoldComplete()
        }
    }

    fun onHoldRelease() {
        if (_uiState.value.state == SosState.ARMING) {
            holdJob?.cancel()
            _uiState.update { it.copy(state = SosState.IDLE, holdProgress = 0f) }
        }
    }

    // ── Activation countdown flow ─────────────────────────────────────────────

    private fun onHoldComplete() {
        _uiState.update {
            it.copy(state = SosState.ARMED, holdProgress = 1f, countdownSeconds = COUNTDOWN_SECONDS)
        }
        startActivationCountdown()
    }

    private fun startActivationCountdown() {
        countdownJob = viewModelScope.launch {
            for (remaining in COUNTDOWN_SECONDS - 1 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(countdownSeconds = remaining) }
            }
            activateSos()
        }
    }

    /**
     * Activates SOS deliberately after confirmation.
     * Prevents duplicate requests, fetches freshest GPS fix, dispatches alerts
     * to configured emergency contacts, and records the event in Room & backend.
     */
    fun activateSos() {
        val currentState = _uiState.value.state
        if (currentState == SosState.ACTIVATED || currentState == SosState.ACTIVATING) {
            return // Prevent duplicate requests from repeated taps
        }

        countdownJob?.cancel()
        holdJob?.cancel()
        shakeDetector.stopListening()

        _uiState.update {
            it.copy(
                state = SosState.ACTIVATING,
                isFetchingLocation = true,
                isShakeListening = false,
                generalError = null
            )
        }

        viewModelScope.launch {
            // 1. Obtain freshest available location with fallback
            var userLocation: UserLocation? = null
            var locError: String? = null
            try {
                val locResult = locationClient?.getCurrentLocation(highAccuracy = true)
                if (locResult is LocationState.Success) {
                    userLocation = locResult.location
                } else {
                    userLocation = locationClient?.getLastKnownLocation()
                    if (userLocation == null) {
                        locError = "Unable to acquire current GPS coordinates. Ensure location is enabled."
                    }
                }
            } catch (e: Exception) {
                userLocation = locationClient?.getLastKnownLocation()
                if (userLocation == null) {
                    locError = e.localizedMessage ?: "GPS location request failed"
                }
            }

            // 2. Fetch authenticated user full name for personalized alert
            val userName = try {
                userRepository?.getPrimaryUserDirect()?.fullName
                    ?: SamrakshaApp.instance.sessionManager.getUser()?.fullName
            } catch (_: Exception) {
                null
            }

            // 3. Prepare formatted emergency message
            val alertMessage = alertDispatcher?.formatEmergencyMessage(userName, userLocation)
                ?: ("EMERGENCY SOS ALERT! " + (userName ?: "User") + " has triggered an emergency alert in SAMRAKSHA.")

            // 4. Retrieve configured emergency contacts
            val contacts = try {
                contactRepository?.getEmergencyContacts()?.first() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            // 5. Dispatch alerts to configured contacts
            val deliveries = if (contacts.isNotEmpty()) {
                alertDispatcher?.dispatchAlerts(contacts, alertMessage) ?: contacts.map {
                    ContactAlertDelivery(
                        contactId = it.id,
                        contactName = it.name,
                        phoneNumber = it.phoneNumber,
                        status = AlertDeliveryState.FAILED_UNKNOWN,
                        errorMessage = "No dispatcher configured"
                    )
                }
            } else {
                emptyList()
            }

            // 6. Generate delivery summary
            val summary = if (contacts.isEmpty()) {
                "No emergency contacts configured. Please add trusted guardians to send automatic alerts."
            } else {
                val sentCount = deliveries.count { it.status == AlertDeliveryState.SENT || it.status == AlertDeliveryState.DELIVERED }
                val failedCount = deliveries.count { it.status != AlertDeliveryState.SENT && it.status != AlertDeliveryState.DELIVERED }
                if (failedCount == 0) {
                    "Alerts sent successfully to all $sentCount contact(s)."
                } else if (sentCount > 0) {
                    "Alerts sent to $sentCount contact(s); $failedCount delivery failed or pending."
                } else {
                    "Automatic delivery failed for ${contacts.size} contact(s). Use messaging fallback."
                }
            }

            // 7. Create unique emergency event record
            val eventId = UUID.randomUUID().toString()
            val isShake = _uiState.value.isShakeTriggered
            val noteSuffix = if (isShake) " (Three-Shake Foreground Trigger)" else ""
            val eventNotes = summary + noteSuffix

            val event = EmergencyEvent(
                id = eventId,
                timestamp = System.currentTimeMillis(),
                latitude = userLocation?.latitude ?: 0.0,
                longitude = userLocation?.longitude ?: 0.0,
                status = EmergencyEventStatus.ACTIVE,
                eventType = EmergencyEventType.MANUAL_SOS,
                notes = eventNotes,
                isSynced = false
            )
            eventRepository.createEvent(event)

            // 8. Update UI state to live ACTIVATED
            _uiState.update {
                it.copy(
                    state = SosState.ACTIVATED,
                    activeEventId = eventId,
                    userLocation = userLocation,
                    locationError = locError,
                    isFetchingLocation = false,
                    contactDeliveries = deliveries,
                    deliverySummary = summary,
                    activeAlertMessage = alertMessage
                )
            }

            // 9. Post persistent local notification
            try {
                SamrakshaApp.instance.notificationManager.notifyEmergencyActivated(event.eventType)
            } catch (_: Exception) {}
        }
    }

    /**
     * Retries GPS location acquisition for an already active emergency,
     * updates coordinates, and dispatches an updated alert link.
     */
    fun retryLocationAndResendAlert() {
        val state = _uiState.value
        if (state.state != SosState.ACTIVATED) return

        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true) }
            val freshLocation = try {
                (locationClient?.getCurrentLocation(highAccuracy = true) as? LocationState.Success)?.location
                    ?: locationClient?.getLastKnownLocation()
            } catch (_: Exception) {
                null
            }

            if (freshLocation != null) {
                val userName = try {
                    userRepository?.getPrimaryUserDirect()?.fullName
                        ?: SamrakshaApp.instance.sessionManager.getUser()?.fullName
                } catch (_: Exception) { null }

                val updatedMessage = alertDispatcher?.formatEmergencyMessage(userName, freshLocation)
                    ?: ("EMERGENCY SOS ALERT! Updated location: ${freshLocation.latitude}, ${freshLocation.longitude}")

                val contacts = try {
                    contactRepository?.getEmergencyContacts()?.first() ?: emptyList()
                } catch (_: Exception) { emptyList() }

                val deliveries = if (contacts.isNotEmpty()) {
                    alertDispatcher?.dispatchAlerts(contacts, updatedMessage) ?: emptyList()
                } else emptyList()

                _uiState.update {
                    it.copy(
                        userLocation = freshLocation,
                        locationError = null,
                        isFetchingLocation = false,
                        contactDeliveries = if (deliveries.isNotEmpty()) deliveries else it.contactDeliveries,
                        activeAlertMessage = updatedMessage
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        locationError = "Retry failed. Unable to acquire GPS lock. Please check location settings."
                    )
                }
            }
        }
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    /**
     * Cancels an in-progress or active SOS.
     * Updates the Room and backend record to CANCELLED.
     */
    fun cancel() {
        countdownJob?.cancel()
        holdJob?.cancel()
        shakeDetector.reset()

        val currentEventId = _uiState.value.activeEventId
        val wasActivated = _uiState.value.state == SosState.ACTIVATED

        _uiState.update {
            SosUiState(
                state = SosState.CANCELLED,
                isShakeToSosEnabled = it.isShakeToSosEnabled,
                isShakeListening = shakeDetector.isListening
            )
        }

        if (currentEventId != null) {
            viewModelScope.launch {
                eventRepository.updateStatus(currentEventId, EmergencyEventStatus.CANCELLED)
            }
        }

        if (wasActivated || currentEventId != null) {
            try {
                SamrakshaApp.instance.notificationManager
                    .notifyEmergencyStatusUpdate(EmergencyEventStatus.CANCELLED)
            } catch (_: Exception) {}
        }
    }

    /** Return to IDLE — called when user acknowledges cancelled/safe state. */
    fun reset() {
        countdownJob?.cancel()
        holdJob?.cancel()
        shakeDetector.reset()
        _uiState.update {
            SosUiState(
                isShakeToSosEnabled = it.isShakeToSosEnabled,
                isShakeListening = shakeDetector.isListening
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        holdJob?.cancel()
        shakeDetector.stopListening()
    }

    // ── ViewModel Factory ─────────────────────────────────────────────────────

    class Factory(
        private val repository: EmergencyEventRepository = SamrakshaApp.instance.emergencyEventRepository,
        private val contactRepository: EmergencyContactRepository = SamrakshaApp.instance.emergencyContactRepository,
        private val locationClient: LocationClient? = SamrakshaApp.instance.locationClient,
        private val alertDispatcher: EmergencyAlertDispatcher = SamrakshaApp.instance.emergencyAlertDispatcher,
        private val userRepository: UserRepository = SamrakshaApp.instance.userRepository,
        private val settingsPreferences: SettingsPreferences = SamrakshaApp.instance.settingsPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SosViewModel(
                repository,
                contactRepository,
                locationClient,
                alertDispatcher,
                userRepository,
                settingsPreferences
            ) as T
        }
    }
}
