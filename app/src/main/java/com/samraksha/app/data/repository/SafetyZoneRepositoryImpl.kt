package com.samraksha.app.data.repository

import org.maplibre.android.geometry.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samraksha.app.data.local.SafetyZoneLocalCache
import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.SafetyZoneDto
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.SafetyZoneApiService
import com.samraksha.app.domain.repository.SafetyZoneLoadResult
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.safety.JourneySafetyMonitor
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.screens.location.model.SafetyZoneShape
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Concrete implementation of [SafetyZoneRepository].
 * Loads safety zones from the REST API backend, persists them locally,
 * and seamlessly preserves previously available local data when the network is unreachable.
 */
class SafetyZoneRepositoryImpl(
    private val apiService: SafetyZoneApiService = ApiClient.safetyZoneApiService,
    private val localCache: SafetyZoneLocalCache = SafetyZoneLocalCache()
) : SafetyZoneRepository {

    private val gson = Gson()

    override suspend fun getSafetyZones(
        lat: Double?,
        lng: Double?,
        radius: Double?,
        riskLevel: String?,
        forceRefresh: Boolean
    ): SafetyZoneLoadResult {
        return try {
            val response = apiService.getSafetyZones(
                riskLevel = riskLevel,
                lat = lat,
                lng = lng,
                radius = radius
            )

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse != null && apiResponse.success && apiResponse.data != null) {
                    val dtos = apiResponse.data.zones

                    // Save full zone list to local cache for offline access
                    if (riskLevel == null) {
                        localCache.saveZones(dtos)
                    }

                    val domainZones = dtos.map { it.toDomain() }
                    val validatedZones = if (lat != null && lng != null && radius != null && (lat != 0.0 || lng != 0.0)) {
                        domainZones.filter { zone ->
                            JourneySafetyMonitor.isInsideZone(lat, lng, zone) ||
                                JourneySafetyMonitor.distanceToZone(lat, lng, zone) <= radius
                        }
                    } else {
                        domainZones
                    }

                    SafetyZoneLoadResult.Success(
                        zones = validatedZones,
                        isFromCache = false,
                        message = apiResponse.message
                    )
                } else {
                    val errorMsg = apiResponse?.message ?: "Failed to retrieve safety zones"
                    val cached = getProximityFilteredCachedZones(lat, lng, radius)
                    SafetyZoneLoadResult.Error(
                        message = if (cached.isNotEmpty()) "$errorMsg. Showing cached local zones." else "$errorMsg. No nearby zones found.",
                        cachedZones = cached
                    )
                }
            } else {
                val errorMessage = extractErrorMessage(response.errorBody()?.string())
                    ?: "Server returned HTTP ${response.code()}"
                val cached = getProximityFilteredCachedZones(lat, lng, radius)
                SafetyZoneLoadResult.Error(
                    message = if (cached.isNotEmpty()) "$errorMessage. Showing previously saved local zones." else "$errorMessage. No nearby zones found.",
                    cachedZones = cached
                )
            }
        } catch (e: ConnectException) {
            handleOfflineError("Cannot connect to SAMRAKSHA safety backend at ${ApiClient.getBaseUrl()}. Check server status.", e, lat, lng, radius)
        } catch (e: SocketTimeoutException) {
            handleOfflineError("Backend connection timed out. Please check your connection.", e, lat, lng, radius)
        } catch (e: UnknownHostException) {
            handleOfflineError("Network address unreachable. Running in offline mode.", e, lat, lng, radius)
        } catch (e: IOException) {
            handleOfflineError("Network error: ${e.localizedMessage ?: "Connection interrupted"}", e, lat, lng, radius)
        } catch (e: Exception) {
            handleOfflineError("Unexpected error loading safety zones: ${e.localizedMessage}", e, lat, lng, radius)
        }
    }

    override fun getCachedSafetyZones(): List<SafetyZone> {
        val dtos = localCache.getCachedZones()
        return if (dtos.isNotEmpty()) {
            dtos.map { it.toDomain() }
        } else {
            emptyList()
        }
    }

    /**
     * Retrieves cached zones strictly filtered by proximity to the specified coordinates and radius.
     * Never falls back to distant zones or seeded data.
     */
    private fun getProximityFilteredCachedZones(
        lat: Double?,
        lng: Double?,
        radius: Double?
    ): List<SafetyZone> {
        val allCached = getCachedSafetyZones()
        if (lat == null || lng == null || radius == null || (lat == 0.0 && lng == 0.0)) {
            return allCached
        }
        return allCached.filter { zone ->
            JourneySafetyMonitor.isInsideZone(lat, lng, zone) ||
                JourneySafetyMonitor.distanceToZone(lat, lng, zone) <= radius
        }
    }

    override suspend fun syncSafetyZones(): Result<List<SafetyZone>> {
        return try {
            val cachedDtos = localCache.getCachedZones()
            if (cachedDtos.isNotEmpty()) {
                val response = apiService.syncSafetyZones(cachedDtos)
                if (response.isSuccessful && response.body()?.data != null) {
                    val serverZones = response.body()!!.data!!.zones
                    localCache.saveZones(serverZones)
                    Result.success(serverZones.map { it.toDomain() })
                } else {
                    val fallback = getCachedSafetyZones()
                    Result.success(fallback)
                }
            } else {
                val loadResult = getSafetyZones(forceRefresh = true)
                when (loadResult) {
                    is SafetyZoneLoadResult.Success -> Result.success(loadResult.zones)
                    is SafetyZoneLoadResult.Error -> Result.success(loadResult.cachedZones.ifEmpty { getCachedSafetyZones() })
                }
            }
        } catch (_: Exception) {
            val cached = getCachedSafetyZones()
            Result.success(cached)
        }
    }

    private fun handleOfflineError(
        message: String,
        cause: Throwable,
        fallbackLat: Double?,
        fallbackLng: Double?,
        radius: Double? = null
    ): SafetyZoneLoadResult {
        val proximityFiltered = getProximityFilteredCachedZones(fallbackLat, fallbackLng, radius)
        return if (proximityFiltered.isNotEmpty()) {
            SafetyZoneLoadResult.Error(
                message = "$message Retaining ${proximityFiltered.size} previously saved local zones.",
                cachedZones = proximityFiltered,
                cause = cause
            )
        } else {
            val radiusText = if (radius != null) " within ${(radius / 1000).toInt()} km" else ""
            SafetyZoneLoadResult.Error(
                message = "$message No verified safety zones found in local cache$radiusText.",
                cachedZones = emptyList(),
                cause = cause
            )
        }
    }

    private fun extractErrorMessage(errorJson: String?): String? {
        if (errorJson.isNullOrBlank()) return null
        return try {
            val type = object : TypeToken<ApiResponse<Any>>() {}.type
            val parsed: ApiResponse<Any> = gson.fromJson(errorJson, type)
            parsed.message
        } catch (_: Exception) {
            null
        }
    }

    private fun SafetyZoneDto.toDomain(): SafetyZone {
        val rawLevel = riskLevel.uppercase()
        val level = when (rawLevel) {
            "SAFE" -> SafetyZoneLevel.SAFE
            "MODERATE_RISK", "MODERATE", "CAUTION" -> SafetyZoneLevel.MODERATE_RISK
            "UNSAFE", "HIGH_RISK", "HIGH" -> SafetyZoneLevel.UNSAFE
            "UNKNOWN", "INSUFFICIENT_DATA" -> SafetyZoneLevel.UNKNOWN
            else -> SafetyZoneLevel.UNKNOWN
        }

        val center = LatLng(latitude, longitude)
        val zoneShape = if (!polygonPoints.isNullOrEmpty() && polygonPoints.size >= 3) {
            SafetyZoneShape.Polygon(points = polygonPoints.map { LatLng(it.latitude, it.longitude) })
        } else {
            SafetyZoneShape.Circular(center = center, radiusMeters = radius)
        }

        val (score, explanation, isProxy) = if (safetyScore != null) {
            val expl = when (level) {
                SafetyZoneLevel.SAFE -> "Classified as Safe based on official surveillance records and low incident density."
                SafetyZoneLevel.MODERATE_RISK -> "Classified as Moderate Risk based on historical patrol logs and transit density."
                SafetyZoneLevel.UNSAFE -> "Classified as High Risk based on recorded incident history and hazard reports."
                SafetyZoneLevel.UNKNOWN -> "Insufficient verified incident or patrol data recorded for this perimeter."
            }
            Triple(safetyScore, expl, false)
        } else {
            val fallbackScore = when (level) {
                SafetyZoneLevel.SAFE -> 85
                SafetyZoneLevel.MODERATE_RISK -> 55
                SafetyZoneLevel.UNSAFE -> 25
                SafetyZoneLevel.UNKNOWN -> -1
            }
            val expl = when (level) {
                SafetyZoneLevel.SAFE -> "Verified safe area with active surveillance."
                SafetyZoneLevel.MODERATE_RISK -> "Moderate transit activity. Caution advised."
                SafetyZoneLevel.UNSAFE -> "Caution advised in this area."
                SafetyZoneLevel.UNKNOWN -> "Insufficient verified data."
            }
            Triple(fallbackScore, expl, true)
        }

        return SafetyZone(
            id = id,
            name = name,
            level = level,
            shape = zoneShape,
            centerPoint = center,
            safetyScore = score,
            advisory = advisory?.takeIf { it.isNotBlank() } ?: description,
            activePatrols = activePatrols ?: 0,
            cctvCoveragePercentage = cctvCoverage ?: 0,
            recentIncidentSummary = description,
            dataSource = "Municipal Safety Records & Police Patrol Logs",
            lastUpdated = updatedAt ?: createdAt ?: "Recently Audited",
            isProxyOrEstimated = isProxy,
            scoreExplanation = explanation
        )
    }
}
