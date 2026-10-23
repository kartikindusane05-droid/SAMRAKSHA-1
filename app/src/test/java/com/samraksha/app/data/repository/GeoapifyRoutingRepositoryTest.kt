package com.samraksha.app.data.repository

import com.google.gson.JsonParser
import com.samraksha.app.data.model.GeoapifyRoutingGeometry
import com.samraksha.app.domain.model.RouteInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.maplibre.android.geometry.LatLng

class GeoapifyRoutingRepositoryTest {

    @Test
    fun geometry_lineString_parsesToLatLngCorrectly() {
        val json = """
            {
                "type": "LineString",
                "coordinates": [
                    [77.5946, 12.9716],
                    [77.6000, 12.9800]
                ]
            }
        """.trimIndent()
        val jsonElement = JsonParser.parseString(json).asJsonObject.get("coordinates")
        val geometry = GeoapifyRoutingGeometry(
            type = "LineString",
            coordinates = jsonElement
        )

        val latLngList = geometry.toLatLngList()
        assertEquals(2, latLngList.size)
        assertEquals(12.9716, latLngList[0].latitude, 0.0001)
        assertEquals(77.5946, latLngList[0].longitude, 0.0001)
        assertEquals(12.9800, latLngList[1].latitude, 0.0001)
        assertEquals(77.6000, latLngList[1].longitude, 0.0001)
    }

    @Test
    fun geometry_multiLineString_parsesToLatLngCorrectly() {
        val json = """
            {
                "type": "MultiLineString",
                "coordinates": [
                    [
                        [77.5946, 12.9716],
                        [77.5980, 12.9750]
                    ],
                    [
                        [77.5980, 12.9750],
                        [77.6000, 12.9800]
                    ]
                ]
            }
        """.trimIndent()
        val jsonElement = JsonParser.parseString(json).asJsonObject.get("coordinates")
        val geometry = GeoapifyRoutingGeometry(
            type = "MultiLineString",
            coordinates = jsonElement
        )

        val latLngList = geometry.toLatLngList()
        assertEquals(4, latLngList.size)
        assertEquals(12.9716, latLngList[0].latitude, 0.0001)
        assertEquals(77.5946, latLngList[0].longitude, 0.0001)
        assertEquals(12.9800, latLngList[3].latitude, 0.0001)
        assertEquals(77.6000, latLngList[3].longitude, 0.0001)
    }

    @Test
    fun geometry_emptyOrNull_returnsEmptyList() {
        val emptyGeometry = GeoapifyRoutingGeometry(type = "LineString", coordinates = null)
        assertEquals(0, emptyGeometry.toLatLngList().size)
    }

    @Test
    fun routeInfo_formattedValues_formatMetersAndKilometersCorrectly() {
        val shortRoute = RouteInfo(
            points = listOf(LatLng(12.0, 77.0), LatLng(12.1, 77.1)),
            distanceMeters = 850.0,
            durationSeconds = 540.0,
            startPoint = LatLng(12.0, 77.0),
            destinationPoint = LatLng(12.1, 77.1),
            destinationName = "Short Walk"
        )
        assertEquals("850 m", shortRoute.formattedDistance)
        assertEquals("9 min", shortRoute.formattedDuration)

        val longRoute = RouteInfo(
            points = listOf(LatLng(12.0, 77.0), LatLng(12.1, 77.1)),
            distanceMeters = 15420.0,
            durationSeconds = 4500.0, // 1 hr 15 min
            startPoint = LatLng(12.0, 77.0),
            destinationPoint = LatLng(12.1, 77.1),
            destinationName = "Long Drive"
        )
        assertEquals("15.4 km", longRoute.formattedDistance)
        assertEquals("1h 15m", longRoute.formattedDuration)
    }

    @Test
    fun coordinateParsing_parsesCommaSeparatedLatLon() {
        val repo = GeoapifyRoutingRepositoryImpl()
        val parsed = repo.parseCoordinatesString("12.9716, 77.5946")
        assertNotNull(parsed)
        assertEquals(12.9716, parsed?.latitude ?: 0.0, 0.0001)
        assertEquals(77.5946, parsed?.longitude ?: 0.0, 0.0001)

        val invalid = repo.parseCoordinatesString("Not a coordinate string")
        assertNull(invalid)
    }
}
