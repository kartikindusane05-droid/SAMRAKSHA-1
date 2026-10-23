package com.samraksha.app.data.repository

import com.samraksha.app.BuildConfig
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.remote.GeoapifyApiService
import com.samraksha.app.domain.model.AutocompleteResult
import com.samraksha.app.domain.model.CorridorPoi
import com.samraksha.app.domain.model.PlaceSuggestion
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteOption
import com.samraksha.app.domain.repository.RouteOptionsResult
import com.samraksha.app.domain.repository.RoutingRepository
import com.samraksha.app.domain.repository.RoutingResult
import com.samraksha.app.domain.repository.SafetyZoneRepository
import com.samraksha.app.domain.safety.NightActivityScoreCalculator
import com.samraksha.app.domain.safety.OpeningHoursParser
import com.samraksha.app.domain.safety.VenueOpenStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import org.maplibre.android.geometry.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Concrete implementation of [RoutingRepository] using Geoapify REST APIs.
 * Communicates with https://api.geoapify.com/ for turn-by-turn routing, places, and forward geocoding.
 * Safely handles missing API keys, network drops, and geocoding fallbacks without crashing.
 */
class GeoapifyRoutingRepositoryImpl(
    private val apiService: GeoapifyApiService = createDefaultApiService(),
    private val safetyZoneRepository: SafetyZoneRepository? = null
) : RoutingRepository {

    override suspend fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destination: String,
        destLat: Double?,
        destLng: Double?,
        mode: String
    ): RoutingResult {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) {
            return RoutingResult.Error(
                "Geoapify API key is not configured. Please add GEOAPIFY_API_KEY to local.properties."
            )
        }

        if (startLat == 0.0 && startLng == 0.0) {
            return RoutingResult.Error("Origin GPS coordinates are unavailable. Please enable location.")
        }

        return try {
            val targetCoords = resolveDestinationCoordinates(destination, destLat, destLng)
                ?: return RoutingResult.Error("Could not resolve location for '$destination'. Please check the destination name.")

            val targetLat = targetCoords.latitude
            val targetLng = targetCoords.longitude

            // Format waypoints: "lat1,lon1|lat2,lon2"
            val waypoints = "$startLat,$startLng|$targetLat,$targetLng"
            val response = apiService.getRoute(
                waypoints = waypoints,
                mode = mode,
                type = "balanced",
                details = "route_details,instruction_details",
                apiKey = apiKey
            )

            if (response.isSuccessful) {
                val body = response.body()
                val feature = body?.features?.firstOrNull()

                if (feature != null) {
                    val points = feature.geometry?.toLatLngList() ?: emptyList()
                    val distanceMeters = feature.properties?.distance ?: 0.0
                    val durationSeconds = feature.properties?.time ?: 0.0
                    val steps = feature.properties?.toRouteStepInfoList() ?: emptyList()

                    val finalPoints = if (points.isNotEmpty()) {
                        points
                    } else {
                        // Fallback straight-line connecting start and end
                        listOf(LatLng(startLat, startLng), LatLng(targetLat, targetLng))
                    }

                    RoutingResult.Success(
                        RouteInfo(
                            points = finalPoints,
                            distanceMeters = distanceMeters,
                            durationSeconds = durationSeconds,
                            startPoint = LatLng(startLat, startLng),
                            destinationPoint = LatLng(targetLat, targetLng),
                            destinationName = destination,
                            steps = steps
                        )
                    )
                } else {
                    RoutingResult.Error("No route found to destination.")
                }
            } else {
                val code = response.code()
                val errorMsg = when (code) {
                    401 -> "Invalid Geoapify API key. Please check key in local.properties."
                    402 -> "Geoapify API request quota exceeded."
                    400 -> "Invalid route parameters or coordinates."
                    else -> "Routing server error (HTTP $code)."
                }
                RoutingResult.Error(errorMsg)
            }
        } catch (_: UnknownHostException) {
            RoutingResult.Error("Network unreachable. Running in offline mode.")
        } catch (_: ConnectException) {
            RoutingResult.Error("Could not connect to routing service. Running in offline mode.")
        } catch (_: SocketTimeoutException) {
            RoutingResult.Error("Routing request timed out. Please check your connection.")
        } catch (e: IOException) {
            RoutingResult.Error("Network error while calculating route: ${e.localizedMessage}")
        } catch (e: Exception) {
            RoutingResult.Error("Unexpected routing error: ${e.localizedMessage}")
        }
    }

    override suspend fun calculateRouteOptions(
        startLat: Double,
        startLng: Double,
        destination: String,
        destLat: Double?,
        destLng: Double?,
        mode: String
    ): RouteOptionsResult {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) {
            return RouteOptionsResult.Error(
                "Geoapify API key is not configured. Please add GEOAPIFY_API_KEY to local.properties."
            )
        }

        if (startLat == 0.0 && startLng == 0.0) {
            return RouteOptionsResult.Error("Origin GPS coordinates are unavailable. Please enable location.")
        }

        return try {
            val targetCoords = resolveDestinationCoordinates(destination, destLat, destLng)
                ?: return RouteOptionsResult.Error("Could not resolve location for '$destination'. Please check the destination name.")

            val targetLat = targetCoords.latitude
            val targetLng = targetCoords.longitude
            val waypoints = "$startLat,$startLng|$targetLat,$targetLng"

            // Route strategies to fetch
            val strategySpecs = listOf(
                Triple("balanced", "Route A (Balanced)", "balanced"),
                Triple("short", "Route B (Shortest)", "short"),
                Triple("less_maneuvers", "Route C (Fewer Turns)", "less_maneuvers")
            )

            // Calculate midpoint and search radius for corridor places
            val midLat = (startLat + targetLat) / 2.0
            val midLng = (startLng + targetLng) / 2.0
            val directDistance = NightActivityScoreCalculator.haversineDistance(startLat, startLng, targetLat, targetLng)
            val searchRadius = (directDistance / 2.0 + 800.0).toInt().coerceIn(1000, 5000)

            // Concurrently query routing strategies and corridor places
            coroutineScope {
                val routeJobs = strategySpecs.map { (strategyType, label, strategyKey) ->
                    async {
                        try {
                            val resp = apiService.getRoute(
                                waypoints = waypoints,
                                mode = mode,
                                type = strategyType,
                                details = "route_details,instruction_details",
                                apiKey = apiKey
                            )
                            if (resp.isSuccessful) {
                                val feature = resp.body()?.features?.firstOrNull()
                                if (feature != null) {
                                    val pts = feature.geometry?.toLatLngList() ?: emptyList()
                                    val dist = feature.properties?.distance ?: 0.0
                                    val time = feature.properties?.time ?: 0.0
                                    val steps = feature.properties?.toRouteStepInfoList() ?: emptyList()
                                    val finalPts = if (pts.isNotEmpty()) pts else listOf(LatLng(startLat, startLng), LatLng(targetLat, targetLng))

                                    val info = RouteInfo(
                                        points = finalPts,
                                        distanceMeters = dist,
                                        durationSeconds = time,
                                        startPoint = LatLng(startLat, startLng),
                                        destinationPoint = LatLng(targetLat, targetLng),
                                        destinationName = destination,
                                        steps = steps
                                    )
                                    Pair(Triple(strategyType, label, strategyKey), info)
                                } else null
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                }

                val placesJob = async {
                    fetchCorridorPois(midLat, midLng, searchRadius, apiKey)
                }

                val routeResults = routeJobs.mapNotNull { it.await() }
                val corridorPois = placesJob.await()

                if (routeResults.isEmpty()) {
                    return@coroutineScope RouteOptionsResult.Error("No routes found to '$destination'.")
                }

                // Retrieve verified safe zones
                val verifiedZones = getVerifiedSafeZones()

                // Deduplicate routes: remove duplicate geometries that share identical distance
                val uniqueRoutes = mutableListOf<Pair<Triple<String, String, String>, RouteInfo>>()
                for (candidate in routeResults) {
                    val candidateInfo = candidate.second
                    val isDuplicate = uniqueRoutes.any { existing ->
                        val existingInfo = existing.second
                        abs(candidateInfo.distanceMeters - existingInfo.distanceMeters) < 40.0 &&
                                abs(candidateInfo.points.size - existingInfo.points.size) < 4
                    }
                    if (!isDuplicate) {
                        uniqueRoutes.add(candidate)
                    }
                }

                val distinctCandidates = if (uniqueRoutes.isNotEmpty()) uniqueRoutes else listOf(routeResults.first())

                // Compute Night Activity Score for each distinct route
                val routeOptions = distinctCandidates.mapIndexed { index, (strategySpec, info) ->
                    val (_, label, strategyKey) = strategySpec
                    val score = NightActivityScoreCalculator.calculate(
                        route = info,
                        corridorPois = corridorPois,
                        verifiedSafeZones = verifiedZones
                    )
                    RouteOption(
                        id = UUID.randomUUID().toString(),
                        label = label,
                        strategy = strategyKey,
                        routeInfo = info,
                        nightActivityScore = score,
                        isSelected = (index == 0)
                    )
                }

                val isAlternativeAvailable = routeOptions.size > 1
                val limitationNotice = if (!isAlternativeAvailable) {
                    "Single direct route available for this transit corridor. Geoapify found no distinct alternative paths."
                } else null

                RouteOptionsResult.Success(
                    options = routeOptions,
                    isAlternativeAvailable = isAlternativeAvailable,
                    limitationNotice = limitationNotice
                )
            }
        } catch (_: UnknownHostException) {
            RouteOptionsResult.Error("Network unreachable. Running in offline mode.")
        } catch (_: ConnectException) {
            RouteOptionsResult.Error("Could not connect to routing service. Running in offline mode.")
        } catch (_: SocketTimeoutException) {
            RouteOptionsResult.Error("Routing request timed out. Please check your connection.")
        } catch (e: IOException) {
            RouteOptionsResult.Error("Network error while calculating routes: ${e.localizedMessage}")
        } catch (e: Exception) {
            RouteOptionsResult.Error("Unexpected error while calculating route options: ${e.localizedMessage}")
        }
    }

    private suspend fun fetchCorridorPois(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Int,
        apiKey: String
    ): List<CorridorPoi> {
        val categories = "commercial,catering,service,healthcare.hospital,service.police,public_transport"
        val filter = "circle:$centerLng,$centerLat,$radiusMeters"

        return try {
            val response = apiService.getPlaces(
                categories = categories,
                filter = filter,
                limit = 40,
                apiKey = apiKey
            )
            if (response.isSuccessful) {
                val features = response.body()?.features ?: emptyList()
                features.mapNotNull { feat ->
                    val props = feat.properties ?: return@mapNotNull null
                    val lat = props.lat ?: return@mapNotNull null
                    val lon = props.lon ?: return@mapNotNull null
                    val primaryCat = props.categories?.firstOrNull() ?: "commercial"
                    val openStatus = OpeningHoursParser.isOpen(props.openingHours)
                    val name = props.name ?: props.formatted ?: "Local Venue"

                    CorridorPoi(
                        id = props.placeId ?: UUID.randomUUID().toString(),
                        name = name,
                        category = primaryCat,
                        location = LatLng(lat, lon),
                        openingHoursRaw = props.openingHours,
                        openStatus = openStatus,
                        distanceToRouteMeters = 0.0 // Measured dynamically by calculator
                    )
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getVerifiedSafeZones(): List<com.samraksha.app.ui.screens.location.model.SafetyZone> {
        return try {
            safetyZoneRepository?.getCachedSafetyZones()
                ?: SamrakshaApp.instance.safetyZoneRepository.getCachedSafetyZones()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun resolveDestinationCoordinates(
        destination: String,
        destLat: Double?,
        destLng: Double?
    ): LatLng? {
        if (destLat != null && destLng != null && (destLat != 0.0 || destLng != 0.0)) {
            return LatLng(destLat, destLng)
        }
        val parsed = parseCoordinatesString(destination)
        if (parsed != null) return parsed

        return geocodeAddress(destination)
    }

    override suspend fun geocodeAddress(address: String): LatLng? {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) return null
        val query = address.trim()
        if (query.isBlank()) return null

        return try {
            val response = apiService.geocode(
                text = query,
                apiKey = apiKey,
                limit = 1
            )
            if (response.isSuccessful) {
                val feature = response.body()?.features?.firstOrNull()
                val props = feature?.properties
                if (props?.lat != null && props.lon != null) {
                    LatLng(props.lat, props.lon)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun searchPlaces(
        query: String,
        userLat: Double?,
        userLng: Double?,
        cityFilter: String?
    ): AutocompleteResult {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) {
            return AutocompleteResult.Error("Geoapify API key not configured")
        }

        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return AutocompleteResult.Empty
        }

        // If a specific city filter is active (and not "All Cities"), scope search text to that city
        val effectiveText = if (!cityFilter.isNullOrBlank() && !cityFilter.equals("All Cities", ignoreCase = true) && !trimmed.contains(cityFilter, ignoreCase = true)) {
            "$trimmed, $cityFilter"
        } else {
            trimmed
        }

        val proximityBias = if (userLat != null && userLng != null && (userLat != 0.0 || userLng != 0.0)) {
            "proximity:$userLng,$userLat"
        } else null

        return try {
            val response = apiService.autocomplete(
                text = effectiveText,
                apiKey = apiKey,
                limit = 7,
                bias = proximityBias
            )

            if (response.isSuccessful) {
                val features = response.body()?.features ?: emptyList()
                val suggestions = features.mapNotNull { feat ->
                    val props = feat.properties ?: return@mapNotNull null
                    val lat = props.lat ?: return@mapNotNull null
                    val lon = props.lon ?: return@mapNotNull null

                    val name = props.name?.takeIf { it.isNotBlank() }
                        ?: props.addressLine1?.takeIf { it.isNotBlank() }
                        ?: props.formatted?.substringBefore(",")?.trim()
                        ?: "Selected Location"

                    val fullAddress = props.formatted?.takeIf { it.isNotBlank() }
                        ?: listOfNotNull(props.addressLine1, props.addressLine2, props.city, props.country).joinToString(", ")

                    val locality = props.addressLine2?.takeIf { it.isNotBlank() }
                        ?: props.city?.takeIf { it.isNotBlank() }
                        ?: props.state

                    val distanceMeters = if (userLat != null && userLng != null && (userLat != 0.0 || userLng != 0.0)) {
                        NightActivityScoreCalculator.haversineDistance(userLat, userLng, lat, lon)
                    } else null

                    PlaceSuggestion(
                        id = props.placeId ?: UUID.randomUUID().toString(),
                        name = name,
                        fullAddress = fullAddress,
                        locality = locality,
                        coordinates = LatLng(lat, lon),
                        distanceMeters = distanceMeters
                    )
                }

                if (suggestions.isEmpty()) {
                    AutocompleteResult.Empty
                } else {
                    AutocompleteResult.Success(suggestions)
                }
            } else {
                val code = response.code()
                when (code) {
                    429 -> AutocompleteResult.Error("API rate limit exceeded. Please wait a moment.", isRateLimited = true)
                    402 -> AutocompleteResult.Error("Geoapify request quota exceeded.", isRateLimited = true)
                    else -> AutocompleteResult.Error("Geocoding service returned HTTP $code")
                }
            }
        } catch (_: UnknownHostException) {
            AutocompleteResult.Error("Network unreachable. Please check your connection.", isNetworkError = true)
        } catch (_: ConnectException) {
            AutocompleteResult.Error("Could not connect to search service.", isNetworkError = true)
        } catch (_: SocketTimeoutException) {
            AutocompleteResult.Error("Search request timed out. Please try again.", isNetworkError = true)
        } catch (e: IOException) {
            AutocompleteResult.Error("Network error while searching: ${e.localizedMessage}", isNetworkError = true)
        } catch (e: Exception) {
            AutocompleteResult.Error("Unexpected search error: ${e.localizedMessage}")
        }
    }

    internal fun parseCoordinatesString(text: String): LatLng? {
        return try {
            val parts = text.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lon = parts[1].trim().toDoubleOrNull()
                if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                    LatLng(lat, lon)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val BASE_URL = "https://api.geoapify.com/"

        fun createDefaultApiService(): GeoapifyApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeoapifyApiService::class.java)
        }
    }
}
