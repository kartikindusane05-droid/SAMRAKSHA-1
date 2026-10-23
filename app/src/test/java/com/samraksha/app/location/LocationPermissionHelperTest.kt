package com.samraksha.app.location

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationPermissionHelperTest {

    @Test
    fun requiredPermissions_containsFineAndCoarseLocation() {
        val expected = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        assertArrayEquals(expected, LocationPermissionHelper.REQUIRED_PERMISSIONS)
    }

    @Test
    fun rationaleStrings_areNonEmptyAndContainSafetyAndPrivacyContext() {
        assertTrue(LocationPermissionHelper.RATIONALE_TITLE.isNotBlank())
        assertTrue(LocationPermissionHelper.RATIONALE_MESSAGE.contains("safety", ignoreCase = true))
        assertTrue(LocationPermissionHelper.RATIONALE_PRIVACY_NOTE.contains("device", ignoreCase = true))
        assertTrue(LocationPermissionHelper.PERMANENTLY_DENIED_TITLE.isNotBlank())
        assertTrue(LocationPermissionHelper.PERMANENTLY_DENIED_MESSAGE.contains("settings", ignoreCase = true))
    }

    @Test
    fun locationPermissionStatus_enumExhaustiveCheck() {
        val statuses = LocationPermissionHelper.LocationPermissionStatus.values()
        assertEquals(3, statuses.size)
        assertTrue(statuses.contains(LocationPermissionHelper.LocationPermissionStatus.GRANTED_FINE))
        assertTrue(statuses.contains(LocationPermissionHelper.LocationPermissionStatus.GRANTED_COARSE_ONLY))
        assertTrue(statuses.contains(LocationPermissionHelper.LocationPermissionStatus.DENIED))
    }
}
