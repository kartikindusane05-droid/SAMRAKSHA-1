package com.samraksha.app.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.samraksha.app.domain.model.RouteStepInfo
import org.maplibre.android.geometry.LatLng
import java.util.Locale

/**
 * Geoapify Routing API GeoJSON response container.
 * Format: FeatureCollection with LineString or MultiLineString geometry.
 */
data class GeoapifyRoutingResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("features") val features: List<GeoapifyRoutingFeature>? = null
)

data class GeoapifyRoutingFeature(
    @SerializedName("type") val type: String? = null,
    @SerializedName("properties") val properties: GeoapifyRoutingProperties? = null,
    @SerializedName("geometry") val geometry: GeoapifyRoutingGeometry? = null
)

data class GeoapifyRoutingProperties(
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("distance") val distance: Double? = null, // in meters
    @SerializedName("time") val time: Double? = null,          // in seconds
    @SerializedName("legs") val legs: List<GeoapifyLeg>? = null
) {
    fun toRouteStepInfoList(): List<RouteStepInfo> {
        val result = mutableListOf<RouteStepInfo>()
        legs?.forEach { leg ->
            leg.steps?.forEach { step ->
                val dist = step.distance ?: 0.0
                val time = step.time ?: 0.0
                val roadClass = step.roadClass
                val instructionText = step.instruction?.text
                val instructionType = step.instruction?.type?.lowercase(Locale.US)
                val isTurn = instructionType != null && instructionType != "none" && instructionType != "straight"
                result.add(
                    RouteStepInfo(
                        distanceMeters = dist,
                        timeSeconds = time,
                        roadClass = roadClass,
                        instruction = instructionText,
                        isTurn = isTurn
                    )
                )
            }
        }
        return result
    }
}

data class GeoapifyLeg(
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("time") val time: Double? = null,
    @SerializedName("steps") val steps: List<GeoapifyStep>? = null
)

data class GeoapifyStep(
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("time") val time: Double? = null,
    @SerializedName("road_class") val roadClass: String? = null,
    @SerializedName("instruction") val instruction: GeoapifyInstruction? = null
)

data class GeoapifyInstruction(
    @SerializedName("text") val text: String? = null,
    @SerializedName("type") val type: String? = null
)

data class GeoapifyRoutingGeometry(
    @SerializedName("type") val type: String? = null,
    @SerializedName("coordinates") val coordinates: JsonElement? = null
) {
    /**
     * Extracts coordinates list handling both LineString and MultiLineString GeoJSON geometries.
     * LineString: [ [lon, lat], [lon, lat], ... ]
     * MultiLineString: [ [ [lon, lat], ... ], [ [lon, lat], ... ] ]
     */
    fun toLatLngList(): List<LatLng> {
        val coords = coordinates ?: return emptyList()
        val result = mutableListOf<LatLng>()
        try {
            if (!coords.isJsonArray) return emptyList()
            val outerArray = coords.asJsonArray
            if (outerArray.size() == 0) return emptyList()

            val firstElem = outerArray.get(0)
            if (firstElem.isJsonArray) {
                val firstArr = firstElem.asJsonArray
                if (firstArr.size() > 0 && firstArr.get(0).isJsonArray) {
                    // MultiLineString: Array of line strings
                    for (lineElement in outerArray) {
                        if (lineElement.isJsonArray) {
                            for (pointElement in lineElement.asJsonArray) {
                                parseCoordinatePair(pointElement)?.let { result.add(it) }
                            }
                        }
                    }
                } else {
                    // LineString: Array of coordinate pairs [lon, lat]
                    for (pointElement in outerArray) {
                        parseCoordinatePair(pointElement)?.let { result.add(it) }
                    }
                }
            }
        } catch (_: Exception) {
            // Return whatever points could be parsed safely
        }
        return result
    }

    private fun parseCoordinatePair(element: JsonElement): LatLng? {
        return try {
            if (!element.isJsonArray) return null
            val pt = element.asJsonArray
            if (pt.size() >= 2) {
                val lon = pt.get(0).asDouble
                val lat = pt.get(1).asDouble
                LatLng(lat, lon)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Geoapify Geocoding API response container.
 */
data class GeoapifyGeocodeResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("features") val features: List<GeoapifyGeocodeFeature>? = null
)

data class GeoapifyGeocodeFeature(
    @SerializedName("properties") val properties: GeoapifyGeocodeProperties? = null
)

data class GeoapifyGeocodeProperties(
    @SerializedName("place_id") val placeId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("formatted") val formatted: String? = null,
    @SerializedName("address_line1") val addressLine1: String? = null,
    @SerializedName("address_line2") val addressLine2: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("result_type") val resultType: String? = null,
    @SerializedName("category") val category: String? = null
)

/**
 * Geoapify Places API response container.
 */
data class GeoapifyPlacesResponse(
    @SerializedName("type") val type: String? = null,
    @SerializedName("features") val features: List<GeoapifyPlaceFeature>? = null
)

data class GeoapifyPlaceFeature(
    @SerializedName("type") val type: String? = null,
    @SerializedName("properties") val properties: GeoapifyPlaceProperties? = null
)

data class GeoapifyPlaceProperties(
    @SerializedName("place_id") val placeId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("categories") val categories: List<String>? = null,
    @SerializedName("opening_hours") val openingHours: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("formatted") val formatted: String? = null
)
