package com.samraksha.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Root navigation wrapper for SAMRAKSHA.
 * Delegates to the central [AppNavHost].
 */
@Composable
fun SamrakshaNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    AppNavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    )
}
