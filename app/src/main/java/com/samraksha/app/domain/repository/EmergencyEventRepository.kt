package com.samraksha.app.domain.repository

import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import kotlinx.coroutines.flow.Flow

interface EmergencyEventRepository {
    /** Observe all events, newest-first. */
    fun getAllEvents(): Flow<List<EmergencyEvent>>

    /** Observe a single event by ID. */
    fun getEventById(eventId: String): Flow<EmergencyEvent?>

    /** Observe events not yet synced with the backend. */
    fun getUnsyncedEvents(): Flow<List<EmergencyEvent>>

    /** Write a new event locally (always succeeds, even offline). */
    suspend fun createEvent(event: EmergencyEvent)

    /** Transition a stored event to a new status. */
    suspend fun updateStatus(eventId: String, status: EmergencyEventStatus, resolvedAt: Long? = null)

    /** Mark an event as successfully uploaded to the backend. */
    suspend fun markSynced(eventId: String)

    /** Delete an event record. */
    suspend fun deleteEvent(eventId: String)

    /** Synchronize events with the backend (push unsynced and pull latest). */
    suspend fun syncEvents(): Result<List<EmergencyEvent>>
}
