package com.samraksha.app.emergency

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * On-device, conservative accelerometer-based shake detector for SAMRAKSHA.
 *
 * Detects exactly 3 deliberate, high-magnitude shakes within a short configurable time window
 * while the app is in the foreground. Ordinary movement, pocket jostling, and walking/running
 * are filtered out using acceleration thresholds and inter-shake debouncing.
 *
 * Threshold Tuning & Adjustment:
 * - [accelerationThresholdG]: The minimum G-force magnitude required to count as a shake impulse.
 *   Default is 1.45G (approx 14.2 m/s²). Normal device handling stays between 0.8G and 1.25G.
 *   Increase (e.g. to 1.8G) for higher resistance against false positives; decrease (e.g. to 1.3G)
 *   if users have difficulty producing forceful shakes.
 * - [minShakeIntervalMs]: Minimum elapsed time between two registered shakes (debounce window).
 *   Default is 250ms. Prevents counting the forward and backward swings of a single shake as two.
 * - [shakeWindowMs]: Maximum allowed time window between the first shake and the third shake.
 *   Default is 2000ms. If 3 shakes are not completed within this window, the counter resets.
 * - [cooldownMs]: Post-activation refractory period.
 *   Default is 3500ms. Suppresses all subsequent sensor triggers while countdown is active.
 * - [requiredShakeCount]: Number of qualifying shakes required (strictly 3).
 *
 * Privacy & Security:
 * - Processes sensor data strictly on-device in memory.
 * - Never logs, stores, or transmits raw accelerometer readings.
 * - No microphone, camera, or background service usage.
 */
class ShakeDetector(
    private val accelerationThresholdG: Float = DEFAULT_ACCELERATION_THRESHOLD_G,
    private val minShakeIntervalMs: Long = DEFAULT_MIN_SHAKE_INTERVAL_MS,
    private val shakeWindowMs: Long = DEFAULT_SHAKE_WINDOW_MS,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val requiredShakeCount: Int = DEFAULT_REQUIRED_SHAKE_COUNT,
    private val onThreeShakesDetected: () -> Unit
) : SensorEventListener {

    var isListening: Boolean = false
        private set

    private var sensorManager: SensorManager? = null

    // Algorithm state
    private var shakeCount = 0
    private var firstShakeTimeMs = 0L
    private var lastShakeTimeMs = 0L
    private var lastTriggerTimeMs = 0L

    companion object {
        const val DEFAULT_ACCELERATION_THRESHOLD_G = 1.45f // ~14.2 m/s² (1.45 x 9.80665)
        const val DEFAULT_MIN_SHAKE_INTERVAL_MS = 250L    // Debounce between shakes
        const val DEFAULT_SHAKE_WINDOW_MS = 2000L         // 2 seconds window for 3 shakes
        const val DEFAULT_COOLDOWN_MS = 3500L             // Cooldown after trigger
        const val DEFAULT_REQUIRED_SHAKE_COUNT = 3
        private const val GRAVITY_EARTH = 9.80665f
    }

    /**
     * Starts listening to accelerometer sensor events if a sensor is present.
     * Returns true if successfully registered, or false if device lacks an accelerometer.
     */
    fun startListening(manager: SensorManager?): Boolean {
        if (manager == null) return false
        if (isListening) return true

        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        val registered = manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        if (registered) {
            sensorManager = manager
            isListening = true
            reset()
        }
        return registered
    }

    /**
     * Immediately unregisters sensor listener and clears shake tracking state.
     */
    fun stopListening() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            sensorManager = null
            isListening = false
        }
        reset()
    }

    /**
     * Resets shake counter and timestamps.
     */
    fun reset() {
        shakeCount = 0
        firstShakeTimeMs = 0L
        lastShakeTimeMs = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values.getOrNull(0) ?: return
        val y = event.values.getOrNull(1) ?: return
        val z = event.values.getOrNull(2) ?: return
        val now = System.currentTimeMillis()
        processAcceleration(x, y, z, now)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for shake detection
    }

    /**
     * Pure acceleration evaluation method — public to enable comprehensive, deterministic unit testing
     * without needing an active Android SensorManager or physical device.
     *
     * Returns true if this event completed the 3-shake trigger condition.
     */
    fun processAcceleration(x: Float, y: Float, z: Float, nowMs: Long): Boolean {
        // Enforce post-trigger cooldown
        if (lastTriggerTimeMs > 0L && (nowMs - lastTriggerTimeMs < cooldownMs)) {
            return false
        }

        // Calculate normalized G-force magnitude: sqrt(x² + y² + z²) / 9.80665
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val gForce = magnitude / GRAVITY_EARTH

        // Filter out ordinary movement below threshold
        if (gForce < accelerationThresholdG) {
            // Check if existing sequence has expired
            if (shakeCount > 0 && (nowMs - firstShakeTimeMs > shakeWindowMs)) {
                reset()
            }
            return false
        }

        // Debounce: verify minimum time has elapsed since previous counted shake impulse
        if (shakeCount > 0 && (nowMs - lastShakeTimeMs < minShakeIntervalMs)) {
            return false // Debounced: part of the same physical movement
        }

        // Window expiration: if too much time passed since the first shake, reset window
        if (shakeCount > 0 && (nowMs - firstShakeTimeMs > shakeWindowMs)) {
            reset()
        }

        if (shakeCount == 0) {
            firstShakeTimeMs = nowMs
        }

        shakeCount++
        lastShakeTimeMs = nowMs

        if (shakeCount >= requiredShakeCount) {
            reset()
            lastTriggerTimeMs = nowMs
            onThreeShakesDetected()
            return true
        }

        return false
    }

    fun getShakeCountForTesting(): Int = shakeCount
}
