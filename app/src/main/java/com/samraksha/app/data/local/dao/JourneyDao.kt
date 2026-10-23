package com.samraksha.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samraksha.app.data.local.entity.JourneyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journeys ORDER BY startTime DESC")
    fun getAllJourneys(): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM journeys ORDER BY startTime DESC")
    suspend fun getAllJourneysList(): List<JourneyEntity>

    @Query("SELECT * FROM journeys WHERE isSynced = 0")
    suspend fun getUnsyncedJourneys(): List<JourneyEntity>

    @Query("SELECT COUNT(*) FROM journeys WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM journeys WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveJourney(): Flow<JourneyEntity?>

    @Query("SELECT * FROM journeys WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveJourneySync(): JourneyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneys(journeys: List<JourneyEntity>)

    @Query("UPDATE journeys SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE journeys SET status = :status, completedTime = :completedTime, isSynced = 0 WHERE id = :id")
    suspend fun updateJourneyStatus(id: String, status: String, completedTime: Long? = null)

    @Query("UPDATE journeys SET currentLatitude = :latitude, currentLongitude = :longitude, isSynced = 0 WHERE id = :id")
    suspend fun updateCurrentLocation(id: String, latitude: Double, longitude: Double)

    @Query("DELETE FROM journeys WHERE id = :id")
    suspend fun deleteJourney(id: String)

    @Query("DELETE FROM journeys")
    suspend fun clearAll()
}
