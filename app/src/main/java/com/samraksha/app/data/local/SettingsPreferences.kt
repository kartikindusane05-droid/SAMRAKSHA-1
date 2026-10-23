package com.samraksha.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Manages persistent user preferences for theme, emergency preferences,
 * and notification preferences using SharedPreferences and reactive StateFlows.
 */
class SettingsPreferences(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ── Theme Mode ────────────────────────────────────────────────────────────
    private val _themeMode = MutableStateFlow(
        prefs?.getString(KEY_THEME_MODE, null)?.let {
            try {
                ThemeMode.valueOf(it)
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            }
        } ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // ── Notifications ─────────────────────────────────────────────────────────
    private val _emergencyAlerts = MutableStateFlow(prefs?.getBoolean(KEY_EMERGENCY_ALERTS, true) ?: true)
    val emergencyAlerts: StateFlow<Boolean> = _emergencyAlerts.asStateFlow()

    private val _zoneAlerts = MutableStateFlow(prefs?.getBoolean(KEY_ZONE_ALERTS, true) ?: true)
    val zoneAlerts: StateFlow<Boolean> = _zoneAlerts.asStateFlow()

    private val _journeyReminders = MutableStateFlow(prefs?.getBoolean(KEY_JOURNEY_REMINDERS, true) ?: true)
    val journeyReminders: StateFlow<Boolean> = _journeyReminders.asStateFlow()

    private val _soundAlerts = MutableStateFlow(prefs?.getBoolean(KEY_SOUND_ALERTS, true) ?: true)
    val soundAlerts: StateFlow<Boolean> = _soundAlerts.asStateFlow()

    private val _vibration = MutableStateFlow(prefs?.getBoolean(KEY_VIBRATION, true) ?: true)
    val vibration: StateFlow<Boolean> = _vibration.asStateFlow()

    // ── Emergency Preferences ─────────────────────────────────────────────────
    private val _shakeToSos = MutableStateFlow(prefs?.getBoolean(KEY_SHAKE_TO_SOS, false) ?: false)
    val shakeToSos: StateFlow<Boolean> = _shakeToSos.asStateFlow()

    private val _sosCountdownSeconds = MutableStateFlow(prefs?.getInt(KEY_SOS_COUNTDOWN, 5) ?: 5)
    val sosCountdownSeconds: StateFlow<Int> = _sosCountdownSeconds.asStateFlow()

    private val _autoRecordAudio = MutableStateFlow(prefs?.getBoolean(KEY_AUTO_RECORD_AUDIO, true) ?: true)
    val autoRecordAudio: StateFlow<Boolean> = _autoRecordAudio.asStateFlow()

    private val _smsFallback = MutableStateFlow(prefs?.getBoolean(KEY_SMS_FALLBACK, true) ?: true)
    val smsFallback: StateFlow<Boolean> = _smsFallback.asStateFlow()

    private val _batterySaverSafeMode = MutableStateFlow(prefs?.getBoolean(KEY_BATTERY_SAVER_SAFE_MODE, false) ?: false)
    val batterySaverSafeMode: StateFlow<Boolean> = _batterySaverSafeMode.asStateFlow()

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun setEmergencyAlerts(enabled: Boolean) {
        _emergencyAlerts.value = enabled
        prefs?.edit()?.putBoolean(KEY_EMERGENCY_ALERTS, enabled)?.apply()
    }

    fun setZoneAlerts(enabled: Boolean) {
        _zoneAlerts.value = enabled
        prefs?.edit()?.putBoolean(KEY_ZONE_ALERTS, enabled)?.apply()
    }

    fun setJourneyReminders(enabled: Boolean) {
        _journeyReminders.value = enabled
        prefs?.edit()?.putBoolean(KEY_JOURNEY_REMINDERS, enabled)?.apply()
    }

    fun setSoundAlerts(enabled: Boolean) {
        _soundAlerts.value = enabled
        prefs?.edit()?.putBoolean(KEY_SOUND_ALERTS, enabled)?.apply()
    }

    fun setVibration(enabled: Boolean) {
        _vibration.value = enabled
        prefs?.edit()?.putBoolean(KEY_VIBRATION, enabled)?.apply()
    }

    fun setShakeToSos(enabled: Boolean) {
        _shakeToSos.value = enabled
        prefs?.edit()?.putBoolean(KEY_SHAKE_TO_SOS, enabled)?.apply()
    }

    fun setSosCountdownSeconds(seconds: Int) {
        _sosCountdownSeconds.value = seconds
        prefs?.edit()?.putInt(KEY_SOS_COUNTDOWN, seconds)?.apply()
    }

    fun setAutoRecordAudio(enabled: Boolean) {
        _autoRecordAudio.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_RECORD_AUDIO, enabled)?.apply()
    }

    fun setSmsFallback(enabled: Boolean) {
        _smsFallback.value = enabled
        prefs?.edit()?.putBoolean(KEY_SMS_FALLBACK, enabled)?.apply()
    }

    fun setBatterySaverSafeMode(enabled: Boolean) {
        _batterySaverSafeMode.value = enabled
        prefs?.edit()?.putBoolean(KEY_BATTERY_SAVER_SAFE_MODE, enabled)?.apply()
    }

    companion object {
        private const val PREFS_NAME = "samraksha_settings_prefs"
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_EMERGENCY_ALERTS = "pref_emergency_alerts"
        private const val KEY_ZONE_ALERTS = "pref_zone_alerts"
        private const val KEY_JOURNEY_REMINDERS = "pref_journey_reminders"
        private const val KEY_SOUND_ALERTS = "pref_sound_alerts"
        private const val KEY_VIBRATION = "pref_vibration"
        private const val KEY_SHAKE_TO_SOS = "pref_shake_to_sos"
        private const val KEY_SOS_COUNTDOWN = "pref_sos_countdown"
        private const val KEY_AUTO_RECORD_AUDIO = "pref_auto_record_audio"
        private const val KEY_SMS_FALLBACK = "pref_sms_fallback"
        private const val KEY_BATTERY_SAVER_SAFE_MODE = "pref_battery_saver_safe_mode"
    }
}
