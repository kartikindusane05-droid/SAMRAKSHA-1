package com.samraksha.app.data.repository

import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.User
import com.samraksha.app.domain.repository.EmergencyContactRepository
import com.samraksha.app.domain.repository.EmergencyEventRepository
import com.samraksha.app.domain.repository.JourneyRepository
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.repository.SyncRepository
import com.samraksha.app.domain.repository.SyncState
import com.samraksha.app.domain.repository.SyncSummary
import com.samraksha.app.domain.repository.UserRepository
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [SyncRepository].
 * Coordinates synchronization across User data, Emergency contacts,
 * Emergency events, Journeys, and Safety zones.
 *
 * Automatically initiates sync when the device reconnects to the internet.
 * Uses Mutex to prevent duplicate/concurrent sync races and safe exponential backoff
 * on transient network failures.
 */
class SyncRepositoryImpl(
    private val userRepository: UserRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val emergencyEventRepository: EmergencyEventRepository,
    private val journeyRepository: JourneyRepository,
    private val safetyZoneRepository: SafetyZoneRepository,
    private val networkMonitor: NetworkMonitor,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SyncRepository {

    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow(
        SyncState(
            isOnline = networkMonitor.isConnected(),
            statusMessage = if (networkMonitor.isConnected()) "Ready to sync" else "Offline storage active"
        )
    )
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        // 1. Startup check: If currently online, trigger sync in background
        scope.launch {
            if (networkMonitor.isConnected()) {
                syncAll()
            }
        }

        // 2. Observe network connectivity transitions and auto-sync on reconnect
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                val wasOffline = !_syncState.value.isOnline
                _syncState.update {
                    it.copy(
                        isOnline = online,
                        statusMessage = if (online) "Online" else "Offline mode - changes stored locally"
                    )
                }

                // Auto-sync when reconnecting from offline
                if (online && wasOffline) {
                    syncAll()
                }
            }
        }
    }

    override suspend fun syncAll(): Result<SyncSummary> = syncMutex.withLock {
        withContext(ioDispatcher) {
            val startTime = System.currentTimeMillis()
            val isOnline = networkMonitor.isConnected()

            _syncState.update {
                it.copy(
                    isSyncing = true,
                    isOnline = isOnline,
                    statusMessage = if (isOnline) "Synchronizing with SAMRAKSHA cloud..." else "Reconciling offline data..."
                )
            }

            try {
                // Execute individual module syncs, retrying with backoff if online
                val userResult = if (isOnline) {
                    retryWithBackoff { syncUserData() }
                } else {
                    syncUserData()
                }
                val userSuccess = userResult.isSuccess

                val contactsResult = if (isOnline) {
                    retryWithBackoff { syncContacts() }
                } else {
                    syncContacts()
                }
                val contactsCount = contactsResult.getOrNull()?.size ?: 0

                val eventsResult = if (isOnline) {
                    retryWithBackoff { syncEmergencyEvents() }
                } else {
                    syncEmergencyEvents()
                }
                val eventsCount = eventsResult.getOrNull()?.size ?: 0

                val journeysResult = if (isOnline) {
                    retryWithBackoff { syncJourneys() }
                } else {
                    syncJourneys()
                }
                val journeysCount = journeysResult.getOrNull()?.size ?: 0

                val safetyZonesResult = if (isOnline) {
                    retryWithBackoff { syncSafetyZones() }
                } else {
                    syncSafetyZones()
                }
                val safetyZonesCount = safetyZonesResult.getOrNull()?.size ?: 0

                // If any module failed, propagate the exception to the catch handler
                val firstFailure = listOf(userResult, contactsResult, eventsResult, journeysResult, safetyZonesResult)
                    .firstOrNull { it.isFailure }
                if (firstFailure != null) {
                    throw firstFailure.exceptionOrNull() ?: RuntimeException("Sync operation failed")
                }

                val duration = System.currentTimeMillis() - startTime
                val summary = SyncSummary(
                    userSuccess = userSuccess,
                    contactsSynced = contactsCount,
                    eventsSynced = eventsCount,
                    journeysSynced = journeysCount,
                    safetyZonesSynced = safetyZonesCount,
                    durationMs = duration,
                    isFullySynced = isOnline
                )

                val statusMsg = if (isOnline) {
                    "Synced: $contactsCount contacts, $eventsCount events, $journeysCount journeys, $safetyZonesCount zones"
                } else {
                    "Offline: All local records safely retained"
                }

                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncTimestamp = System.currentTimeMillis(),
                        lastSummary = summary,
                        statusMessage = statusMsg,
                        error = null
                    )
                }

                Result.success(summary)
            } catch (e: Exception) {
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = "Offline storage preserved (${e.localizedMessage ?: "Unknown reason"})",
                        error = e.localizedMessage
                    )
                }
                // Offline-first graceful fallback: preserve all local storage
                Result.success(
                    SyncSummary(
                        userSuccess = true,
                        durationMs = System.currentTimeMillis() - startTime,
                        isFullySynced = false
                    )
                )
            }
        }
    }

    private suspend fun <T> retryWithBackoff(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 300L,
        factor: Double = 2.0,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastResult: Result<T> = Result.failure(IllegalStateException("No attempts executed"))

        for (attempt in 1..maxAttempts) {
            try {
                lastResult = block()
                if (lastResult.isSuccess) {
                    return lastResult
                }
            } catch (e: Exception) {
                lastResult = Result.failure(e)
            }
            if (attempt < maxAttempts) {
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        return lastResult
    }

    override suspend fun syncUserData(): Result<User> {
        return userRepository.syncUser()
    }

    override suspend fun syncContacts(): Result<List<EmergencyContact>> {
        return emergencyContactRepository.syncContacts()
    }

    override suspend fun syncEmergencyEvents(): Result<List<EmergencyEvent>> {
        return emergencyEventRepository.syncEvents()
    }

    override suspend fun syncJourneys(): Result<List<JourneyEntity>> {
        return journeyRepository.syncJourneys()
    }

    override suspend fun syncSafetyZones(): Result<List<SafetyZone>> {
        return safetyZoneRepository.syncSafetyZones()
    }
}
