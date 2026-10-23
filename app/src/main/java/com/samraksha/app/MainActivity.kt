package com.samraksha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.samraksha.app.data.local.ThemeMode
import com.samraksha.app.navigation.AppNavHost
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Single-Activity host for the SAMRAKSHA application.
 *
 * All screens are rendered as Compose destinations inside [AppNavHost].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by SamrakshaApp.instance.settingsPreferences.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            val startDestination = intent?.getStringExtra("destination") ?: com.samraksha.app.navigation.Screen.Splash.route

            SamrakshaTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(startDestination = startDestination)
                }
            }
        }
    }
}
