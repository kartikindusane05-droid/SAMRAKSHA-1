package com.samraksha.app.data.repository

import com.samraksha.app.data.local.SafetyZoneLocalCache
import com.samraksha.app.data.model.ApiResponse
import com.samraksha.app.data.model.SafetyZoneDto
import com.samraksha.app.data.model.SafetyZoneListData
import com.samraksha.app.data.remote.SafetyZoneApiService
import com.samraksha.app.domain.repository.SafetyZoneLoadResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SafetyZoneRepositoryImplTest {

    private lateinit var fakeApiService: FakeSafetyZoneApiService
    private lateinit var localCache: SafetyZoneLocalCache
    private lateinit var repository: SafetyZoneRepositoryImpl

    @Before
    fun setUp() {
        fakeApiService = FakeSafetyZoneApiService()
        localCache = SafetyZoneLocalCache(context = null)
        repository = SafetyZoneRepositoryImpl(
            apiService = fakeApiService,
            localCache = localCache
        )
    }

    @Test
    fun getSafetyZones_validatesRadius_filtersOutDistantZones() = runTest {
        val userLat = 13.0000
        val userLng = 77.6000

        // 1. Close zone: ~1.1 km away
        val nearZone = SafetyZoneDto(
            id = "near-1",
            name = "Near Zone",
            latitude = userLat + 0.01,
            longitude = userLng,
            radius = 100.0,
            riskLevel = "SAFE",
            description = "Safe nearby"
        )
        // 2. Far zone: ~11 km away
        val farZone = SafetyZoneDto(
            id = "far-1",
            name = "Far Zone",
            latitude = userLat + 0.1,
            longitude = userLng,
            radius = 100.0,
            riskLevel = "UNSAFE",
            description = "Distant zone"
        )

        fakeApiService.responseToReturn = Response.success(
            ApiResponse(
                success = true,
                message = "Zones retrieved",
                data = SafetyZoneListData(count = 2, zones = listOf(nearZone, farZone))
            )
        )

        val result = repository.getSafetyZones(lat = userLat, lng = userLng, radius = 5000.0)

        assertTrue(result is SafetyZoneLoadResult.Success)
        val success = result as SafetyZoneLoadResult.Success
        assertEquals("Only nearby zone within 5km should be returned", 1, success.zones.size)
        assertEquals("near-1", success.zones.first().id)
    }

    @Test
    fun handleOfflineError_whenCachedZonesAreDistant_returnsEmptyListNotDistantOrMockZones() = runTest {
        // User is at Mumbai coordinates (~19.0760, 72.8777)
        val userLat = 19.0760
        val userLng = 72.8777

        // Pre-populate cache with Bengaluru zones (~880 km away)
        val distantBengaluruZone = SafetyZoneDto(
            id = "bengaluru-cache-1",
            name = "MG Road Zone",
            latitude = 12.9716,
            longitude = 77.5946,
            radius = 200.0,
            riskLevel = "SAFE",
            description = "Bengaluru cached zone"
        )
        localCache.saveZones(listOf(distantBengaluruZone))

        // Simulate network failure
        fakeApiService.exceptionToThrow = IOException("No internet connection")

        val result = repository.getSafetyZones(lat = userLat, lng = userLng, radius = 5000.0)

        assertTrue(result is SafetyZoneLoadResult.Error)
        val error = result as SafetyZoneLoadResult.Error
        assertTrue("Distant cached zones 880km away must be filtered out", error.cachedZones.isEmpty())
    }

    @Test
    fun handleOfflineError_whenCachedZonesAreNearby_returnsNearbyCachedZones() = runTest {
        val userLat = 13.0000
        val userLng = 77.6000

        val nearZone = SafetyZoneDto(
            id = "near-cached-1",
            name = "Near Cached Zone",
            latitude = userLat + 0.005,
            longitude = userLng,
            radius = 100.0,
            riskLevel = "SAFE",
            description = "Nearby safe zone in cache"
        )
        val farZone = SafetyZoneDto(
            id = "far-cached-1",
            name = "Far Cached Zone",
            latitude = userLat + 0.2,
            longitude = userLng,
            radius = 100.0,
            riskLevel = "MODERATE_RISK",
            description = "Far zone in cache"
        )
        localCache.saveZones(listOf(nearZone, farZone))

        fakeApiService.exceptionToThrow = IOException("Network down")

        val result = repository.getSafetyZones(lat = userLat, lng = userLng, radius = 5000.0)

        assertTrue(result is SafetyZoneLoadResult.Error)
        val error = result as SafetyZoneLoadResult.Error
        assertEquals(1, error.cachedZones.size)
        assertEquals("near-cached-1", error.cachedZones.first().id)
    }

    private class FakeSafetyZoneApiService : SafetyZoneApiService {
        var responseToReturn: Response<ApiResponse<SafetyZoneListData>>? = null
        var exceptionToThrow: Exception? = null

        override suspend fun getSafetyZones(
            riskLevel: String?,
            lat: Double?,
            lng: Double?,
            radius: Double?
        ): Response<ApiResponse<SafetyZoneListData>> {
            exceptionToThrow?.let { throw it }
            return responseToReturn ?: Response.error(
                500,
                "{}".toResponseBody("application/json".toMediaType())
            )
        }

        override suspend fun getSafetyZoneById(id: String): Response<ApiResponse<SafetyZoneDto>> {
            throw UnsupportedOperationException()
        }

        override suspend fun syncSafetyZones(zones: List<SafetyZoneDto>): Response<ApiResponse<SafetyZoneListData>> {
            throw UnsupportedOperationException()
        }
    }
}
