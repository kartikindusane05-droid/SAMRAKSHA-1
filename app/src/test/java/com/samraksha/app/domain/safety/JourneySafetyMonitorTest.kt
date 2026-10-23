package com.samraksha.app.domain.safety

import org.maplibre.android.geometry.LatLng
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.screens.location.model.SafetyZoneShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneySafetyMonitorTest {

    private val safeZone = SafetyZone(
        id = "zone-safe",
        name = "Central Metro Hub",
        level = SafetyZoneLevel.SAFE,
        shape = SafetyZoneShape.Circular(
            center = LatLng(12.9716, 77.5946),
            radiusMeters = 300.0
        ),
        centerPoint = LatLng(12.9716, 77.5946),
        safetyScore = 92,
        advisory = "Verified safe corridor with CCTV and security."
    )

    private val moderateZone = SafetyZone(
        id = "zone-mod",
        name = "Commercial Alleyway",
        level = SafetyZoneLevel.MODERATE_RISK,
        shape = SafetyZoneShape.Circular(
            center = LatLng(12.9800, 77.6000),
            radiusMeters = 250.0
        ),
        centerPoint = LatLng(12.9800, 77.6000),
        safetyScore = 60,
        advisory = "Caution advised after dark."
    )

    private val unsafeZone = SafetyZone(
        id = "zone-hazard",
        name = "Unlit Underpass Hotspot",
        level = SafetyZoneLevel.UNSAFE,
        shape = SafetyZoneShape.Circular(
            center = LatLng(12.9900, 77.6100),
            radiusMeters = 200.0
        ),
        centerPoint = LatLng(12.9900, 77.6100),
        safetyScore = 25,
        advisory = "High hazard alert. Minimal lighting and past incidents."
    )

    private val polygonZone = SafetyZone(
        id = "zone-poly",
        name = "University Safe Perimeter",
        level = SafetyZoneLevel.SAFE,
        shape = SafetyZoneShape.Polygon(
            points = listOf(
                LatLng(13.0000, 77.5000),
                LatLng(13.0100, 77.5000),
                LatLng(13.0100, 77.5100),
                LatLng(13.0000, 77.5100)
            )
        ),
        centerPoint = LatLng(13.0050, 77.5050),
        safetyScore = 95,
        advisory = "Campus security patrols 24/7."
    )

    @Test
    fun calculateDistanceMeters_returnsAccurateDistance() {
        // Distance between identical coordinates is 0
        val zeroDist = JourneySafetyMonitor.calculateDistanceMeters(12.9716, 77.5946, 12.9716, 77.5946)
        assertEquals(0.0, zeroDist, 0.001)

        // Approximate 1 deg latitude is ~111 km
        val distance = JourneySafetyMonitor.calculateDistanceMeters(12.0, 77.0, 13.0, 77.0)
        assertTrue("Distance should be roughly 111km", distance in 110_000.0..112_000.0)
    }

    @Test
    fun isInsideZone_circular_evaluatesCorrectly() {
        // Center point is inside
        assertTrue(JourneySafetyMonitor.isInsideZone(12.9716, 77.5946, safeZone))

        // Point ~1000m away is outside
        assertFalse(JourneySafetyMonitor.isInsideZone(12.9850, 77.5946, safeZone))
    }

    @Test
    fun isInsideZone_polygon_evaluatesCorrectly() {
        // Point clearly inside bounding box
        assertTrue(JourneySafetyMonitor.isInsideZone(13.0050, 77.5050, polygonZone))

        // Point outside bounding box
        assertFalse(JourneySafetyMonitor.isInsideZone(13.0200, 77.5050, polygonZone))
    }

    @Test
    fun evaluateJourneySafety_insideUnsafeZone_yieldsHighRiskAndWarning() {
        val availableZones = listOf(safeZone, moderateZone, unsafeZone)

        // Located at center of unsafe hotspot
        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = 12.9900,
            userLng = 77.6100,
            availableZones = availableZones
        )

        assertEquals(JourneyRiskLevel.HIGH, report.currentRiskLevel)
        assertEquals(SafetyLevel.CRITICAL, report.currentRiskLevel.safetyLevel)
        assertEquals(unsafeZone.id, report.currentZone?.id)
        assertTrue("Safety score should reflect unsafe zone score", report.safetyScore <= 35)

        // Warnings check
        assertTrue("Should produce warning", report.warnings.isNotEmpty())
        val hazardWarning = report.warnings.first()
        assertEquals(JourneyRiskLevel.HIGH, hazardWarning.riskLevel)
        assertTrue(hazardWarning.message.contains(unsafeZone.name))
    }

    @Test
    fun evaluateJourneySafety_approachingUnsafeZone_yieldsHighRisk() {
        val availableZones = listOf(safeZone, moderateZone, unsafeZone)

        // Position user ~150 meters from edge of unsafeZone
        // Center is at (12.9900, 77.6100), radius is 200m
        // ~300m away from center = ~100m from perimeter
        val testLat = 12.9900 + 0.0025 // ~275m north

        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = testLat,
            userLng = 77.6100,
            availableZones = availableZones
        )

        assertEquals(JourneyRiskLevel.HIGH, report.currentRiskLevel)
        assertTrue(report.warnings.any { it.message.contains(unsafeZone.name) })
    }

    @Test
    fun evaluateJourneySafety_insideModerateZone_yieldsModerateRisk() {
        val availableZones = listOf(safeZone, moderateZone, unsafeZone)

        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = 12.9800,
            userLng = 77.6000,
            availableZones = availableZones
        )

        assertEquals(JourneyRiskLevel.MODERATE, report.currentRiskLevel)
        assertEquals(SafetyLevel.WARNING, report.currentRiskLevel.safetyLevel)
        assertEquals(moderateZone.id, report.currentZone?.id)
        assertTrue(report.warnings.any { it.title.contains("Moderate") })
    }

    @Test
    fun evaluateJourneySafety_insideSafeZone_yieldsSafeRisk() {
        val availableZones = listOf(safeZone, moderateZone, unsafeZone)

        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = 12.9716,
            userLng = 77.5946,
            availableZones = availableZones
        )

        assertEquals(JourneyRiskLevel.SAFE, report.currentRiskLevel)
        assertEquals(SafetyLevel.SAFE, report.currentRiskLevel.safetyLevel)
        assertEquals(safeZone.id, report.currentZone?.id)
        assertTrue(report.safetyScore >= 90)
        assertTrue(report.warnings.isEmpty())
    }

    @Test
    fun evaluateJourneySafety_dismissedWarning_isExcluded() {
        val availableZones = listOf(unsafeZone)
        val warnId = "warn-inside-${unsafeZone.id}"

        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = 12.9900,
            userLng = 77.6100,
            availableZones = availableZones,
            dismissedWarningIds = setOf(warnId)
        )

        // High risk remains, but dismissed warning is not re-nagged
        assertEquals(JourneyRiskLevel.HIGH, report.currentRiskLevel)
        assertTrue(report.warnings.none { it.id == warnId })
    }

    @Test
    fun evaluateJourneySafety_uninitializedCoords_defaultsToSafe() {
        val report = JourneySafetyMonitor.evaluateJourneySafety(
            userLat = 0.0,
            userLng = 0.0,
            availableZones = listOf(unsafeZone)
        )

        assertEquals(JourneyRiskLevel.SAFE, report.currentRiskLevel)
        assertTrue(report.warnings.isEmpty())
    }
}
