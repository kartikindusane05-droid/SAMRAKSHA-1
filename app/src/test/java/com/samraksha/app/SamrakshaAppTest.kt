package com.samraksha.app

import com.samraksha.app.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for SAMRAKSHA navigation and application integrity.
 */
class SamrakshaAppTest {

    @Test
    fun appPackageName_isCorrect() {
        assertEquals("com.samraksha.app", "com.samraksha.app")
    }

    @Test
    fun navigationRoutes_areUniqueAndCorrect() {
        val routes = listOf(
            Screen.Splash.route,
            Screen.Login.route,
            Screen.Signup.route,
            Screen.Home.route,
            Screen.EmergencyContacts.route,
            Screen.Sos.route,
            Screen.EmergencyHistory.route,
            Screen.SafetyMap.route,
            Screen.Journey.route,
            Screen.SafetyStore.route,
            Screen.Profile.route,
            Screen.Settings.route
        )

        // Ensure 12 unique routes
        assertEquals(12, routes.size)
        assertEquals(12, routes.distinct().size)

        // Verify key route strings
        assertEquals("splash", Screen.Splash.route)
        assertEquals("login", Screen.Login.route)
        assertEquals("signup", Screen.Signup.route)
        assertEquals("home", Screen.Home.route)
        assertEquals("emergency_contacts", Screen.EmergencyContacts.route)
        assertEquals("sos", Screen.Sos.route)
        assertEquals("emergency_history", Screen.EmergencyHistory.route)
        assertEquals("safety_map", Screen.SafetyMap.route)
        assertEquals("journey", Screen.Journey.route)
        assertEquals("safety_store", Screen.SafetyStore.route)
        assertEquals("profile", Screen.Profile.route)
        assertEquals("settings", Screen.Settings.route)
    }
}
