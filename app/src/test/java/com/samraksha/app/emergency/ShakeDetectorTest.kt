package com.samraksha.app.emergency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShakeDetectorTest {

    private var triggerCount = 0
    private lateinit var detector: ShakeDetector

    // Helper: 1.5G acceleration vector in m/s² (1.5 * 9.80665 ≈ 14.71 m/s²)
    private val qualifyingAccX = 14.71f
    private val qualifyingAccY = 0f
    private val qualifyingAccZ = 0f

    // Helper: 0.8G ordinary handling vector in m/s² (0.8 * 9.80665 ≈ 7.85 m/s²)
    private val lowAccX = 7.85f
    private val lowAccY = 0f
    private val lowAccZ = 0f

    @Before
    fun setUp() {
        triggerCount = 0
        detector = ShakeDetector(
            accelerationThresholdG = 1.45f,
            minShakeIntervalMs = 250L,
            shakeWindowMs = 2000L,
            cooldownMs = 3500L,
            requiredShakeCount = 3
        ) {
            triggerCount++
        }
    }

    @Test
    fun initialState_hasZeroShakesAndNotListening() {
        assertEquals(0, detector.getShakeCountForTesting())
        assertFalse(detector.isListening)
    }

    @Test
    fun startListening_withNullSensorManager_returnsFalseGracefullyWithoutCrashing() {
        val result = detector.startListening(null)
        assertFalse(result)
        assertFalse(detector.isListening)
    }

    @Test
    fun lowAcceleration_belowThreshold_isFilteredOut() {
        val triggered = detector.processAcceleration(lowAccX, lowAccY, lowAccZ, 1000L)
        assertFalse(triggered)
        assertEquals(0, detector.getShakeCountForTesting())
        assertEquals(0, triggerCount)
    }

    @Test
    fun singleQualifyingShake_incrementsCounter_doesNotTrigger() {
        val triggered = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        assertFalse(triggered)
        assertEquals(1, detector.getShakeCountForTesting())
        assertEquals(0, triggerCount)
    }

    @Test
    fun twoQualifyingShakes_withValidInterval_doesNotTrigger() {
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        val triggered = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1300L)

        assertFalse(triggered)
        assertEquals(2, detector.getShakeCountForTesting())
        assertEquals(0, triggerCount)
    }

    @Test
    fun threeQualifyingShakes_withinWindow_triggersCallbackExactlyOnce() {
        val s1 = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        val s2 = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1300L)
        val s3 = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1600L)

        assertFalse(s1)
        assertFalse(s2)
        assertTrue(s3)
        assertEquals(1, triggerCount)
        assertEquals(0, detector.getShakeCountForTesting()) // Reset after successful trigger
    }

    @Test
    fun debounce_shakesCloserThanMinInterval_areIgnored() {
        // First shake at 1000ms
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        assertEquals(1, detector.getShakeCountForTesting())

        // Rapid swingback at 1100ms (< 250ms debounce)
        val ignored = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1100L)
        assertFalse(ignored)
        assertEquals(1, detector.getShakeCountForTesting()) // Still 1

        // Valid second shake at 1300ms (>= 250ms)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1300L)
        assertEquals(2, detector.getShakeCountForTesting())

        // Rapid swingback at 1400ms (< 250ms debounce)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1400L)
        assertEquals(2, detector.getShakeCountForTesting())

        // Valid third shake at 1650ms
        val triggered = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1650L)
        assertTrue(triggered)
        assertEquals(1, triggerCount)
    }

    @Test
    fun windowExpiration_resetsCounterIfThreeShakesNotCompletedInTime() {
        // Shake 1 at 1000ms
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        assertEquals(1, detector.getShakeCountForTesting())

        // Shake 2 at 1300ms
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1300L)
        assertEquals(2, detector.getShakeCountForTesting())

        // Shake 3 arrives at 3200ms (> 2000ms window from first shake at 1000ms)
        // This causes window expiration and starts a new sequence with count 1
        val triggered = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 3200L)
        assertFalse(triggered)
        assertEquals(1, detector.getShakeCountForTesting())
        assertEquals(0, triggerCount)
    }

    @Test
    fun postTriggerCooldown_suppressesSubsequentTriggers() {
        // Complete 3 shakes
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1300L)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1600L)
        assertEquals(1, triggerCount)

        // Additional shakes during 3500ms cooldown (e.g. at 2000ms, 2500ms, 3000ms)
        val duringCooldown1 = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 2000L)
        val duringCooldown2 = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 2500L)
        val duringCooldown3 = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 3000L)

        assertFalse(duringCooldown1)
        assertFalse(duringCooldown2)
        assertFalse(duringCooldown3)
        assertEquals(1, triggerCount) // Still only 1 trigger

        // After cooldown expires (> 1600 + 3500 = 5100ms)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 5200L)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 5500L)
        val postCooldownTrigger = detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 5800L)

        assertTrue(postCooldownTrigger)
        assertEquals(2, triggerCount)
    }

    @Test
    fun reset_clearsTrackingState() {
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1300L)
        assertEquals(2, detector.getShakeCountForTesting())

        detector.reset()
        assertEquals(0, detector.getShakeCountForTesting())

        // Third shake after reset only counts as shake 1
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1600L)
        assertEquals(1, detector.getShakeCountForTesting())
        assertEquals(0, triggerCount)
    }

    @Test
    fun stopListening_resetsState() {
        detector.processAcceleration(qualifyingAccX, qualifyingAccY, qualifyingAccZ, 1000L)
        assertEquals(1, detector.getShakeCountForTesting())

        detector.stopListening()
        assertFalse(detector.isListening)
        assertEquals(0, detector.getShakeCountForTesting())
    }
}
