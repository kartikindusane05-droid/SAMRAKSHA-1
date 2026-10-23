package com.samraksha.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimarySoftBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlueBackground,
    onPrimaryContainer = PrimaryDark,
    inversePrimary = Color(0xFFA5BCFA),
    secondary = SecondaryLavender,
    onSecondary = Color.White,
    secondaryContainer = SoftLavenderBackground,
    onSecondaryContainer = Color(0xFF47368D),
    tertiary = SafeStatusTeal,
    onTertiary = Color.White,
    tertiaryContainer = SafeContainer,
    onTertiaryContainer = OnSafeContainer,
    error = EmergencyCoralRed,
    onError = Color.White,
    errorContainer = EmergencyContainer,
    onErrorContainer = OnEmergencyContainer,
    background = AppBackground,
    onBackground = PrimaryText,
    surface = SurfaceCard,
    onSurface = PrimaryText,
    surfaceVariant = SoftBlueBackground,
    onSurfaceVariant = SecondaryText,
    surfaceContainer = SurfaceCard,
    outline = BorderDivider,
    outlineVariant = BorderDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90ACFF),
    onPrimary = Color(0xFF142C68),
    primaryContainer = Color(0xFF28458A),
    onPrimaryContainer = Color(0xFFDCE5FF),
    inversePrimary = PrimarySoftBlue,
    secondary = Color(0xFFCFBEFF),
    onSecondary = Color(0xFF2B1D62),
    secondaryContainer = Color(0xFF433282),
    onSecondaryContainer = Color(0xFFEADBFF),
    tertiary = Color(0xFF66CBB4),
    onTertiary = Color(0xFF00382D),
    tertiaryContainer = Color(0xFF104E40),
    onTertiaryContainer = Color(0xFFB0ECE0),
    error = Color(0xFFF27B79),
    onError = Color(0xFF5C0E0E),
    errorContainer = Color(0xFF7A1515),
    onErrorContainer = Color(0xFFFFDAD6),
    background = NeutralDarkBackground,
    onBackground = NeutralDarkTextPrimary,
    surface = NeutralDarkSurface,
    onSurface = NeutralDarkTextPrimary,
    surfaceVariant = NeutralDarkSurfaceVariant,
    onSurfaceVariant = NeutralDarkTextSecondary,
    surfaceContainer = NeutralDarkSurfaceContainer,
    outline = NeutralDarkOutline,
    outlineVariant = NeutralDarkOutlineVariant
)

/**
 * Access object for the SAMRAKSHA Design System.
 */
object SamrakshaTheme {
    val spacing: SamrakshaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val colors: SamrakshaCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSamrakshaColors.current

    val shapes: SamrakshaCustomShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomShapes.current
}

/**
 * Root theme composable for SAMRAKSHA.
 * Configures Material 3, custom safety colors, spacing, and shapes.
 */
@Composable
fun SamrakshaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Defaults to false to ensure consistent, elegant branding
    spacing: SamrakshaSpacing = SamrakshaSpacing(),
    customShapes: SamrakshaCustomShapes = SamrakshaCustomShapes(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalSamrakshaColors provides customColors,
        LocalCustomShapes provides customShapes
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SamrakshaTypography,
            shapes = SamrakshaShapes,
            content = content
        )
    }
}
