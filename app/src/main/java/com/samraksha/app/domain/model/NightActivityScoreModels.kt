package com.samraksha.app.domain.model

import com.samraksha.app.domain.safety.VenueOpenStatus
import org.maplibre.android.geometry.LatLng

/**
 * The 6 core factors comprising the Night Activity Score.
 * Total maximum possible weight is strictly 100.0.
 */
enum class ActivityFactorType(
    val title: String,
    val maxWeight: Double,
    val description: String
) {
    OPEN_PLACES(
        title = "Open Venues & Stores",
        maxWeight = 25.0,
        description = "Active commercial venues, convenience stores, pharmacies, and dining locations operating along the corridor"
    ),
    NEARBY_HELP(
        title = "Nearby Verified Help Points",
        maxWeight = 15.0,
        description = "Verified safe zones, police stations, hospitals, and 24/7 emergency points within 500m of the route"
    ),
    MAIN_ROAD_COVERAGE(
        title = "Main-Road Coverage",
        maxWeight = 15.0,
        description = "Proportion of the route along well-lit primary arterials, motorways, or major connector roads"
    ),
    TRANSPORT_OPEN(
        title = "Active Transit Hubs",
        maxWeight = 10.0,
        description = "Operating public transit stations, bus interchanges, and metro stops with regular pedestrian activity"
    ),
    CONTINUITY(
        title = "Continuity of Active Corridors",
        maxWeight = 25.0,
        description = "Consistency of active places along route intervals without extended isolated or unmonitored gaps"
    ),
    FEWER_TURNS(
        title = "Direct & Connected Path",
        maxWeight = 10.0,
        description = "Simpler navigation with fewer turns and less complex maneuvers through side streets and alleys"
    )
}

/**
 * Detailed breakdown of points earned for a single factor.
 */
data class ActivityFactorScore(
    val factor: ActivityFactorType,
    val earnedPoints: Double,
    val maxPoints: Double = factor.maxWeight,
    val rawMetricDisplay: String,
    val detail: String
) {
    val percentage: Int
        get() = if (maxPoints > 0) Math.round((earnedPoints / maxPoints) * 100).toInt().coerceIn(0, 100) else 0
}

/**
 * Confidence level disclosing data completeness or absence along the evaluated corridor.
 * Missing opening hours are never assumed open or closed, but reduce score confidence.
 */
enum class ScoreConfidenceLevel(
    val label: String,
    val description: String
) {
    HIGH(
        label = "High Confidence",
        description = "Verified opening hours and infrastructure data available for the majority of venues along this corridor."
    ),
    MEDIUM(
        label = "Moderate Confidence",
        description = "Partial operating hours recorded. Some venues along this corridor have unverified operating schedules."
    ),
    LOW(
        label = "Reduced Confidence",
        description = "Limited operating hour data available in public registries for this corridor. Evaluated from road hierarchy and nearby permanent help points."
    )
}

/**
 * Activity tiers for visual badges and user guidance.
 */
enum class ActivityLevel(
    val title: String,
    val badgeLabel: String,
    val description: String,
    val colorHex: Long
) {
    HIGH_ACTIVITY(
        title = "High Night Activity",
        badgeLabel = "High Activity",
        description = "Well-trafficked corridor with active venues, main roads, and nearby help points.",
        colorHex = 0xFF22C55E // Emerald green
    ),
    MODERATE_ACTIVITY(
        title = "Moderate Activity",
        badgeLabel = "Moderate Activity",
        description = "Moderate corridor activity. Some segments pass through quiet or residential streets.",
        colorHex = 0xFFF59E0B // Amber
    ),
    LIMITED_ACTIVITY(
        title = "Limited Activity",
        badgeLabel = "Limited Activity",
        description = "Quiet corridor with fewer open venues or transit hubs late at night. Exercise awareness.",
        colorHex = 0xFF64748B // Slate
    )
}

/**
 * POI along route corridor evaluated for Night Activity.
 */
data class CorridorPoi(
    val id: String,
    val name: String,
    val category: String,
    val location: LatLng,
    val openingHoursRaw: String?,
    val openStatus: VenueOpenStatus,
    val distanceToRouteMeters: Double
)

/**
 * Step in a calculated route with road classification and maneuvers.
 */
data class RouteStepInfo(
    val distanceMeters: Double,
    val timeSeconds: Double,
    val roadClass: String? = null,
    val instruction: String? = null,
    val isTurn: Boolean = false
)

/**
 * Complete evaluation result for a route option's Night Activity Score.
 */
data class NightActivityScoreResult(
    val totalScore: Int, // strictly [0, 100]
    val maxScore: Int = 100,
    val activityLevel: ActivityLevel,
    val confidenceLevel: ScoreConfidenceLevel,
    val confidenceDisclosure: String,
    val factorScores: List<ActivityFactorScore>,
    val openPlacesCount: Int,
    val unknownHoursCount: Int,
    val totalPlacesEvaluated: Int,
    val evaluatedAtMillis: Long = System.currentTimeMillis()
)

/**
 * A selectable route option for side-by-side comparison.
 */
data class RouteOption(
    val id: String,
    val label: String,            // e.g. "Main Road Route (Balanced)", "Shortest Path"
    val strategy: String,         // "balanced", "short", "less_maneuvers"
    val routeInfo: RouteInfo,
    val nightActivityScore: NightActivityScoreResult,
    val isSelected: Boolean = false
)
