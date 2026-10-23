package com.samraksha.app.domain.model

/**
 * Lifecycle status of a tracked journey.
 */
enum class JourneyStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

/**
 * Domain model representing a user journey.
 */
data class Journey(
    val id: String,
    val startLocationName: String,
    val endLocationName: String,
    val startTime: Long,
    val expectedEndTime: Long = 0L,
    val completedTime: Long? = null,
    val status: JourneyStatus = JourneyStatus.IN_PROGRESS,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val currentLatitude: Double = 0.0,
    val currentLongitude: Double = 0.0,
    val destinationLatitude: Double = 0.0,
    val destinationLongitude: Double = 0.0
)
