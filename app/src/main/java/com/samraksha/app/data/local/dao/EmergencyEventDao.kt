package com.samraksha.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samraksha.app.data.local.entity.EmergencyEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyEventDao {

    /** Observe all events ordered newest-first. */
    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EmergencyEventEntity>>

    /** Observe a single event by its ID. */
    @Query("SELECT * FROM emergency_events WHERE id = :id LIMIT 1")
    fun getEventById(id: String): Flow<EmergencyEventEntity?>

    /** Observe events that have not yet been uploaded to the backend. */
    @Query("SELECT * FROM emergency_events WHERE isSynced = 0 ORDER BY timestamp ASC")
    fun getUnsyncedEvents(): Flow<List<EmergencyEventEntity>>

    @Query("SELECT * FROM emergency_events WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedEventsList(): List<EmergencyEventEntity>

    @Query("SELECT * FROM emergency_events ORDER BY timestamp DESC")
    suspend fun getAllEventsList(): List<EmergencyEventEntity>

    @Query("SELECT COUNT(*) FROM emergency_events WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    /** Insert a new event (local-first write). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EmergencyEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EmergencyEventEntity>)

    /** Transition an event to a new status (ACTIVE → CANCELLED / RESOLVED). Sets isSynced = 0 for offline sync. */
    @Query("UPDATE emergency_events SET status = :status, resolvedAt = :resolvedAt, isSynced = 0 WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, resolvedAt: Long? = null)

    /** Mark an event as successfully synced with the backend. */
    @Query("UPDATE emergency_events SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    /** Delete an event by ID. */
    @Query("DELETE FROM emergency_events WHERE id = :id")
    suspend fun deleteEvent(id: String)

    @Query("DELETE FROM emergency_events")
    suspend fun clearAll()
}
