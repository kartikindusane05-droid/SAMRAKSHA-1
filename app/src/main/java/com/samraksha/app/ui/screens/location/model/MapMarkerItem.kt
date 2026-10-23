package com.samraksha.app.ui.screens.location.model

import org.maplibre.android.geometry.LatLng

enum class MarkerCategory(val label: String) {
    USER_LOCATION("You"),
    POLICE_STATION("Police Station"),
    HOSPITAL("Hospital / Medical"),
    SAFE_ZONE("Verified Safe Zone"),
    EMERGENCY_INCIDENT("SOS Incident")
}

data class MapMarkerItem(
    val id: String,
    val position: LatLng,
    val title: String,
    val snippet: String? = null,
    val category: MarkerCategory = MarkerCategory.SAFE_ZONE
)
