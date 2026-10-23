package com.samraksha.app.ui.screens.location.model

import androidx.compose.ui.graphics.Color
import org.maplibre.android.geometry.LatLng

/**
 * Safety risk tiers with distinct color coding, accessibility labels, and advisory guidelines.
 */
enum class SafetyZoneLevel(
    val title: String,
    val badgeLabel: String,
    val description: String,
    val strokeColor: Color,
    val fillColor: Color,
    val markerHue: Float
) {
    SAFE(
        title = "Safe Zone",
        badgeLabel = "Verified Safe",
        description = "High CCTV coverage, active public transit, regular security patrols, well-lit corridors.",
        strokeColor = Color(0xFF2E7D32),
        fillColor = Color(0x334CAF50),       // 20% opacity emerald green
        markerHue = 120f
    ),
    MODERATE_RISK(
        title = "Moderate Risk Zone",
        badgeLabel = "Caution Advised",
        description = "Moderate foot traffic, lower lighting in side alleys, or isolated commercial sectors at night.",
        strokeColor = Color(0xFFF57F17),
        fillColor = Color(0x33FFB300),       // 20% opacity amber
        markerHue = 40f
    ),
    UNSAFE(
        title = "Unsafe / High-Risk Area",
        badgeLabel = "High Risk Hotspot",
        description = "Reported hazard hotspot, poor lighting, minimal surveillance, or frequent incident reports.",
        strokeColor = Color(0xFFC62828),
        fillColor = Color(0x33F44336),       // 20% opacity emergency red
        markerHue = 0f
    ),
    UNKNOWN(
        title = "Unknown / Insufficient Data",
        badgeLabel = "Insufficient Data",
        description = "Limited or unverified safety records. Exercise standard caution.",
        strokeColor = Color(0xFF757575),
        fillColor = Color(0x339E9E9E),       // 20% opacity neutral gray
        markerHue = 210f
    )
}

/**
 * Geometric shape definition for a safety zone.
 */
sealed class SafetyZoneShape {
    data class Circular(
        val center: LatLng,
        val radiusMeters: Double
    ) : SafetyZoneShape()

    data class Polygon(
        val points: List<LatLng>
    ) : SafetyZoneShape()
}

/**
 * Reusable model representing a geographical zone with safety ratings,
 * provenance metadata, and actionable guidelines.
 */
data class SafetyZone(
    val id: String,
    val name: String,
    val level: SafetyZoneLevel,
    val shape: SafetyZoneShape,
    val centerPoint: LatLng,
    val safetyScore: Int,                 // 0 - 100 (-1 for Unknown / Insufficient Data)
    val advisory: String,
    val activePatrols: Int = 0,
    val cctvCoveragePercentage: Int = 0,
    val recentIncidentSummary: String? = null,
    val dataSource: String = "Official Municipal Safety Register",
    val lastUpdated: String? = null,
    val isProxyOrEstimated: Boolean = false,
    val scoreExplanation: String? = null
)
