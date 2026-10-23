package com.samraksha.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.ui.screens.auth.LoginScreen
import com.samraksha.app.ui.screens.auth.SignupScreen
import com.samraksha.app.ui.screens.emergency.EmergencyContactsScreen
import com.samraksha.app.ui.screens.emergency.EmergencyHistoryScreen
import com.samraksha.app.ui.screens.emergency.SosScreen
import com.samraksha.app.ui.screens.home.HomeScreen
import com.samraksha.app.ui.screens.journey.JourneyScreen
import com.samraksha.app.ui.screens.journey.JourneyViewModel
import com.samraksha.app.ui.screens.journey.NightActivityScoreScreen
import com.samraksha.app.ui.screens.location.SafetyMapScreen
import com.samraksha.app.ui.screens.profile.ProfileScreen
import com.samraksha.app.ui.screens.settings.SettingsScreen
import com.samraksha.app.ui.screens.splash.SplashScreen
import com.samraksha.app.ui.screens.store.SafetyStoreScreen

/**
 * Central Navigation Host for SAMRAKSHA.
 * Manages transitions between all app destinations.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ── Splash ──────────────────────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Auth ────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Dashboard Home ──────────────────────────────────
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSos = {
                    navController.navigate(Screen.Sos.route)
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.EmergencyContacts.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.EmergencyHistory.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.SafetyMap.route)
                },
                onNavigateToJourney = {
                    navController.navigate(Screen.Journey.route)
                },
                onNavigateToStore = {
                    navController.navigate(Screen.SafetyStore.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // ── Emergency Features ──────────────────────────────
        composable(Screen.Sos.route) {
            SosScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.EmergencyContacts.route)
                }
            )
        }

        composable(Screen.EmergencyContacts.route) {
            EmergencyContactsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.EmergencyHistory.route) {
            EmergencyHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Location & Navigation ───────────────────────────
        composable(Screen.SafetyMap.route) {
            SafetyMapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Journey.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                backStackEntry
            }
            val journeyViewModel: JourneyViewModel = viewModel(
                parentEntry,
                factory = JourneyViewModel.Factory(
                    journeyRepository = SamrakshaApp.instance.journeyRepository,
                    locationClient = SamrakshaApp.instance.locationClient,
                    safetyZoneRepository = SamrakshaApp.instance.safetyZoneRepository,
                    routingRepository = SamrakshaApp.instance.routingRepository
                )
            )
            JourneyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMap = {
                    navController.navigate(Screen.SafetyMap.route)
                },
                onNavigateToNightActivityScore = {
                    navController.navigate(Screen.NightActivityScore.route)
                },
                viewModel = journeyViewModel
            )
        }

        composable(Screen.NightActivityScore.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                try {
                    navController.getBackStackEntry(Screen.Journey.route)
                } catch (_: Exception) {
                    backStackEntry
                }
            }
            val journeyViewModel: JourneyViewModel = viewModel(
                parentEntry,
                factory = JourneyViewModel.Factory(
                    journeyRepository = SamrakshaApp.instance.journeyRepository,
                    locationClient = SamrakshaApp.instance.locationClient,
                    safetyZoneRepository = SamrakshaApp.instance.safetyZoneRepository,
                    routingRepository = SamrakshaApp.instance.routingRepository
                )
            )
            NightActivityScoreScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onUseRoute = {
                    navController.popBackStack()
                },
                viewModel = journeyViewModel
            )
        }

        // ── Store & Utility ─────────────────────────────────
        composable(Screen.SafetyStore.route) {
            SafetyStoreScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── User Management & Config ────────────────────────
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.EmergencyContacts.route)
                },
                onNavigateToJourney = {
                    navController.navigate(Screen.Journey.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.EmergencyContacts.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
