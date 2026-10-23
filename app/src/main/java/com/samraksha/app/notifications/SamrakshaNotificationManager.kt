package com.samraksha.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.samraksha.app.MainActivity
import com.samraksha.app.R
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType
import com.samraksha.app.domain.safety.JourneyRiskLevel

/**
 * Central dispatcher for all SAMRAKSHA user-facing notifications.
 *
 * ## Design principles
 * - **Single point of truth**: every notification in the app goes through this
 *   class so channel IDs, intents, and ID allocation are consistent.
 * - **No notification spam**: methods are designed around meaningful safety events
 *   (journey lifecycle, risk escalation, emergency activation/resolution).
 *   Routine/periodic updates (e.g. GPS tick) are handled silently by the
 *   foreground service's ongoing notification, not by this class.
 * - **Risk-escalation only**: Safety-warning notifications fire only when the
 *   risk level *increases* (SAFE→MODERATE or any→HIGH). Returning to SAFE is
 *   reflected in the foreground notification text, not via an extra alert.
 * - **Thread-safe**: [NotificationManager.notify] is thread-safe per Android
 *   docs; all methods may be called from any coroutine context.
 */
class SamrakshaNotificationManager(private val context: Context) {

    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // ── Notification ID allocation ────────────────────────────────────────────
    // Stable IDs: once assigned they must not change across releases so that
    // update calls (notify with same ID) correctly replace, not duplicate, notifs.

    private object NotifId {
        const val JOURNEY_ONGOING = 7821     // foreground service notification
        const val JOURNEY_STARTED = 7822
        const val JOURNEY_COMPLETED = 7823
        const val SAFETY_WARNING = 7824      // replaced on each escalation
        const val EMERGENCY_ACTIVE = 7825
        const val EMERGENCY_STATUS = 7826
    }

    // ── Channel creation ──────────────────────────────────────────────────────

    /**
     * Creates all notification channels. Safe to call multiple times — the OS
     * is idempotent for existing channels. Call from [Application.onCreate].
     */
    fun createChannels() {
        listOf(
            NotificationChannel(
                NotificationChannels.JOURNEY_ONGOING,
                NotificationChannels.JOURNEY_ONGOING_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = NotificationChannels.JOURNEY_ONGOING_DESC
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            },
            NotificationChannel(
                NotificationChannels.JOURNEY_EVENTS,
                NotificationChannels.JOURNEY_EVENTS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = NotificationChannels.JOURNEY_EVENTS_DESC
                setShowBadge(true)
            },
            NotificationChannel(
                NotificationChannels.SAFETY_WARNINGS,
                NotificationChannels.SAFETY_WARNINGS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NotificationChannels.SAFETY_WARNINGS_DESC
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                NotificationChannels.EMERGENCY_ACTIVATED,
                NotificationChannels.EMERGENCY_ACTIVATED_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NotificationChannels.EMERGENCY_ACTIVATED_DESC
                enableVibration(true)
                setShowBadge(true)
                // Emergency channel must not be silenceable by DND
                setBypassDnd(true)
            },
            NotificationChannel(
                NotificationChannels.EMERGENCY_STATUS,
                NotificationChannels.EMERGENCY_STATUS_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = NotificationChannels.EMERGENCY_STATUS_DESC
                setShowBadge(true)
            }
        ).forEach { manager.createNotificationChannel(it) }
    }

    // ── Tap intent (opens main app) ───────────────────────────────────────────

    private fun mainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // ── 1. Journey ongoing (foreground service status bar notification) ───────

    /**
     * Builds the persistent notification used by [JourneySafetyService].
     * Called by the service itself via [notifyJourneyOngoing].
     */
    fun buildJourneyOngoingNotification(
        riskLabel: String = "Monitoring active",
        riskEmoji: String = "🟢"
    ): android.app.Notification =
        NotificationCompat.Builder(context, NotificationChannels.JOURNEY_ONGOING)
            .setContentTitle("$riskEmoji SAMRAKSHA Journey Sentinel")
            .setContentText("$riskLabel — Tap to open safety dashboard")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    /** Updates the existing foreground service notification in-place. */
    fun notifyJourneyOngoing(riskLabel: String, riskEmoji: String) {
        manager.notify(NotifId.JOURNEY_ONGOING, buildJourneyOngoingNotification(riskLabel, riskEmoji))
    }

    // ── 2. Journey started ────────────────────────────────────────────────────

    /**
     * Fired once when the user taps "Start Journey". Informs the user that safety
     * monitoring is now active and where they can check it.
     */
    fun notifyJourneyStarted(destination: String) {
        val body = if (destination.isNotBlank())
            "Heading to $destination — safety monitoring is active"
        else
            "Journey started — safety monitoring is active"

        val notification = NotificationCompat.Builder(context, NotificationChannels.JOURNEY_EVENTS)
            .setContentTitle("🚀 Journey Started")
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(mainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        manager.notify(NotifId.JOURNEY_STARTED, notification)
    }

    // ── 3. Journey completed ──────────────────────────────────────────────────

    /**
     * Fired once when the user ends a journey. Provides a positive closure signal.
     */
    fun notifyJourneyCompleted(destination: String, durationLabel: String = "") {
        val body = buildString {
            if (destination.isNotBlank()) append("Arrived at $destination")
            else append("Journey complete")
            if (durationLabel.isNotBlank()) append(" · $durationLabel")
            append(" — You're safe!")
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.JOURNEY_EVENTS)
            .setContentTitle("✅ Journey Completed")
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(mainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        // Dismiss the "Journey started" badge and post completion
        manager.cancel(NotifId.JOURNEY_STARTED)
        manager.notify(NotifId.JOURNEY_COMPLETED, notification)
    }

    // ── 4. Safety warning ─────────────────────────────────────────────────────

    /**
     * Fired when the journey safety monitor detects a risk escalation.
     *
     * Only posts for [JourneyRiskLevel.MODERATE] and [JourneyRiskLevel.HIGH];
     * returning to SAFE is handled silently by updating the foreground notification.
     *
     * @param warningTitle  Short title from [JourneySafetyWarning.title].
     * @param warningMessage Body text from [JourneySafetyWarning.message].
     * @param riskLevel     The new risk level that triggered the warning.
     */
    fun notifySafetyWarning(
        warningTitle: String,
        warningMessage: String,
        riskLevel: JourneyRiskLevel
    ) {
        // Only alert on escalation, not on SAFE
        if (riskLevel == JourneyRiskLevel.SAFE) return

        val (emoji, priority) = when (riskLevel) {
            JourneyRiskLevel.MODERATE ->
                Pair("⚠️", NotificationCompat.PRIORITY_HIGH)
            JourneyRiskLevel.HIGH ->
                Pair("🚨", NotificationCompat.PRIORITY_MAX)
            JourneyRiskLevel.SAFE -> return   // guard — already filtered above
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.SAFETY_WARNINGS)
            .setContentTitle("$emoji $warningTitle")
            .setContentText(warningMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(warningMessage))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false) // always re-alert on risk escalation
            .setContentIntent(mainActivityIntent())
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        // Re-use the same ID so each new warning replaces the previous one
        manager.notify(NotifId.SAFETY_WARNING, notification)
    }

    // ── 5. Emergency activated ────────────────────────────────────────────────

    /**
     * Fired the instant the SOS is confirmed active. Uses the EMERGENCY channel
     * which bypasses DND to ensure it is never silenced.
     *
     * @param eventType  The trigger type (e.g. MANUAL_SOS, VOICE_TRIGGER).
     */
    fun notifyEmergencyActivated(eventType: EmergencyEventType) {
        val body = when (eventType) {
            EmergencyEventType.MANUAL_SOS ->
                "SOS has been triggered. Emergency contacts are being alerted. Stay calm."
            EmergencyEventType.JOURNEY_TIMEOUT ->
                "Missed check-in alert active. Emergency contacts have been notified."
            EmergencyEventType.GEOFENCE_ALERT ->
                "Unexpected route departure detected. Emergency contacts alerted."
            EmergencyEventType.VOICE_TRIGGER ->
                "Voice SOS activated. Emergency contacts are being alerted."
            EmergencyEventType.IMPACT_DETECTION ->
                "Impact detected. Emergency contacts are being alerted."
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.EMERGENCY_ACTIVATED)
            .setContentTitle("🆘 Emergency Activated")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)           // persists until explicitly cancelled
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        manager.notify(NotifId.EMERGENCY_ACTIVE, notification)
    }

    // ── 6. Emergency status update ────────────────────────────────────────────

    /**
     * Fired when an emergency event transitions to [EmergencyEventStatus.CANCELLED]
     * or [EmergencyEventStatus.RESOLVED]. Dismisses the active emergency notification
     * and replaces it with a status update.
     *
     * Does nothing for [EmergencyEventStatus.ACTIVE] since [notifyEmergencyActivated]
     * handles that.
     */
    fun notifyEmergencyStatusUpdate(newStatus: EmergencyEventStatus) {
        if (newStatus == EmergencyEventStatus.ACTIVE) return

        // Clear the persistent "Emergency Activated" banner
        manager.cancel(NotifId.EMERGENCY_ACTIVE)

        val (title, body) = when (newStatus) {
            EmergencyEventStatus.CANCELLED ->
                Pair("Emergency Cancelled", "The SOS has been cancelled. Stay safe.")
            EmergencyEventStatus.RESOLVED ->
                Pair("✅ Emergency Resolved", "The emergency has been marked as resolved. Stay safe.")
            EmergencyEventStatus.ACTIVE -> return  // guard
        }

        val notification = NotificationCompat.Builder(context, NotificationChannels.EMERGENCY_STATUS)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(mainActivityIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        manager.notify(NotifId.EMERGENCY_STATUS, notification)
    }

    // ── Convenience: dismiss safety warning ──────────────────────────────────

    /** Called when risk returns to SAFE to clear any lingering warning banner. */
    fun dismissSafetyWarning() {
        manager.cancel(NotifId.SAFETY_WARNING)
    }
}
