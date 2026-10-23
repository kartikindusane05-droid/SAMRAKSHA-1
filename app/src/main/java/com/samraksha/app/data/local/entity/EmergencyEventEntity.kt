package com.samraksha.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType

/**
 * Room entity for persisting emergency events.
 *
 * [isSynced] tracks whether the event has been successfully uploaded to the backend.
 * Events are always written locally first, ensuring reliability when offline.
 */
@Entity(tableName = "emergency_events")
data class EmergencyEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val status: String,                         // EmergencyEventStatus name
    val eventType: String = EmergencyEventType.MANUAL_SOS.name, // EmergencyEventType name
    val address: String? = null,
    val notes: String? = null,
    val isSynced: Boolean = false,
    val resolvedAt: Long? = null
)

fun EmergencyEventEntity.toDomainModel(): EmergencyEvent {
    return EmergencyEvent(
        id = id,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        status = runCatching { EmergencyEventStatus.valueOf(status) }.getOrDefault(EmergencyEventStatus.ACTIVE),
        eventType = runCatching { EmergencyEventType.valueOf(eventType) }.getOrDefault(EmergencyEventType.MANUAL_SOS),
        address = address,
        notes = notes,
        isSynced = isSynced,
        resolvedAt = resolvedAt
    )
}

fun EmergencyEvent.toEntity(): EmergencyEventEntity {
    return EmergencyEventEntity(
        id = id,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        status = status.name,
        eventType = eventType.name,
        address = address,
        notes = notes,
        isSynced = isSynced,
        resolvedAt = resolvedAt
    )
}
