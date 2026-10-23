package com.samraksha.app.navigation

/**
 * Defines all navigation routes for SAMRAKSHA.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object Home : Screen("home")
    data object EmergencyContacts : Screen("emergency_contacts")
    data object Sos : Screen("sos")
    data object EmergencyHistory : Screen("emergency_history")
    data object SafetyMap : Screen("safety_map")
    data object Journey : Screen("journey")
    data object NightActivityScore : Screen("night_activity_score")
    data object SafetyStore : Screen("safety_store")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
}
