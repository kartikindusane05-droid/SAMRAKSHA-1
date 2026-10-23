package com.samraksha.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility helper for managing location runtime permissions.
 *
 * ## Principles:
 * - Only requested when contextually required (e.g., viewing Safety Map, starting a Journey).
 * - Distinguishes between FINE and COARSE location for optimal accuracy without crashing.
 * - Adheres to zero-tracking outside active safety features.
 */
object LocationPermissionHelper {

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * Checks if either FINE or COARSE location permission has been granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return hasFine || hasCoarse
    }

    /**
     * Checks if high-accuracy (FINE) location permission is granted.
     */
    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if approximate (COARSE) location permission is granted.
     */
    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Enumerates the current status of location permission for fine-grained UI feedback.
     */
    enum class LocationPermissionStatus {
        GRANTED_FINE,
        GRANTED_COARSE_ONLY,
        DENIED
    }

    /**
     * Returns the current [LocationPermissionStatus].
     */
    fun getPermissionStatus(context: Context): LocationPermissionStatus {
        val hasFine = hasFineLocationPermission(context)
        val hasCoarse = hasCoarseLocationPermission(context)

        return when {
            hasFine -> LocationPermissionStatus.GRANTED_FINE
            hasCoarse -> LocationPermissionStatus.GRANTED_COARSE_ONLY
            else -> LocationPermissionStatus.DENIED
        }
    }

    /**
     * Opens the system App Details Settings page for SAMRAKSHA, allowing the user
     * to grant or toggle permissions when standard dialogs are blocked or denied.
     */
    fun openAppSettings(context: Context) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Opens the device Location Settings page (to toggle GPS / Location Services on).
     */
    fun openLocationSettings(context: Context) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
        ).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private const val PREFS_NAME = "samraksha_permissions"
    private const val KEY_LOCATION_PROMPTED = "location_prompted_once"

    fun markPermissionRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOCATION_PROMPTED, true)
            .apply()
    }

    fun hasRequestedPermissionBefore(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCATION_PROMPTED, false)
    }

    /**
     * Determines whether a location permission has been permanently denied.
     * In Android, if a permission is denied and `shouldShowRequestPermissionRationale`
     * returns false AFTER having been requested, the user selected "Don't ask again"
     * (or denied twice on newer Android versions).
     * On first launch (never requested before), this returns false.
     */
    fun isPermanentlyDenied(activity: android.app.Activity, permission: String = Manifest.permission.ACCESS_FINE_LOCATION): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        if (isGranted) return false
        if (!hasRequestedPermissionBefore(activity)) return false
        val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return !shouldShowRationale
    }

    // ── Rationale copy for user transparency ─────────────────────────────────

    const val RATIONALE_TITLE = "Location Access for Safety"

    const val RATIONALE_MESSAGE =
        "SAMRAKSHA requires your location to display verified safety zones, monitor your transit corridor, and transmit live coordinates to your emergency guardians if you trigger SOS."

    const val RATIONALE_PRIVACY_NOTE =
        "Your location is processed directly on your device during active journeys and distress events. We never sell, track, or share your movement data for marketing or commercial profiling."

    const val PERMANENTLY_DENIED_TITLE = "Location Access Disabled"

    const val PERMANENTLY_DENIED_MESSAGE =
        "Location access was previously declined. To use live safety zones, transit sentinel tracking, or automatic SOS coordinates, please enable Location in your device settings."
}
