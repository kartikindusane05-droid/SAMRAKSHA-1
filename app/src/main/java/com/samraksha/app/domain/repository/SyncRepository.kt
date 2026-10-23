package com.samraksha.app.domain.repository

import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.User
import com.samraksha.app.ui.screens.location.model.SafetyZone
import kotlinx.coroutines.flow.StateFlow

/**
 * Detailed outcome summary of a synchronization session.
 */
data class SyncSummary(
    val userSuccess: Boolean = false,
    val contactsSynced: Int = 0,
    val eventsSynced: Int = 0,
    val journeysSynced: Int = 0,
    val safetyZonesSynced: Int = 0,
    val durationMs: Long = 0L,
    val isFullySynced: Boolean = true
)

/**
 * Reactive state representing the synchronization status between app and backend.
 */
data class SyncState(
    val isSyncing: Boolean = false,
    val isOnline: Boolean = true,
    val lastSyncTimestamp: Long? = null,
    val lastSummary: SyncSummary? = null,
    val statusMessage: String = "Ready",
    val error: String? = null,
    val pendingItemsCount: Int = 0
)

/**
 * Central orchestrator interface coordinating synchronization across:
 * - User data
 * - Emergency contacts
 * - Emergency events
 * - Journeys
 * - Safety zones
 */
interface SyncRepository {

    val syncState: StateFlow<SyncState>

    /**
     * Executes full synchronization across all 5 modules.
     * Offline-first: if the device is temporarily offline, local cache is preserved.
     */
    suspend fun syncAll(): Result<SyncSummary>

    suspend fun syncUserData(): Result<User>
    suspend fun syncContacts(): Result<List<EmergencyContact>>
    suspend fun syncEmergencyEvents(): Result<List<EmergencyEvent>>
    suspend fun syncJourneys(): Result<List<JourneyEntity>>
    suspend fun syncSafetyZones(): Result<List<SafetyZone>>
}
