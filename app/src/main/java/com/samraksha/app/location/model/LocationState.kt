package com.samraksha.app.location.model

/**
 * Represents all possible states of location queries and continuous streams.
 */
sealed interface LocationState {
    data object Idle : LocationState
    data object Loading : LocationState
    data class Success(val location: UserLocation) : LocationState

    sealed class Error(val message: String) : LocationState {
        data object PermissionDenied : Error("Location permission is denied")
        data object GpsDisabled : Error("Location services (GPS) are turned off")
        data class Unavailable(val reason: String = "Location is currently unavailable") : Error(reason)
    }
}
