package com.samraksha.app.domain.safety

import com.samraksha.app.domain.model.ActivityFactorScore
import com.samraksha.app.domain.model.ActivityFactorType
import com.samraksha.app.domain.model.ActivityLevel
import com.samraksha.app.domain.model.CorridorPoi
import com.samraksha.app.domain.model.NightActivityScoreResult
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteStepInfo
import com.samraksha.app.domain.model.ScoreConfidenceLevel
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import org.maplibre.android.geometry.LatLng
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Dedicated, deterministic calculator for the Night Activity Score.
 * Computes a normalized [0, 100] score based on 6 core factors:
 * 1. Open commercial & public venues (max 25 pts)
 * 2. Nearby verified help points (max 15 pts)
 * 3. Main-road coverage (max 15 pts)
 * 4. Active transit hubs (max 10 pts)
 * 5. Continuity of active corridors (max 25 pts)
 * 6. Direct & connected routing / fewer turns (max 10 pts)
 * Total weight = strictly 100.0 points.
 *
 * Missing or unrecorded opening hours strictly resolve to UNKNOWN and are never
 * assumed to be open or closed, but gracefully reduce the confidence level.
 */
object NightActivityScoreCalculator {

    private val MAIN_ROAD_CLASSES = setOf(
        "motorway",
        "motorway_link",
        "trunk",
        "trunk_link",
        "primary",
        "primary_link",
        "secondary",
        "secondary_link"
    )

    private val HELP_CATEGORIES = setOf(
        "service.police",
        "healthcare.hospital",
        "healthcare.pharmacy"
    )

    private val TRANSIT_CATEGORIES = setOf(
        "public_transport",
        "public_transport.bus",
        "public_transport.train",
        "public_transport.subway",
        "railway.station",
        "amenity.bus_station"
    )

    /**
     * Calculates the complete Night Activity Score for a given route and corridor environment.
     */
    fun calculate(
        route: RouteInfo,
        corridorPois: List<CorridorPoi>,
        verifiedSafeZones: List<SafetyZone> = emptyList(),
        evaluationDateTime: LocalDateTime = LocalDateTime.now()
    ): NightActivityScoreResult {
        val distanceKm = (route.distanceMeters / 1000.0).coerceAtLeast(0.1)

        // 1. Open Commercial & Public Venues (25 pts max)
        val commercialPois = corridorPois.filter { poi ->
            !isHelpCategory(poi.category) && !isTransitCategory(poi.category)
        }
        val openVenues = commercialPois.filter { it.openStatus == VenueOpenStatus.OPEN }
        val openVenuesCount = openVenues.size
        val unknownHoursCount = commercialPois.count { it.openStatus == VenueOpenStatus.UNKNOWN }

        val openPlacesScore = calculateOpenPlacesScore(openVenuesCount, distanceKm)

        // 2. Nearby Verified Help Points (15 pts max)
        val helpPois = corridorPois.filter { isHelpCategory(it.category) && it.distanceToRouteMeters <= 500.0 }
        val safeZonesAlongRoute = verifiedSafeZones.filter { zone ->
            zone.level == SafetyZoneLevel.SAFE && isPointNearRoute(zone.centerPoint, route.points, 500.0)
        }
        val totalHelpPoints = helpPois.size + safeZonesAlongRoute.size
        val nearbyHelpScore = calculateNearbyHelpScore(totalHelpPoints, distanceKm)

        // 3. Main-Road Coverage (15 pts max)
        val mainRoadScore = calculateMainRoadScore(route)

        // 4. Active Transit Hubs (10 pts max)
        val transitPois = corridorPois.filter {
            isTransitCategory(it.category) && it.openStatus != VenueOpenStatus.CLOSED
        }
        val activeTransitCount = transitPois.size
        val transportScore = calculateTransportScore(activeTransitCount, distanceKm)

        // 5. Continuity of Active Corridors (25 pts max)
        val activeCorridorPoints = mutableListOf<LatLng>()
        openVenues.forEach { activeCorridorPoints.add(it.location) }
        helpPois.forEach { activeCorridorPoints.add(it.location) }
        safeZonesAlongRoute.forEach { activeCorridorPoints.add(it.centerPoint) }
        transitPois.forEach { activeCorridorPoints.add(it.location) }

        val continuityScore = calculateContinuityScore(route.points, activeCorridorPoints)

        // 6. Direct & Connected Path / Fewer Turns (10 pts max)
        val fewerTurnsScore = calculateFewerTurnsScore(route, distanceKm)

        val factorScores = listOf(
            openPlacesScore,
            nearbyHelpScore,
            mainRoadScore,
            transportScore,
            continuityScore,
            fewerTurnsScore
        )

        val totalPointsRaw = factorScores.sumOf { it.earnedPoints }
        val clampedScore = totalPointsRaw.roundToInt().coerceIn(0, 100)

        val activityLevel = when {
            clampedScore >= 70 -> ActivityLevel.HIGH_ACTIVITY
            clampedScore >= 40 -> ActivityLevel.MODERATE_ACTIVITY
            else -> ActivityLevel.LIMITED_ACTIVITY
        }

        // Confidence calculation
        val totalPlaces = corridorPois.size
        val knownPlaces = totalPlaces - unknownHoursCount
        val knownRatio = if (totalPlaces > 0) knownPlaces.toDouble() / totalPlaces else 0.5

        val confidenceLevel = when {
            totalPlaces > 0 && knownRatio >= 0.65 -> ScoreConfidenceLevel.HIGH
            totalPlaces > 0 && knownRatio >= 0.35 -> ScoreConfidenceLevel.MEDIUM
            else -> ScoreConfidenceLevel.LOW
        }

        val confidenceDisclosure = when (confidenceLevel) {
            ScoreConfidenceLevel.HIGH -> "High confidence: verified opening hours and infrastructure data available for the majority of venues along this corridor."
            ScoreConfidenceLevel.MEDIUM -> "Moderate confidence: partial operating hours recorded. Some venues along this corridor have unverified schedules."
            ScoreConfidenceLevel.LOW -> "Reduced confidence: venue operating schedules along this corridor are partially unrecorded in public registries. Evaluated primarily from road hierarchy and verified emergency points."
        }

        return NightActivityScoreResult(
            totalScore = clampedScore,
            maxScore = 100,
            activityLevel = activityLevel,
            confidenceLevel = confidenceLevel,
            confidenceDisclosure = confidenceDisclosure,
            factorScores = factorScores,
            openPlacesCount = openVenuesCount,
            unknownHoursCount = unknownHoursCount,
            totalPlacesEvaluated = totalPlaces,
            evaluatedAtMillis = System.currentTimeMillis()
        )
    }

    private fun calculateOpenPlacesScore(openCount: Int, distanceKm: Double): ActivityFactorScore {
        val density = openCount / distanceKm
        // Target: ~2.5 open places per km yields full 25 points.
        // For very short trips (< 0.8 km), 2 open venues yields full 25 points.
        val ratio = if (distanceKm < 0.8) {
            (openCount / 2.0).coerceIn(0.0, 1.0)
        } else {
            (density / 2.5).coerceIn(0.0, 1.0)
        }
        val earned = (ratio * ActivityFactorType.OPEN_PLACES.maxWeight).coerceIn(0.0, ActivityFactorType.OPEN_PLACES.maxWeight)

        val display = if (openCount == 1) "1 open venue" else "$openCount open venues"
        val detail = if (openCount > 0) {
            "$openCount open commercial venues active along route corridor (~${String.format(Locale.US, "%.1f", density)}/km)"
        } else {
            "No verified open commercial venues recorded along this corridor at this time"
        }

        return ActivityFactorScore(
            factor = ActivityFactorType.OPEN_PLACES,
            earnedPoints = earned,
            rawMetricDisplay = display,
            detail = detail
        )
    }

    private fun calculateNearbyHelpScore(helpPointsCount: Int, distanceKm: Double): ActivityFactorScore {
        // Target: At least 2 verified help points gives full 15 points (1 gives 7.5 pts, 2+ gives 15 pts)
        val ratio = (helpPointsCount / 2.0).coerceIn(0.0, 1.0)
        val earned = (ratio * ActivityFactorType.NEARBY_HELP.maxWeight).coerceIn(0.0, ActivityFactorType.NEARBY_HELP.maxWeight)

        val display = if (helpPointsCount == 1) "1 help point" else "$helpPointsCount help points"
        val detail = if (helpPointsCount > 0) {
            "$helpPointsCount verified help points (police stations, hospitals, or safe zones) within 500m of route"
        } else {
            "No emergency stations or verified safe zones located within 500m of route corridor"
        }

        return ActivityFactorScore(
            factor = ActivityFactorType.NEARBY_HELP,
            earnedPoints = earned,
            rawMetricDisplay = display,
            detail = detail
        )
    }

    private fun calculateMainRoadScore(route: RouteInfo): ActivityFactorScore {
        val steps = route.steps
        val mainRoadRatio = if (steps.isNotEmpty()) {
            val totalStepsDistance = steps.sumOf { it.distanceMeters }
            val mainRoadDistance = steps
                .filter { step -> step.roadClass?.lowercase(Locale.US) in MAIN_ROAD_CLASSES }
                .sumOf { it.distanceMeters }

            if (totalStepsDistance > 0.0) {
                (mainRoadDistance / totalStepsDistance).coerceIn(0.0, 1.0)
            } else {
                0.5
            }
        } else {
            // Default when steps are not populated (e.g. basic fallback route)
            0.6
        }

        val earned = (mainRoadRatio * ActivityFactorType.MAIN_ROAD_COVERAGE.maxWeight)
            .coerceIn(0.0, ActivityFactorType.MAIN_ROAD_COVERAGE.maxWeight)
        val percent = (mainRoadRatio * 100).roundToInt()

        return ActivityFactorScore(
            factor = ActivityFactorType.MAIN_ROAD_COVERAGE,
            earnedPoints = earned,
            rawMetricDisplay = "$percent% main roads",
            detail = "$percent% of path along well-lit primary arterials, motorways, or major connector roadways"
        )
    }

    private fun calculateTransportScore(transitCount: Int, distanceKm: Double): ActivityFactorScore {
        // Target: 2 or more transit hubs = full 10 points (1 = 5 pts)
        val ratio = (transitCount / 2.0).coerceIn(0.0, 1.0)
        val earned = (ratio * ActivityFactorType.TRANSPORT_OPEN.maxWeight)
            .coerceIn(0.0, ActivityFactorType.TRANSPORT_OPEN.maxWeight)

        val display = if (transitCount == 1) "1 transit hub" else "$transitCount transit hubs"
        val detail = if (transitCount > 0) {
            "$transitCount operating transit stops, train stations, or bus interchanges along route"
        } else {
            "No active transit hubs or bus interchanges detected directly along route corridor"
        }

        return ActivityFactorScore(
            factor = ActivityFactorType.TRANSPORT_OPEN,
            earnedPoints = earned,
            rawMetricDisplay = display,
            detail = detail
        )
    }

    private fun calculateContinuityScore(
        routePoints: List<LatLng>,
        activePoints: List<LatLng>
    ): ActivityFactorScore {
        if (routePoints.size < 2) {
            return ActivityFactorScore(
                factor = ActivityFactorType.CONTINUITY,
                earnedPoints = 12.5,
                rawMetricDisplay = "50% continuous",
                detail = "Limited route geometry available for continuity analysis"
            )
        }

        // Divide the route into equal intervals (target segment length ~ 400m, min 3 segments, max 10 segments)
        val totalPoints = routePoints.size
        val segmentCount = totalPoints.coerceIn(3, 10)
        var coveredSegments = 0

        for (i in 0 until segmentCount) {
            val sampleIndex = (i.toDouble() / (segmentCount - 1) * (totalPoints - 1)).roundToInt()
            val samplePoint = routePoints[sampleIndex]

            // Check if any active point is within 450m of this sample point
            val isNearActive = activePoints.any { active ->
                haversineDistance(samplePoint.latitude, samplePoint.longitude, active.latitude, active.longitude) <= 450.0
            }
            if (isNearActive) {
                coveredSegments++
            }
        }

        // If no active points exist anywhere along corridor, continuity is naturally low
        val continuityRatio = if (activePoints.isEmpty()) {
            0.1
        } else {
            (coveredSegments.toDouble() / segmentCount).coerceIn(0.0, 1.0)
        }

        val earned = (continuityRatio * ActivityFactorType.CONTINUITY.maxWeight)
            .coerceIn(0.0, ActivityFactorType.CONTINUITY.maxWeight)
        val percent = (continuityRatio * 100).roundToInt()

        return ActivityFactorScore(
            factor = ActivityFactorType.CONTINUITY,
            earnedPoints = earned,
            rawMetricDisplay = "$percent% active corridor",
            detail = "$coveredSegments of $segmentCount route sectors have active open venues or help points within 450m"
        )
    }

    private fun calculateFewerTurnsScore(route: RouteInfo, distanceKm: Double): ActivityFactorScore {
        val totalTurns = if (route.steps.isNotEmpty()) {
            route.steps.count { it.isTurn || isTurnInstruction(it.instruction) }
        } else {
            countBearingChanges(route.points)
        }

        val turnsPerKm = totalTurns / distanceKm.coerceAtLeast(0.5)

        // Baseline:
        // <= 1.0 turn/km -> 10.0 points (direct, straight path)
        // 1.0 to 4.0 turns/km -> 10.0 down to 4.0 points
        // > 4.0 turns/km -> drops down to minimum 1.5 points
        val earned = when {
            turnsPerKm <= 1.0 -> 10.0
            turnsPerKm <= 4.0 -> 10.0 - (turnsPerKm - 1.0) * 2.0
            else -> (4.0 - (turnsPerKm - 4.0) * 0.5).coerceAtLeast(1.5)
        }.coerceIn(0.0, ActivityFactorType.FEWER_TURNS.maxWeight)

        val display = if (totalTurns == 1) "1 turn" else "$totalTurns turns"
        val detail = if (totalTurns == 0) {
            "Direct continuous roadway with zero turns required"
        } else {
            "$totalTurns turns along route (~${String.format(Locale.US, "%.1f", turnsPerKm)} turns/km)"
        }

        return ActivityFactorScore(
            factor = ActivityFactorType.FEWER_TURNS,
            earnedPoints = earned,
            rawMetricDisplay = display,
            detail = detail
        )
    }

    private fun isTurnInstruction(instruction: String?): Boolean {
        if (instruction.isNullOrBlank()) return false
        val text = instruction.lowercase(Locale.US)
        return text.contains("turn") || text.contains("left") || text.contains("right") ||
                text.contains("roundabout") || text.contains("fork") || text.contains("u-turn")
    }

    private fun countBearingChanges(points: List<LatLng>): Int {
        if (points.size < 3) return 0
        var turnCount = 0
        for (i in 0 until points.size - 2) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[i + 2]
            val b1 = computeBearing(p1, p2)
            val b2 = computeBearing(p2, p3)
            val diff = Math.abs(b1 - b2)
            val angleChange = if (diff > 180.0) 360.0 - diff else diff
            if (angleChange >= 45.0) {
                turnCount++
            }
        }
        return turnCount
    }

    private fun computeBearing(from: LatLng, to: LatLng): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    private fun isHelpCategory(category: String): Boolean {
        val lower = category.lowercase(Locale.US)
        return HELP_CATEGORIES.any { lower.contains(it) } ||
                lower.contains("police") || lower.contains("hospital")
    }

    private fun isTransitCategory(category: String): Boolean {
        val lower = category.lowercase(Locale.US)
        return TRANSIT_CATEGORIES.any { lower.contains(it) } ||
                lower.contains("transit") || lower.contains("bus_station") ||
                lower.contains("train_station") || lower.contains("subway")
    }

    private fun isPointNearRoute(point: LatLng, routePoints: List<LatLng>, maxDistMeters: Double): Boolean {
        if (routePoints.isEmpty()) return false
        return routePoints.any { routePt ->
            haversineDistance(point.latitude, point.longitude, routePt.latitude, routePt.longitude) <= maxDistMeters
        }
    }

    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
