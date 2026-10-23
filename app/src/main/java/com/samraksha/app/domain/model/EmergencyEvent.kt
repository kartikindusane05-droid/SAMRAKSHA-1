package com.samraksha.app.domain.model

/**
 * Lifecycle states of an emergency event.
 *
 * ACTIVE    — SOS has been triggered; alert is live.
 * CANCELLED — User cancelled before resolution.
 * RESOLVED  — Emergency is over; safe confirmation recorded.
 */
enum class EmergencyEventStatus {
    ACTIVE,
    CANCELLED,
    RESOLVED
}

/**
 * Types of emergency triggers supported in SAMRAKSHA.
 */
enum class EmergencyEventType(val displayName: String, val description: String) {
    MANUAL_SOS("Manual SOS", "Intentional press-and-hold trigger"),
    JOURNEY_TIMEOUT("Journey Alert", "Missed check-in during active journey"),
    GEOFENCE_ALERT("Geofence Deviation", "Unexpected route departure"),
    VOICE_TRIGGER("Voice SOS", "Emergency voice trigger activated"),
    IMPACT_DETECTION("Impact Detection", "Sudden motion or impact alert")
}

/**
 * Domain representation of a recorded emergency event.
 *
 * [isSynced] is false when the event is stored locally in Room but not yet
 * confirmed by the backend API.
 */
data class EmergencyEvent(
    val id: String,
    val timestamp: Long,          // Epoch millis
    val latitude: Double,
    val longitude: Double,
    val status: EmergencyEventStatus,
    val eventType: EmergencyEventType = EmergencyEventType.MANUAL_SOS,
    val address: String? = null,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val resolvedAt: Long? = null
)
