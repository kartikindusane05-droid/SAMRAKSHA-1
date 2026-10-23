package com.samraksha.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samraksha.app.data.local.SessionManager
import com.samraksha.app.data.local.dao.JourneyDao
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.data.model.CreateJourneyRequest
import com.samraksha.app.data.model.EndJourneyRequest
import com.samraksha.app.data.model.JourneyDto
import com.samraksha.app.data.model.UpdateJourneyLocationRequest
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.JourneyApiService
import com.samraksha.app.domain.repository.JourneyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Robust, offline-first implementation of [JourneyRepository].
 *
 * Persists journey history and active journey tracking to Room SQLite database
 * and local SharedPreferences, ensuring journey state safely recovers after
 * connectivity loss or application restart.
 *
 * Synchronization deduplicates journeys by unique ID, preserves accurate local
 * start/completion timestamps, and prevents stale server states from overwriting
 * unsynced offline edits.
 */
class JourneyRepositoryImpl(
    context: Context,
    private val dao: JourneyDao? = null,
    private val apiService: JourneyApiService = ApiClient.journeyApiService,
    private val sessionManager: SessionManager = SessionManager(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : JourneyRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _journeysFlow = MutableStateFlow<List<JourneyEntity>>(emptyList())

    init {
        // 1. Instant load from SharedPreferences for zero-latency UI display
        loadPersistedJourneys()

        // 2. Hydrate from and observe Room database if available
        if (dao != null) {
            scope.launch(ioDispatcher) {
                try {
                    val roomList = dao.getAllJourneysList()
                    if (roomList.isNotEmpty()) {
                        val currentPrefList = _journeysFlow.value
                        val merged = mergeLocalLists(currentPrefList, roomList)
                        persistJourneys(merged)
                    }
                } catch (_: Exception) {
                    // Gracefully continue with SharedPreferences cache
                }

                // Continuously observe Room database for reactive updates
                try {
                    dao.getAllJourneys().collect { roomList ->
                        if (roomList.isNotEmpty()) {
                            val current = _journeysFlow.value.associateBy { it.id }
                            val merged = roomList.map { roomItem ->
                                val inMem = current[roomItem.id]
                                if (inMem != null && !inMem.isSynced && roomItem.isSynced) inMem else roomItem
                            }.sortedByDescending { it.startTime }
                            _journeysFlow.value = merged
                            val json = gson.toJson(merged)
                            prefs.edit().putString(KEY_JOURNEYS, json).apply()
                        }
                    }
                } catch (_: Exception) {
                    // Room observation fallback
                }
            }
        }
    }

    private fun loadPersistedJourneys() {
        val json = prefs.getString(KEY_JOURNEYS, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<JourneyEntity>>() {}.type
                val loaded: List<JourneyEntity> = gson.fromJson(json, type) ?: emptyList()
                _journeysFlow.value = loaded
            } catch (_: Exception) {
                _journeysFlow.value = emptyList()
            }
        }
    }

    private fun persistJourneys(list: List<JourneyEntity>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_JOURNEYS, json).apply()
        _journeysFlow.value = list
    }

    private fun mergeLocalLists(
        prefsList: List<JourneyEntity>,
        roomList: List<JourneyEntity>
    ): List<JourneyEntity> {
        val map = prefsList.associateBy { it.id }.toMutableMap()
        for (item in roomList) {
            val existing = map[item.id]
            if (existing == null) {
                map[item.id] = item
            } else if (!existing.isSynced) {
                // Keep local unsynced edits
                map[item.id] = existing
            } else {
                map[item.id] = item
            }
        }
        return map.values.sortedByDescending { it.startTime }
    }

    override fun getAllJourneys(): Flow<List<JourneyEntity>> = _journeysFlow.asStateFlow()

    override fun getActiveJourney(): Flow<JourneyEntity?> =
        _journeysFlow.map { list ->
            list.firstOrNull { it.status == "IN_PROGRESS" }
        }

    override suspend fun startJourney(journey: JourneyEntity) {
        withContext(ioDispatcher) {
            val currentList = _journeysFlow.value.toMutableList()
            // Complete any previously hanging active journeys locally
            val updatedList = currentList.map {
                if (it.status == "IN_PROGRESS") {
                    it.copy(status = "COMPLETED", completedTime = System.currentTimeMillis(), isSynced = false)
                } else {
                    it
                }
            }.toMutableList()

            val journeyWithSync = journey.copy(isSynced = false)
            updatedList.add(0, journeyWithSync)
            persistJourneys(updatedList)

            try {
                dao?.insertJourney(journeyWithSync)
            } catch (_: Exception) {
                // Room fallback grace
            }

            // Attempt backend start immediately
            try {
                val request = CreateJourneyRequest(
                    id = journey.id,
                    startLocationName = journey.startLocationName,
                    endLocationName = journey.endLocationName,
                    startLatitude = journey.startLatitude,
                    startLongitude = journey.startLongitude,
                    destinationLatitude = journey.destinationLatitude,
                    destinationLongitude = journey.destinationLongitude,
                    expectedEndTime = if (journey.expectedEndTime > 0) journey.expectedEndTime.toString() else null
                )
                val response = apiService.startJourney(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    markSyncedInternal(journey.id)
                }
            } catch (_: Exception) {
                // Offline: preserved locally with isSynced = false
            }
        }
    }

    override suspend fun updateJourneyStatus(journeyId: String, status: String) {
        withContext(ioDispatcher) {
            val currentList = _journeysFlow.value
            val completedTime = if (status != "IN_PROGRESS") System.currentTimeMillis() else null
            val updated = currentList.map {
                if (it.id == journeyId) {
                    it.copy(
                        status = status,
                        completedTime = completedTime ?: it.completedTime,
                        isSynced = false
                    )
                } else {
                    it
                }
            }
            persistJourneys(updated)

            try {
                dao?.updateJourneyStatus(journeyId, status, completedTime)
            } catch (_: Exception) {
                // Room fallback grace
            }

            // Attempt backend end/update
            try {
                val response = apiService.endJourney(journeyId, EndJourneyRequest(status = status))
                if (response.isSuccessful) {
                    markSyncedInternal(journeyId)
                }
            } catch (_: Exception) {
                // Offline: preserved locally with isSynced = false
            }
        }
    }

    override suspend fun endJourney(journeyId: String, status: String) {
        updateJourneyStatus(journeyId, status)
    }

    override suspend fun updateCurrentLocation(
        journeyId: String,
        latitude: Double,
        longitude: Double
    ) {
        withContext(ioDispatcher) {
            val currentList = _journeysFlow.value
            val updated = currentList.map {
                if (it.id == journeyId) {
                    it.copy(currentLatitude = latitude, currentLongitude = longitude)
                } else {
                    it
                }
            }
            persistJourneys(updated)

            try {
                dao?.updateCurrentLocation(journeyId, latitude, longitude)
            } catch (_: Exception) {
                // Room fallback grace
            }

            // Attempt backend location update
            try {
                apiService.updateLocation(journeyId, UpdateJourneyLocationRequest(latitude, longitude))
            } catch (_: Exception) {
                // Ignore transient network hiccups during active telemetry
            }
        }
    }

    override suspend fun deleteJourney(journeyId: String) {
        withContext(ioDispatcher) {
            val updated = _journeysFlow.value.filterNot { it.id == journeyId }
            persistJourneys(updated)

            try {
                dao?.deleteJourney(journeyId)
            } catch (_: Exception) {
                // Room fallback grace
            }

            try {
                apiService.deleteJourney(journeyId)
            } catch (_: Exception) {
                // Offline: deleted locally
            }
        }
    }

    override suspend fun syncJourneys(): Result<List<JourneyEntity>> = withContext(ioDispatcher) {
        val userId = sessionManager.getUser()?.id ?: "user_demo_001"
        try {
            // Reconcile current local state from both in-memory and Room
            val roomList = try { dao?.getAllJourneysList() ?: emptyList() } catch (_: Exception) { emptyList() }
            val localCombined = (_journeysFlow.value + roomList).distinctBy { it.id }

            // 1. Push local journeys to backend
            val dtos = localCombined.map { j ->
                JourneyDto(
                    id = j.id,
                    userId = userId,
                    startLocationName = j.startLocationName,
                    endLocationName = j.endLocationName,
                    startTime = j.startTime.toString(),
                    expectedEndTime = if (j.expectedEndTime > 0L) j.expectedEndTime.toString() else null,
                    completedTime = j.completedTime?.toString(),
                    status = j.status,
                    startLatitude = j.startLatitude,
                    startLongitude = j.startLongitude,
                    currentLatitude = j.currentLatitude,
                    currentLongitude = j.currentLongitude,
                    destinationLatitude = j.destinationLatitude,
                    destinationLongitude = j.destinationLongitude
                )
            }

            val response = apiService.syncJourneys(userId = userId, journeys = dtos)
            if (response.isSuccessful && response.body()?.data != null) {
                val serverJourneys = response.body()!!.data!!
                val localMap = localCombined.associateBy { it.id }.toMutableMap()

                for (dto in serverJourneys) {
                    val local = localMap[dto.id]
                    val parsedStartTime = parseTimestamp(dto.startTime)
                    val parsedCompletedTime = parseTimestamp(dto.completedTime)
                    val parsedExpectedEndTime = parseTimestamp(dto.expectedEndTime) ?: 0L

                    if (local == null) {
                        // New journey received from backend
                        localMap[dto.id] = JourneyEntity(
                            id = dto.id,
                            startLocationName = dto.startLocationName,
                            endLocationName = dto.endLocationName,
                            startTime = parsedStartTime ?: System.currentTimeMillis(),
                            expectedEndTime = parsedExpectedEndTime,
                            status = dto.status,
                            completedTime = parsedCompletedTime,
                            startLatitude = dto.startLatitude,
                            startLongitude = dto.startLongitude,
                            currentLatitude = dto.currentLatitude,
                            currentLongitude = dto.currentLongitude,
                            destinationLatitude = dto.destinationLatitude,
                            destinationLongitude = dto.destinationLongitude,
                            isSynced = true
                        )
                    } else if (local.isSynced) {
                        // Local journey was already synced; accept authoritative updates
                        // while strictly preserving local startTime if server did not specify
                        localMap[dto.id] = local.copy(
                            status = dto.status,
                            startLocationName = dto.startLocationName.ifBlank { local.startLocationName },
                            endLocationName = dto.endLocationName.ifBlank { local.endLocationName },
                            startTime = parsedStartTime ?: local.startTime,
                            expectedEndTime = if (parsedExpectedEndTime > 0L) parsedExpectedEndTime else local.expectedEndTime,
                            completedTime = parsedCompletedTime ?: local.completedTime,
                            currentLatitude = if (dto.currentLatitude != 0.0) dto.currentLatitude else local.currentLatitude,
                            currentLongitude = if (dto.currentLongitude != 0.0) dto.currentLongitude else local.currentLongitude,
                            isSynced = true
                        )
                    } else {
                        // Local journey has unsynced offline updates (e.g. status completed/cancelled offline)
                        // Preserve local offline changes; do not overwrite with stale server status!
                    }
                }

                val mergedList = localMap.values.sortedByDescending { it.startTime }
                persistJourneys(mergedList)
                try {
                    dao?.insertJourneys(mergedList)
                    // Mark successfully synced records
                    mergedList.filter { it.isSynced }.forEach { dao?.markSynced(it.id) }
                } catch (_: Exception) {}

                Result.success(mergedList)
            } else {
                Result.success(_journeysFlow.value)
            }
        } catch (_: Exception) {
            // Offline fallback: preserve all existing local journeys
            Result.success(_journeysFlow.value)
        }
    }

    private fun parseTimestamp(timeStr: String?): Long? {
        if (timeStr.isNullOrBlank()) return null
        timeStr.toLongOrNull()?.let { return it }
        return try {
            java.time.Instant.parse(timeStr).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun markSyncedInternal(id: String) {
        val current = _journeysFlow.value.map {
            if (it.id == id) it.copy(isSynced = true) else it
        }
        persistJourneys(current)
        try {
            dao?.markSynced(id)
        } catch (_: Exception) {}
    }

    companion object {
        private const val PREFS_NAME = "samraksha_journey_prefs"
        private const val KEY_JOURNEYS = "persisted_journeys_list"
    }
}
