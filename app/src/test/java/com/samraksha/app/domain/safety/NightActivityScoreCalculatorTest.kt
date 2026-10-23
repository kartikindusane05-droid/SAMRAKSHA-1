package com.samraksha.app.domain.safety

import com.samraksha.app.domain.model.ActivityFactorType
import com.samraksha.app.domain.model.ActivityLevel
import com.samraksha.app.domain.model.CorridorPoi
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteStepInfo
import com.samraksha.app.domain.model.ScoreConfidenceLevel
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.screens.location.model.SafetyZoneShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.maplibre.android.geometry.LatLng
import java.time.LocalDateTime

class NightActivityScoreCalculatorTest {

    private fun createSampleRoute(
        distanceMeters: Double = 2000.0,
        roadClass: String = "primary",
        turnCount: Int = 2
    ): RouteInfo {
        val points = listOf(
            LatLng(12.9716, 77.5946),
            LatLng(12.9750, 77.5980),
            LatLng(12.9800, 77.6020),
            LatLng(12.9850, 77.6050)
        )
        val steps = listOf(
            RouteStepInfo(distanceMeters = distanceMeters * 0.5, timeSeconds = 120.0, roadClass = roadClass, instruction = "Continue straight", isTurn = false),
            RouteStepInfo(distanceMeters = distanceMeters * 0.3, timeSeconds = 90.0, roadClass = roadClass, instruction = "Turn right onto 80ft Road", isTurn = turnCount > 0),
            RouteStepInfo(distanceMeters = distanceMeters * 0.2, timeSeconds = 60.0, roadClass = roadClass, instruction = "Turn left", isTurn = turnCount > 1)
        )
        return RouteInfo(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = 270.0,
            startPoint = points.first(),
            destinationPoint = points.last(),
            destinationName = "Sample Destination",
            steps = steps
        )
    }

    @Test
    fun calculate_maxScore_clampedTo100AndHighActivity() {
        val route = createSampleRoute(distanceMeters = 1500.0, roadClass = "primary", turnCount = 0)

        // Abundant open venues along corridor
        val corridorPois = listOf(
            CorridorPoi("1", "Supermarket 24/7", "commercial.supermarket", LatLng(12.9720, 77.5950), "24/7", VenueOpenStatus.OPEN, 50.0),
            CorridorPoi("2", "Pharmacy 24/7", "healthcare.pharmacy", LatLng(12.9750, 77.5980), "24/7", VenueOpenStatus.OPEN, 40.0),
            CorridorPoi("3", "Cafe", "catering.cafe", LatLng(12.9780, 77.6010), "Mo-Su 08:00-23:00", VenueOpenStatus.OPEN, 60.0),
            CorridorPoi("4", "Restaurant", "catering.restaurant", LatLng(12.9810, 77.6030), "Mo-Su 10:00-23:00", VenueOpenStatus.OPEN, 80.0),
            CorridorPoi("5", "Central Metro", "public_transport.subway", LatLng(12.9755, 77.5985), "24/7", VenueOpenStatus.OPEN, 30.0),
            CorridorPoi("6", "Bus Station", "public_transport.bus", LatLng(12.9830, 77.6040), "24/7", VenueOpenStatus.OPEN, 45.0),
            CorridorPoi("7", "City Police Station", "service.police", LatLng(12.9740, 77.5960), "24/7", VenueOpenStatus.OPEN, 100.0)
        )

        val safeZones = listOf(
            SafetyZone(
                id = "safe-1",
                name = "Downtown Safe Zone",
                level = SafetyZoneLevel.SAFE,
                shape = SafetyZoneShape.Circular(LatLng(12.9750, 77.5980), 300.0),
                centerPoint = LatLng(12.9750, 77.5980),
                safetyScore = 95,
                advisory = "Verified safe area"
            )
        )

        val result = NightActivityScoreCalculator.calculate(
            route = route,
            corridorPois = corridorPois,
            verifiedSafeZones = safeZones
        )

        assertTrue("Total score should be >= 85 for fully active corridor", result.totalScore >= 85)
        assertTrue("Total score must be clamped <= 100", result.totalScore <= 100)
        assertEquals(ActivityLevel.HIGH_ACTIVITY, result.activityLevel)
        assertEquals(ScoreConfidenceLevel.HIGH, result.confidenceLevel)

        // Verify all 6 factor scores sum to total
        val sumOfFactors = result.factorScores.sumOf { it.earnedPoints }
        assertEquals(result.totalScore, Math.round(sumOfFactors).toInt().coerceIn(0, 100))
    }

    @Test
    fun calculate_emptyCorridor_limitedActivity() {
        val points = listOf(LatLng(12.0, 77.0), LatLng(12.01, 77.01))
        val route = RouteInfo(
            points = points,
            distanceMeters = 3000.0,
            durationSeconds = 600.0,
            startPoint = points.first(),
            destinationPoint = points.last(),
            destinationName = "Isolated Road",
            steps = listOf(
                RouteStepInfo(distanceMeters = 3000.0, timeSeconds = 600.0, roadClass = "residential", instruction = "Turn left", isTurn = true)
            )
        )

        val result = NightActivityScoreCalculator.calculate(
            route = route,
            corridorPois = emptyList(),
            verifiedSafeZones = emptyList()
        )

        assertTrue("Empty corridor score should be <= 35", result.totalScore <= 35)
        assertEquals(ActivityLevel.LIMITED_ACTIVITY, result.activityLevel)
        assertEquals(0, result.openPlacesCount)
        assertEquals(0, result.totalPlacesEvaluated)
    }

    @Test
    fun calculate_unknownOperatingHours_reducesConfidenceGracefully() {
        val route = createSampleRoute()

        // 5 venues with UNKNOWN operating hours
        val pois = listOf(
            CorridorPoi("1", "Venue A", "commercial", LatLng(12.972, 77.595), null, VenueOpenStatus.UNKNOWN, 50.0),
            CorridorPoi("2", "Venue B", "commercial", LatLng(12.974, 77.597), "unknown", VenueOpenStatus.UNKNOWN, 50.0),
            CorridorPoi("3", "Venue C", "commercial", LatLng(12.976, 77.599), "", VenueOpenStatus.UNKNOWN, 50.0),
            CorridorPoi("4", "Venue D", "commercial", LatLng(12.978, 77.601), "variable", VenueOpenStatus.UNKNOWN, 50.0),
            CorridorPoi("5", "Venue E", "commercial", LatLng(12.980, 77.603), "unrecorded", VenueOpenStatus.UNKNOWN, 50.0)
        )

        val result = NightActivityScoreCalculator.calculate(
            route = route,
            corridorPois = pois
        )

        assertEquals(ScoreConfidenceLevel.LOW, result.confidenceLevel)
        assertTrue(result.confidenceDisclosure.contains("Reduced confidence"))
        assertEquals(5, result.unknownHoursCount)
        assertEquals(0, result.openPlacesCount) // UNKNOWN is never assumed to be OPEN
    }

    @Test
    fun calculate_straightRouteWithNoTurns_awardsFullTurnPoints() {
        val route = createSampleRoute(turnCount = 0)
        val result = NightActivityScoreCalculator.calculate(
            route = route,
            corridorPois = emptyList()
        )

        val fewerTurnsFactor = result.factorScores.first { it.factor == ActivityFactorType.FEWER_TURNS }
        assertEquals(10.0, fewerTurnsFactor.earnedPoints, 0.1)
    }

    @Test
    fun calculate_exactFactorWeightsSumTo100() {
        val sumMaxWeights = ActivityFactorType.entries.sumOf { it.maxWeight }
        assertEquals(100.0, sumMaxWeights, 0.001)

        val expectedWeights = mapOf(
            ActivityFactorType.OPEN_PLACES to 25.0,
            ActivityFactorType.NEARBY_HELP to 15.0,
            ActivityFactorType.MAIN_ROAD_COVERAGE to 15.0,
            ActivityFactorType.TRANSPORT_OPEN to 10.0,
            ActivityFactorType.CONTINUITY to 25.0,
            ActivityFactorType.FEWER_TURNS to 10.0
        )

        for ((factor, weight) in expectedWeights) {
            assertEquals(weight, factor.maxWeight, 0.001)
        }
    }
}
