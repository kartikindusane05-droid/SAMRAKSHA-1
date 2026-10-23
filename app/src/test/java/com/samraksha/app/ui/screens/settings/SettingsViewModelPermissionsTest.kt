package com.samraksha.app.ui.screens.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelPermissionsTest {

    @Test
    fun settingsUiState_hasAnyLocationPermission_computesCorrectly() {
        // Neither fine nor coarse
        val defaultState = SettingsUiState()
        assertFalse(defaultState.hasAnyLocationPermission)
        assertFalse(defaultState.hasFineLocationPermission)
        assertFalse(defaultState.hasCoarseLocationPermission)

        // Coarse only (Approximate)
        val coarseOnlyState = SettingsUiState(hasCoarseLocationPermission = true)
        assertTrue(coarseOnlyState.hasAnyLocationPermission)
        assertFalse(coarseOnlyState.hasFineLocationPermission)
        assertTrue(coarseOnlyState.hasCoarseLocationPermission)

        // Fine only
        val fineOnlyState = SettingsUiState(hasFineLocationPermission = true)
        assertTrue(fineOnlyState.hasAnyLocationPermission)
        assertTrue(fineOnlyState.hasFineLocationPermission)
        assertFalse(fineOnlyState.hasCoarseLocationPermission)

        // Both fine and coarse
        val bothState = SettingsUiState(
            hasFineLocationPermission = true,
            hasCoarseLocationPermission = true
        )
        assertTrue(bothState.hasAnyLocationPermission)
        assertTrue(bothState.hasFineLocationPermission)
        assertTrue(bothState.hasCoarseLocationPermission)
    }

    @Test
    fun settingsUiState_notificationPermission_defaultsToFalse() {
        val defaultState = SettingsUiState()
        assertFalse(defaultState.hasNotificationPermission)

        val grantedState = SettingsUiState(hasNotificationPermission = true)
        assertTrue(grantedState.hasNotificationPermission)
    }
}
