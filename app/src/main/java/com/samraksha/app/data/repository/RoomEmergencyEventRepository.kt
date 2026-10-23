package com.samraksha.app.data.repository

import com.samraksha.app.data.local.SessionManager
import com.samraksha.app.data.local.dao.EmergencyEventDao
import com.samraksha.app.data.local.entity.EmergencyEventEntity
import com.samraksha.app.data.local.entity.toDomainModel
import com.samraksha.app.data.local.entity.toEntity
import com.samraksha.app.data.model.EmergencyEventDto
import com.samraksha.app.data.model.UpdateEventStatusRequest
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.EmergencyEventApiService
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType
import com.samraksha.app.domain.repository.EmergencyEventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first implementation of [EmergencyEventRepository].
 *
 * Every write goes to the local Room database first.
 * The [isSynced] flag marks events that still need to be uploaded
 * to the backend once connectivity is restored.
 */
class RoomEmergencyEventRepository(
    private val dao: EmergencyEventDao,
    private val apiService: EmergencyEventApiService = ApiClient.emergencyEventApiService,
    private val sessionManager: SessionManager = SessionManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EmergencyEventRepository {

    override fun getAllEvents(): Flow<List<EmergencyEvent>> {
        return dao.getAllEvents().map { list -> list.map { it.toDomainModel() } }
    }

    override fun getEventById(eventId: String): Flow<EmergencyEvent?> {
        return dao.getEventById(eventId).map { it?.toDomainModel() }
    }

    override fun getUnsyncedEvents(): Flow<List<EmergencyEvent>> {
        return dao.getUnsyncedEvents().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun createEvent(event: EmergencyEvent) = withContext(ioDispatcher) {
        // 1. Write locally first with isSynced = false
        val entity = event.toEntity().copy(isSynced = false)
        dao.insertEvent(entity)

        // 2. Attempt push to backend immediately
        try {
            val userId = sessionManager.getUser()?.id ?: "user_demo_001"
            val dto = EmergencyEventDto(
                id = event.id,
                timestamp = event.timestamp,
                latitude = event.latitude,
                longitude = event.longitude,
                status = event.status.name,
                eventType = event.eventType.name,
                address = event.address,
                notes = event.notes,
                resolvedAt = event.resolvedAt,
                userId = userId
            )
            val response = apiService.createEvent(userId = userId, event = dto)
            if (response.isSuccessful && response.body()?.success == true) {
                dao.markSynced(event.id)
            }
        } catch (_: Exception) {
            // Offline: remains persisted in Room with isSynced = false
        }
    }

    override suspend fun updateStatus(eventId: String, status: EmergencyEventStatus, resolvedAt: Long?) =
        withContext(ioDispatcher) {
            // 1. Update status locally in Room
            dao.updateStatus(id = eventId, status = status.name, resolvedAt = resolvedAt)

            // 2. Attempt push to backend
            try {
                val response = apiService.updateStatus(
                    id = eventId,
                    body = UpdateEventStatusRequest(status = status.name, resolvedAt = resolvedAt)
                )
                if (response.isSuccessful) {
                    dao.markSynced(eventId)
                }
            } catch (_: Exception) {
                // Offline: preserved locally
            }
        }

    override suspend fun markSynced(eventId: String) = withContext(ioDispatcher) {
        dao.markSynced(eventId)
    }

    override suspend fun deleteEvent(eventId: String) = withContext(ioDispatcher) {
        dao.deleteEvent(eventId)
    }

    override suspend fun syncEvents(): Result<List<EmergencyEvent>> = withContext(ioDispatcher) {
        val userId = sessionManager.getUser()?.id ?: "user_demo_001"
        try {
            // 1. Upload unsynced events (deduplicated by ID)
            val unsyncedEntities = dao.getUnsyncedEventsList().distinctBy { it.id }
            if (unsyncedEntities.isNotEmpty()) {
                val dtos = unsyncedEntities.map { entity ->
                    EmergencyEventDto(
                        id = entity.id,
                        timestamp = entity.timestamp,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        status = entity.status,
                        eventType = entity.eventType,
                        address = entity.address,
                        notes = entity.notes,
                        resolvedAt = entity.resolvedAt,
                        userId = userId
                    )
                }
                val syncResponse = apiService.syncEvents(userId = userId, events = dtos)
                if (syncResponse.isSuccessful) {
                    unsyncedEntities.forEach { dao.markSynced(it.id) }
                }
            }

            // 2. Pull remote events and merge safely without duplicating or overwriting local unsynced edits
            val getResponse = apiService.getEvents(userId = userId)
            if (getResponse.isSuccessful && getResponse.body()?.data != null) {
                val remoteEvents = getResponse.body()!!.data!!
                val currentLocal = dao.getAllEventsList().associateBy { it.id }
                val toInsert = mutableListOf<EmergencyEventEntity>()

                for (dto in remoteEvents) {
                    val local = currentLocal[dto.id]
                    if (local == null) {
                        // Check if an existing event matches on timestamp, type, and coordinates to prevent duplicate records
                        val isFuzzyDuplicate = currentLocal.values.any { existing ->
                            Math.abs(existing.timestamp - dto.timestamp) < 5000L &&
                            existing.eventType == dto.eventType &&
                            Math.abs(existing.latitude - dto.latitude) < 0.0001 &&
                            Math.abs(existing.longitude - dto.longitude) < 0.0001
                        }
                        if (!isFuzzyDuplicate) {
                            toInsert.add(
                                EmergencyEventEntity(
                                    id = dto.id,
                                    timestamp = dto.timestamp,
                                    latitude = dto.latitude,
                                    longitude = dto.longitude,
                                    status = dto.status,
                                    eventType = dto.eventType,
                                    address = dto.address,
                                    notes = dto.notes,
                                    isSynced = true,
                                    resolvedAt = dto.resolvedAt
                                )
                            )
                        }
                    } else if (local.isSynced) {
                        // Local copy is already synced; update with authoritative server state
                        toInsert.add(
                            local.copy(
                                status = dto.status,
                                resolvedAt = dto.resolvedAt ?: local.resolvedAt,
                                address = dto.address ?: local.address,
                                notes = dto.notes ?: local.notes,
                                isSynced = true
                            )
                        )
                    }
                    // If local is currently unsynced (isSynced == false), retain local offline state until pushed!
                }

                if (toInsert.isNotEmpty()) {
                    dao.insertEvents(toInsert)
                }
            }

            val all = dao.getAllEvents().first().map { it.toDomainModel() }
            Result.success(all)
        } catch (_: Exception) {
            // Offline gracefully: return local events
            val local = dao.getAllEvents().first().map { it.toDomainModel() }
            Result.success(local)
        }
    }
}
