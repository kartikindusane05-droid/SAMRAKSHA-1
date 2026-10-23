package com.samraksha.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samraksha.app.domain.model.Journey
import com.samraksha.app.domain.model.JourneyStatus

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey val id: String,
    val startLocationName: String,
    val endLocationName: String,
    val startTime: Long,
    val expectedEndTime: Long = 0L,
    val status: String = "IN_PROGRESS", // e.g. "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val completedTime: Long? = null,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0,
    val isSynced: Boolean = false
)

fun JourneyEntity.toDomainModel(): Journey = Journey(
    id = id,
    startLocationName = startLocationName,
    endLocationName = endLocationName,
    startTime = startTime,
    expectedEndTime = expectedEndTime,
    completedTime = completedTime,
    status = try { JourneyStatus.valueOf(status) } catch (_: Exception) { JourneyStatus.IN_PROGRESS },
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    currentLatitude = currentLatitude,
    currentLongitude = currentLongitude,
    destinationLatitude = destinationLatitude,
    destinationLongitude = destinationLongitude
)

fun Journey.toEntity(isSynced: Boolean = false): JourneyEntity = JourneyEntity(
    id = id,
    startLocationName = startLocationName,
    endLocationName = endLocationName,
    startTime = startTime,
    expectedEndTime = expectedEndTime,
    completedTime = completedTime,
    status = status.name,
    startLatitude = startLatitude,
    startLongitude = startLongitude,
    currentLatitude = currentLatitude,
    currentLongitude = currentLongitude,
    destinationLatitude = destinationLatitude,
    destinationLongitude = destinationLongitude,
    isSynced = isSynced
)
