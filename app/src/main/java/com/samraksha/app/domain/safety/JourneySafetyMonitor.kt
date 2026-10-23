package com.samraksha.app.domain.safety

import org.maplibre.android.geometry.LatLng
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.screens.location.model.SafetyZoneShape
import java.util.UUID

/**
 * Risk categorization for active journey transit corridors.
 */
enum class JourneyRiskLevel(
    val title: String,
    val badgeLabel: String,
    val safetyLevel: SafetyLevel,
    val description: String
) {
    SAFE(
        title = "Safe Corridor",
        badgeLabel = "Verified Safe",
        safetyLevel = SafetyLevel.SAFE,
        description = "Journey transit corridor has high security coverage, verified safe havens, and active patrols."
    ),
    MODERATE(
        title = "Moderate Caution",
        badgeLabel = "Caution Advised",
        safetyLevel = SafetyLevel.WARNING,
        description = "Caution advised along transit path due to moderate lighting, reduced CCTV, or proximity to hazard sectors."
    ),
    HIGH(
        title = "High Risk Alert",
        badgeLabel = "High Risk Hotspot",
        safetyLevel = SafetyLevel.CRITICAL,
        description = "Located inside or closely approaching a verified high-risk hotspot with incident history. Stay vigilant."
    )
}

/**
 * Encapsulates a nearby safety zone relative to the user's current GPS location.
 */
data class NearbySafetyZone(
    val zone: SafetyZone,
    val distanceMeters: Int,
    val isInside: Boolean
) {
    val formattedDistance: String
        get() = if (isInside) {
            "Inside Area"
        } else if (distanceMeters < 1000) {
            "${distanceMeters} m away"
        } else {
            String.format(java.util.Locale.US, "%.1f km away", distanceMeters / 1000.0)
        }
}

/**
 * An actionable safety warning generated when risk increases during an active journey.
 */
data class JourneySafetyWarning(
    val id: String,
    val title: String,
    val message: String,
    val riskLevel: JourneyRiskLevel,
    val zoneName: String?,
    val distanceMeters: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Comprehensive safety report generated periodically during active journey tracking.
 */
data class JourneySafetyReport(
    val currentRiskLevel: JourneyRiskLevel,
    val currentZone: SafetyZone? = null,
    val nearbyZones: List<NearbySafetyZone> = emptyList(),
    val warnings: List<JourneySafetyWarning> = emptyList(),
    val closestSafeZone: NearbySafetyZone? = null,
    val closestHazardZone: NearbySafetyZone? = null,
    val safetyScore: Int = 100,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Battery-conscious in-memory evaluator for journey safety monitoring.
 *
 * Runs spatial checks against cached safety zones to evaluate containment,
 * proximity, dynamic risk level, and situational warnings without making
 * battery-draining network roundtrips on every periodic GPS fix.
 */
object JourneySafetyMonitor {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two GPS coordinates using the Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Ray-casting algorithm to determine if a GPS coordinate is inside a polygon boundary.
     */
    fun isPointInPolygon(userLat: Double, userLng: Double, polygon: List<LatLng>): Boolean {
        if (polygon.size < 3) return false
        var intersectCount = 0
        for (j in polygon.indices) {
            val i = if (j == 0) polygon.size - 1 else j - 1
            val p1 = polygon[i]
            val p2 = polygon[j]
            if ((p1.longitude > userLng) != (p2.longitude > userLng) &&
                userLat < (p2.latitude - p1.latitude) * (userLng - p1.longitude) / (p2.longitude - p1.longitude) + p1.latitude
            ) {
                intersectCount++
            }
        }
        return intersectCount % 2 == 1
    }

    /**
     * Checks if the user's coordinate is inside the defined shape of a safety zone.
     */
    fun isInsideZone(userLat: Double, userLng: Double, zone: SafetyZone): Boolean {
        return when (val shape = zone.shape) {
            is SafetyZoneShape.Circular -> {
                val dist = calculateDistanceMeters(
                    userLat, userLng,
                    shape.center.latitude, shape.center.longitude
                )
                dist <= shape.radiusMeters
            }
            is SafetyZoneShape.Polygon -> {
                isPointInPolygon(userLat, userLng, shape.points)
            }
        }
    }

    /**
     * Calculates distance from user to the center point or perimeter of a zone.
     */
    fun distanceToZone(userLat: Double, userLng: Double, zone: SafetyZone): Double {
        return when (val shape = zone.shape) {
            is SafetyZoneShape.Circular -> {
                val distToCenter = calculateDistanceMeters(
                    userLat, userLng,
                    shape.center.latitude, shape.center.longitude
                )
                (distToCenter - shape.radiusMeters).coerceAtLeast(0.0)
            }
            is SafetyZoneShape.Polygon -> {
                if (isPointInPolygon(userLat, userLng, shape.points)) {
                    0.0
                } else {
                    calculateDistanceMeters(
                        userLat, userLng,
                        zone.centerPoint.latitude, zone.centerPoint.longitude
                    )
                }
            }
        }
    }

    /**
     * Evaluates current user location against available safety zones.
     *
     * @param userLat Current latitude.
     * @param userLng Current longitude.
     * @param availableZones Curated safety zones (from local cache / repository).
     * @param maxNearbyDistanceMeters Radius in meters to consider zones as "nearby" (default 1500m).
     * @param dismissedWarningIds IDs of warnings previously dismissed by the user.
     */
    fun evaluateJourneySafety(
        userLat: Double,
        userLng: Double,
        availableZones: List<SafetyZone>,
        maxNearbyDistanceMeters: Double = 1500.0,
        dismissedWarningIds: Set<String> = emptySet()
    ): JourneySafetyReport {
        // Guard against uninitialized or default (0.0, 0.0) coordinates
        if (userLat == 0.0 && userLng == 0.0) {
            return JourneySafetyReport(
                currentRiskLevel = JourneyRiskLevel.SAFE,
                currentZone = null,
                nearbyZones = emptyList(),
                warnings = emptyList(),
                safetyScore = 90
            )
        }

        // 1. Identify containing zones (prioritize UNSAFE > MODERATE > SAFE for emergency safety)
        val containingZones = availableZones.filter { isInsideZone(userLat, userLng, it) }
        val currentZone = containingZones.firstOrNull { it.level == SafetyZoneLevel.UNSAFE }
            ?: containingZones.firstOrNull { it.level == SafetyZoneLevel.MODERATE_RISK }
            ?: containingZones.firstOrNull { it.level == SafetyZoneLevel.SAFE }

        // 2. Compute distances to all zones and gather nearby zones
        val proximityList = availableZones.map { zone ->
            val isInside = isInsideZone(userLat, userLng, zone)
            val dist = if (isInside) 0.0 else distanceToZone(userLat, userLng, zone)
            NearbySafetyZone(
                zone = zone,
                distanceMeters = dist.toInt(),
                isInside = isInside
            )
        }

        val nearbyZones = proximityList
            .filter { it.isInside || it.distanceMeters <= maxNearbyDistanceMeters }
            .sortedBy { it.distanceMeters }

        val closestHazardZone = nearbyZones.firstOrNull { it.zone.level == SafetyZoneLevel.UNSAFE }
        val closestSafeZone = nearbyZones.firstOrNull { it.zone.level == SafetyZoneLevel.SAFE }

        // 3. Determine Dynamic Risk Level & Safety Score
        val (riskLevel, calculatedScore) = when {
            // Case A: Inside an Unsafe Zone
            currentZone?.level == SafetyZoneLevel.UNSAFE -> {
                Pair(JourneyRiskLevel.HIGH, currentZone.safetyScore)
            }
            // Case B: Close proximity to an Unsafe Zone (within 250m)
            closestHazardZone != null && closestHazardZone.distanceMeters <= 250 -> {
                val score = (closestHazardZone.zone.safetyScore + (closestHazardZone.distanceMeters * 0.1)).toInt()
                Pair(JourneyRiskLevel.HIGH, score.coerceIn(20, 48))
            }
            // Case C: Inside a Moderate Risk Zone
            currentZone?.level == SafetyZoneLevel.MODERATE_RISK -> {
                Pair(JourneyRiskLevel.MODERATE, currentZone.safetyScore)
            }
            // Case D: Moderate proximity to an Unsafe Zone (within 600m)
            closestHazardZone != null && closestHazardZone.distanceMeters <= 600 -> {
                val score = (55 + (closestHazardZone.distanceMeters * 0.03)).toInt()
                Pair(JourneyRiskLevel.MODERATE, score.coerceIn(50, 68))
            }
            // Case E: Inside a Verified Safe Zone
            currentZone?.level == SafetyZoneLevel.SAFE -> {
                Pair(JourneyRiskLevel.SAFE, currentZone.safetyScore)
            }
            // Case F: Normal Transit Route
            else -> {
                val baseScore = if (closestSafeZone != null && closestSafeZone.distanceMeters <= 400) 88 else 82
                Pair(JourneyRiskLevel.SAFE, baseScore)
            }
        }

        // 4. Generate Situational Warnings
        val rawWarnings = mutableListOf<JourneySafetyWarning>()

        if (currentZone?.level == SafetyZoneLevel.UNSAFE) {
            val warnId = "warn-inside-${currentZone.id}"
            if (!dismissedWarningIds.contains(warnId)) {
                rawWarnings.add(
                    JourneySafetyWarning(
                        id = warnId,
                        title = "Hazard Alert: Inside High-Risk Hotspot",
                        message = "You have entered '${currentZone.name}'. ${currentZone.advisory} Stay on illuminated roads and keep emergency guardians linked.",
                        riskLevel = JourneyRiskLevel.HIGH,
                        zoneName = currentZone.name,
                        distanceMeters = 0
                    )
                )
            }
        } else if (closestHazardZone != null && closestHazardZone.distanceMeters <= 250) {
            val warnId = "warn-approaching-${closestHazardZone.zone.id}"
            if (!dismissedWarningIds.contains(warnId)) {
                rawWarnings.add(
                    JourneySafetyWarning(
                        id = warnId,
                        title = "Caution: Approaching High-Risk Hotspot",
                        message = "'${closestHazardZone.zone.name}' is only ${closestHazardZone.distanceMeters}m ahead. Advisory: ${closestHazardZone.zone.advisory}",
                        riskLevel = JourneyRiskLevel.HIGH,
                        zoneName = closestHazardZone.zone.name,
                        distanceMeters = closestHazardZone.distanceMeters
                    )
                )
            }
        } else if (currentZone?.level == SafetyZoneLevel.MODERATE_RISK) {
            val warnId = "warn-inside-mod-${currentZone.id}"
            if (!dismissedWarningIds.contains(warnId)) {
                rawWarnings.add(
                    JourneySafetyWarning(
                        id = warnId,
                        title = "Caution: Moderate Risk Area",
                        message = "Current route crosses '${currentZone.name}'. ${currentZone.advisory}",
                        riskLevel = JourneyRiskLevel.MODERATE,
                        zoneName = currentZone.name,
                        distanceMeters = 0
                    )
                )
            }
        } else if (closestHazardZone != null && closestHazardZone.distanceMeters <= 600) {
            val warnId = "warn-near-mod-${closestHazardZone.zone.id}"
            if (!dismissedWarningIds.contains(warnId)) {
                rawWarnings.add(
                    JourneySafetyWarning(
                        id = warnId,
                        title = "Awareness: Hazard Zone Nearby",
                        message = "'${closestHazardZone.zone.name}' is ${closestHazardZone.distanceMeters}m away. Avoid diverting onto unlit alleys.",
                        riskLevel = JourneyRiskLevel.MODERATE,
                        zoneName = closestHazardZone.zone.name,
                        distanceMeters = closestHazardZone.distanceMeters
                    )
                )
            }
        }

        return JourneySafetyReport(
            currentRiskLevel = riskLevel,
            currentZone = currentZone,
            nearbyZones = nearbyZones,
            warnings = rawWarnings,
            closestSafeZone = closestSafeZone,
            closestHazardZone = closestHazardZone,
            safetyScore = calculatedScore,
            evaluatedAt = System.currentTimeMillis()
        )
    }
}
