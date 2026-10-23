package com.samraksha.app.journey

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.samraksha.app.MainActivity
import com.samraksha.app.R
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.domain.repository.SafetyZoneLoadResult
import com.samraksha.app.domain.safety.JourneyRiskLevel
import com.samraksha.app.domain.safety.JourneySafetyMonitor
import com.samraksha.app.domain.safety.JourneySafetyReport
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.notifications.NotificationChannels
import com.samraksha.app.ui.screens.location.model.SafetyZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Battery-conscious foreground service for SAMRAKSHA journey safety monitoring.
 *
 * ## Design Goals
 * - Keep location tracking alive when the user backgrounds the app mid-journey.
 * - Use a *visible* status-bar notification (foregroundServiceType=location) rather
 *   than `ACCESS_BACKGROUND_LOCATION`, which Google Play restricts heavily and which
 *   drains battery indiscriminately.
 * - Respect the ViewModel's battery saver setting (15s / 30s intervals, 10m / 25m
 *   displacement thresholds) — no-op GPS callbacks when the device is stationary.
 * - Publish safety reports through a process-wide [SharedFlow] so the ViewModel can
 *   collect updates without IPC overhead.
 * - Stop itself immediately when the journey ends or is cancelled.
 */
class JourneySafetyService : Service() {

    // ── Service-scoped coroutine context ──────────────────────────────────────

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var locationJob: Job? = null
    private var cachedZones: List<SafetyZone> = emptyList()

    /** Tracks the previous risk level to detect escalation vs. de-escalation. */
    private var lastKnownRiskLevel: JourneyRiskLevel = JourneyRiskLevel.SAFE

    // ── Shared state exposed to the app process ───────────────────────────────

    companion object {
        /**
         * Action to start the sentinel service for a journey.
         * Extras: [EXTRA_JOURNEY_ID], [EXTRA_BATTERY_SAVER]
         */
        const val ACTION_START = "com.samraksha.app.journey.ACTION_START_SENTINEL"

        /** Action to stop the sentinel service. */
        const val ACTION_STOP = "com.samraksha.app.journey.ACTION_STOP_SENTINEL"

        /** Key for the journey ID string extra. */
        const val EXTRA_JOURNEY_ID = "journey_id"

        /** Key for the boolean battery-saver extra. */
        const val EXTRA_BATTERY_SAVER = "battery_saver"

        private const val NOTIFICATION_ID = 7821
        private const val CHANNEL_ID = NotificationChannels.JOURNEY_ONGOING

        /**
         * Process-wide stream of safety reports published while a journey is active.
         * Collected by [com.samraksha.app.ui.screens.journey.JourneyViewModel] to
         * merge foreground-service updates into the UI state.
         */
        private val _safetyReports = MutableSharedFlow<JourneySafetyReport>(
            replay = 1,
            extraBufferCapacity = 8
        )
        val safetyReports: SharedFlow<JourneySafetyReport> = _safetyReports.asSharedFlow()

        // ── Convenience factory methods ───────────────────────────────────────

        fun buildStartIntent(
            context: Context,
            journeyId: String,
            batterySaver: Boolean
        ): Intent = Intent(context, JourneySafetyService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_JOURNEY_ID, journeyId)
            putExtra(EXTRA_BATTERY_SAVER, batterySaver)
        }

        fun buildStopIntent(context: Context): Intent =
            Intent(context, JourneySafetyService::class.java).apply {
                action = ACTION_STOP
            }
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val journeyId = intent.getStringExtra(EXTRA_JOURNEY_ID)
                    ?: return START_NOT_STICKY
                val batterySaver = intent.getBooleanExtra(EXTRA_BATTERY_SAVER, false)

                // Verify location permission before starting foreground service (Android 14+ requirement)
                if (!LocationPermissionHelper.hasLocationPermission(this)) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Promote to foreground immediately so the OS does not kill us
                val notifMgr = SamrakshaApp.instance.notificationManager
                val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                } else {
                    0
                }
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notifMgr.buildJourneyOngoingNotification(),
                    foregroundServiceType
                )
                startLocationMonitoring(journeyId, batterySaver)
            }
            ACTION_STOP -> stopSelf()
            else -> Unit
        }
        // START_NOT_STICKY: do not auto-restart; journey must be re-started explicitly
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Location monitoring ───────────────────────────────────────────────────

    /**
     * Configures and starts periodic location updates scoped to this service.
     *
     * Battery-saver settings mirror those in JourneyViewModel.startLocationUpdates:
     *   Default : 15s interval, 10m displacement, high-accuracy GPS
     *   Saver   : 30s interval, 25m displacement, balanced-power mode
     *
     * The displacement threshold silences the GPS radio when stationary — the most
     * impactful battery optimisation for a location-tracking use case.
     */
    private fun startLocationMonitoring(journeyId: String, batterySaver: Boolean) {
        locationJob?.cancel()

        val app = SamrakshaApp.instance
        val locationClient = app.locationClient
        val safetyZoneRepository = app.safetyZoneRepository

        val intervalMs = if (batterySaver) 30_000L else 15_000L
        val minDistanceMeters = if (batterySaver) 25f else 10f
        val highAccuracy = !batterySaver

        // Pre-fetch cached zones for fast offline evaluation
        serviceScope.launch {
            val cached = safetyZoneRepository.getCachedSafetyZones()
            if (cached.isNotEmpty()) {
                cachedZones = cached
            }
        }

        locationJob = locationClient
            .getLocationUpdates(
                intervalMs = intervalMs,
                minDistanceMeters = minDistanceMeters,
                highAccuracy = highAccuracy
            )
            .onEach { state ->
                if (state is LocationState.Success) {
                    val loc = state.location

                    // Persist latest fix to the journey record (non-fatal if it fails)
                    try {
                        app.journeyRepository.updateCurrentLocation(
                            journeyId = journeyId,
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    } catch (_: Exception) { /* UI still reflects last known state */ }

                    // Lazily refresh zone cache when we have valid coordinates
                    if (cachedZones.isEmpty()) {
                        try {
                            val result = safetyZoneRepository.getSafetyZones(
                                lat = loc.latitude,
                                lng = loc.longitude,
                                radius = 5000.0
                            )
                            cachedZones = when (result) {
                                is SafetyZoneLoadResult.Success -> result.zones
                                is SafetyZoneLoadResult.Error -> result.cachedZones
                            }
                        } catch (_: Exception) { /* Safe defaults applied by evaluator */ }
                    }

                    // Pure in-memory spatial safety evaluation — zero network calls
                    val report = JourneySafetyMonitor.evaluateJourneySafety(
                        userLat = loc.latitude,
                        userLng = loc.longitude,
                        availableZones = cachedZones,
                        maxNearbyDistanceMeters = 1500.0
                    )

                    // Publish to ViewModel via the process-wide SharedFlow
                    _safetyReports.emit(report)

                    // Update the foreground notification and fire escalation alerts
                    updateNotification(report)
                }
            }
            .launchIn(serviceScope)
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    /**
     * Updates the foreground status-bar notification and fires a safety-warning
     * heads-up alert when the risk level *escalates*.
     *
     * De-escalation (risk drops) is intentionally silent — only reflected in
     * the ongoing notification text — to avoid alert fatigue.
     */
    private fun updateNotification(report: JourneySafetyReport) {
        val notifMgr = SamrakshaApp.instance.notificationManager
        val newLevel = report.currentRiskLevel

        val (emoji, label) = when (newLevel) {
            JourneyRiskLevel.SAFE ->
                Pair("🟢", "Safe Corridor — No nearby hazards")
            JourneyRiskLevel.MODERATE ->
                Pair("🟡", "Caution Advised — Moderate risk area")
            JourneyRiskLevel.HIGH ->
                Pair("🔴", "High Risk Alert — Stay vigilant")
        }

        // Always update the silent foreground notification text
        notifMgr.notifyJourneyOngoing(riskLabel = label, riskEmoji = emoji)

        // Only fire a heads-up warning on escalation, not on every tick
        val isEscalating = newLevel.ordinal > lastKnownRiskLevel.ordinal
        if (isEscalating && report.warnings.isNotEmpty()) {
            val topWarning = report.warnings.first()
            notifMgr.notifySafetyWarning(
                warningTitle = topWarning.title,
                warningMessage = topWarning.message,
                riskLevel = newLevel
            )
        } else if (newLevel == JourneyRiskLevel.SAFE && lastKnownRiskLevel != JourneyRiskLevel.SAFE) {
            // Risk returned to safe — clear the warning banner
            notifMgr.dismissSafetyWarning()
        }

        lastKnownRiskLevel = newLevel
    }
}
