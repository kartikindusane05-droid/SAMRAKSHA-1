package com.samraksha.app.notifications

/**
 * Central registry of all SAMRAKSHA notification channels.
 *
 * Design decisions:
 *  - Channel IDs are stable constants — never change them after shipping, as the OS
 *    links user-controlled channel settings to these IDs permanently.
 *  - Each channel maps to a discrete safety concern with an appropriate importance
 *    level so Android (and the user) can tune alerts to their preferences.
 *  - Only events that genuinely require the user's immediate attention use HIGH
 *    importance (heads-up pop). Persistent status updates use LOW/MIN.
 */
object NotificationChannels {

    // ── Channel IDs ───────────────────────────────────────────────────────────

    /**
     * Silent ongoing notification shown while the Journey Safety Sentinel
     * foreground service is running. Always visible; never intrusive.
     * Importance: LOW — no sound, no heads-up pop.
     */
    const val JOURNEY_ONGOING = "samraksha_journey_sentinel"

    /**
     * Transient notification fired when a new journey is started or completed.
     * Importance: DEFAULT — ambient chime, no pop-over banner.
     */
    const val JOURNEY_EVENTS = "samraksha_journey_events"

    /**
     * Heads-up notification fired when the safety monitor detects a risk
     * level escalation (SAFE → MODERATE or MODERATE → HIGH) during a journey.
     * Importance: HIGH — heads-up banner + sound to grab the user's attention.
     */
    const val SAFETY_WARNINGS = "samraksha_safety_warnings"

    /**
     * Heads-up notification fired the instant an SOS emergency is activated.
     * Importance: HIGH — must be impossible to miss.
     */
    const val EMERGENCY_ACTIVATED = "samraksha_emergency_active"

    /**
     * Informational notification for emergency lifecycle updates such as
     * CANCELLED or RESOLVED status transitions.
     * Importance: DEFAULT — ambient; user is already aware the emergency existed.
     */
    const val EMERGENCY_STATUS = "samraksha_emergency_status"

    // ── Display names & descriptions (shown in system Settings → App → Notifications) ──

    const val JOURNEY_ONGOING_NAME = "Journey Safety Monitor"
    const val JOURNEY_ONGOING_DESC = "Persistent status shown while actively monitoring a journey"

    const val JOURNEY_EVENTS_NAME = "Journey Events"
    const val JOURNEY_EVENTS_DESC = "Notifications when a journey is started or completed"

    const val SAFETY_WARNINGS_NAME = "Safety Warnings"
    const val SAFETY_WARNINGS_DESC = "Urgent alerts when a risk level change is detected during a journey"

    const val EMERGENCY_ACTIVATED_NAME = "Emergency Activated"
    const val EMERGENCY_ACTIVATED_DESC = "Critical notification fired when an SOS emergency is triggered"

    const val EMERGENCY_STATUS_NAME = "Emergency Status Updates"
    const val EMERGENCY_STATUS_DESC = "Updates when an emergency event is cancelled or resolved"
}
