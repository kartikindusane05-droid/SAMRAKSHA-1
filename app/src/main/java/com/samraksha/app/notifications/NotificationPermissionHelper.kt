package com.samraksha.app.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Utility helper for managing runtime and channel permissions for notifications.
 *
 * Starting in Android 13 (API level 33, Tiramisu), apps must declare and request the
 * [Manifest.permission.POST_NOTIFICATIONS] runtime permission to show notifications.
 *
 * ## Principles:
 * - Only requested when contextually required (e.g. before starting a journey sentinel
 *   or configuring safety alert subscriptions).
 * - Clear rationale provided before presenting system prompt.
 * - Respects older Android versions where permission is granted at install time.
 */
object NotificationPermissionHelper {

    /**
     * The runtime permission string required on Android 13+ (API 33+).
     * Null on older API levels.
     */
    val PERMISSION: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else {
        null
    }

    /**
     * Array containing the runtime permission required on API 33+, or empty if not applicable.
     */
    val REQUIRED_PERMISSIONS: Array<String> = if (PERMISSION != null) {
        arrayOf(PERMISSION)
    } else {
        emptyArray()
    }

    /**
     * Checks if notification permission is granted.
     * On Android 12L (API 32) and lower, runtime permission is not required and returns true.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Comprehensive check verifying both runtime permission and that notifications
     * are not blocked at the app or OS level.
     */
    fun canPostNotifications(context: Context): Boolean {
        val hasRuntime = hasNotificationPermission(context)
        val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return hasRuntime && areNotificationsEnabled
    }

    /**
     * Opens the device Notification Settings directly for SAMRAKSHA.
     * Uses ACTION_APP_NOTIFICATION_SETTINGS on Android 8.0+ (API 26+) for direct channel access,
     * falling back to Application Details Settings on older versions.
     */
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    private const val PREFS_NAME = "samraksha_permissions"
    private const val KEY_NOTIFICATION_PROMPTED = "notification_prompted_once"

    fun markPermissionRequested(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATION_PROMPTED, true)
            .apply()
    }

    fun hasRequestedPermissionBefore(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATION_PROMPTED, false)
    }

    /**
     * Checks if notification permission is permanently denied on Android 13+ (API 33+).
     * If user denied previously and shouldShowRequestPermissionRationale is false,
     * the system dialog will not appear and user must configure it in Settings.
     * On first launch (never requested before), this returns false.
     */
    fun isPermanentlyDenied(activity: android.app.Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val perm = Manifest.permission.POST_NOTIFICATIONS
        val isGranted = ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED
        if (isGranted) return false
        if (!hasRequestedPermissionBefore(activity)) return false
        val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        return !shouldShowRationale
    }

    // ── Rationale copy for user transparency ─────────────────────────────────

    const val RATIONALE_TITLE = "Enable Safety Notifications"

    const val RATIONALE_MESSAGE =
        "SAMRAKSHA uses notifications strictly for urgent safety events: live journey sentinel status, risk escalation warnings, and distress SOS alerts. We never send marketing or promotional notifications."

    const val RATIONALE_PRIVACY_NOTE =
        "All notification dispatch is handled locally or via encrypted channels. Your alerts are private to you and your trusted guardians."

    const val PERMANENTLY_DENIED_TITLE = "Notifications Disabled"

    const val PERMANENTLY_DENIED_MESSAGE =
        "Notifications are disabled in system settings. To receive journey sentinel updates and danger zone warnings while the app is backgrounded, please enable Notifications."
}
