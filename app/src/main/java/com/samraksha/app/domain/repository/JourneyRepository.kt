package com.samraksha.app.domain.repository

import com.samraksha.app.data.local.entity.JourneyEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing journey tracking data.
 */
interface JourneyRepository {
    fun getAllJourneys(): Flow<List<JourneyEntity>>
    fun getActiveJourney(): Flow<JourneyEntity?>
    suspend fun startJourney(journey: JourneyEntity)
    suspend fun updateJourneyStatus(journeyId: String, status: String)
    suspend fun endJourney(journeyId: String, status: String = "COMPLETED")
    suspend fun updateCurrentLocation(journeyId: String, latitude: Double, longitude: Double)
    suspend fun deleteJourney(journeyId: String)
    suspend fun syncJourneys(): Result<List<JourneyEntity>>
}
