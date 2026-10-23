package com.samraksha.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── SAMRAKSHA Modern Soft Sky Blue + Lavender Palette ─────────────
val PrimarySoftBlue = Color(0xFF6C8FF5)
val PrimaryDark = Color(0xFF4568C8)
val SecondaryLavender = Color(0xFFB7A7F7)
val SecondaryLavenderDark = Color(0xFF8B77DE)

// Backgrounds & Surfaces
val AppBackground = Color(0xFFF7F8FC)
val SurfaceCard = Color(0xFFFFFFFF)
val SoftBlueBackground = Color(0xFFEEF3FF)
val SoftLavenderBackground = Color(0xFFF2EEFF)

// Typography & Dividers
val PrimaryText = Color(0xFF20263A)
val SecondaryText = Color(0xFF778096)
val TertiaryText = Color(0xFFA0A7BA)
val BorderDivider = Color(0xFFE7EAF2)

// Semantic Safety Colors
val SafeStatusTeal = Color(0xFF46B8A0)
val SafeStatusTealDark = Color(0xFF288572)
val SafeContainer = Color(0xFFE6F7F4)
val OnSafeContainer = Color(0xFF1B6B5D)

val WarningAmber = Color(0xFFF2B84B)
val WarningAmberDark = Color(0xFFB57E17)
val WarningContainer = Color(0xFFFEF3E2)
val OnWarningContainer = Color(0xFF8A5B0A)

val EmergencyCoralRed = Color(0xFFEF5350)
val EmergencyCoralRedDark = Color(0xFFC62828)
val EmergencyContainer = Color(0xFFFDE8E8)
val OnEmergencyContainer = Color(0xFF7A1515)

// Info / Geofence Accent (Harmonized with Soft Sky Blue)
val InfoBlue = PrimarySoftBlue
val InfoContainer = SoftBlueBackground
val OnInfoContainer = PrimaryDark

// ── Backward-Compatibility Tokens (Replaced Green with Soft Teal & Harmonized Shades) ───
val SafetyGreen10 = OnSafeContainer
val SafetyGreen20 = SafeStatusTealDark
val SafetyGreen30 = Color(0xFF35A087)
val SafetyGreen40 = SafeStatusTeal
val SafetyGreen50 = Color(0xFF57C5AE)
val SafetyGreen80 = Color(0xFF9DE0D2)
val SafetyGreen90 = Color(0xFFCEF1EA)
val SafetyGreen95 = SafeContainer
val SafetyGreen99 = Color(0xFFF7FCFB)

val EmergencyRed10 = Color(0xFF410002)
val EmergencyRed20 = OnEmergencyContainer
val EmergencyRed30 = Color(0xFF93000A)
val EmergencyRed40 = EmergencyCoralRed
val EmergencyRed50 = Color(0xFFE53935)
val EmergencyRed80 = Color(0xFFEF9A9A)
val EmergencyRed90 = Color(0xFFFFCDD2)
val EmergencyRed95 = EmergencyContainer

val WarningAmber10 = Color(0xFF2E1600)
val WarningAmber20 = OnWarningContainer
val WarningAmber30 = Color(0xFF703D00)
val WarningAmber40 = WarningAmber
val WarningAmber50 = Color(0xFFE37400)
val WarningAmber80 = Color(0xFFFFB74D)
val WarningAmber90 = Color(0xFFFFE0B2)
val WarningAmber95 = WarningContainer

val InfoBlue10 = Color(0xFF101C38)
val InfoBlue20 = Color(0xFF1F356B)
val InfoBlue30 = Color(0xFF2E4D9D)
val InfoBlue40 = PrimarySoftBlue
val InfoBlue50 = PrimaryDark
val InfoBlue80 = Color(0xFFA5BCFA)
val InfoBlue90 = Color(0xFFD6E2FD)
val InfoBlue95 = SoftBlueBackground

// Neutrals - Light Theme (Clean, light, high legibility)
val NeutralLightBackground = AppBackground
val NeutralLightSurface = SurfaceCard
val NeutralLightSurfaceVariant = SoftBlueBackground
val NeutralLightSurfaceContainer = SoftLavenderBackground
val NeutralLightOutline = BorderDivider
val NeutralLightOutlineVariant = BorderDivider
val NeutralLightTextPrimary = PrimaryText
val NeutralLightTextSecondary = SecondaryText
val NeutralLightTextTertiary = TertiaryText

// Neutrals - Dark Theme (Sleek slate with soft blue undertones)
val NeutralDarkBackground = Color(0xFF12151D)
val NeutralDarkSurface = Color(0xFF191D28)
val NeutralDarkSurfaceVariant = Color(0xFF212635)
val NeutralDarkSurfaceContainer = Color(0xFF293043)
val NeutralDarkOutline = Color(0xFF333C52)
val NeutralDarkOutlineVariant = Color(0xFF272F42)
val NeutralDarkTextPrimary = Color(0xFFF2F4F8)
val NeutralDarkTextSecondary = Color(0xFFA4ADC0)
val NeutralDarkTextTertiary = Color(0xFF6C758A)

/**
 * Dedicated semantic safety colors extending standard Material 3 color schemes.
 */
@Immutable
data class SamrakshaCustomColors(
    val safe: Color,
    val onSafe: Color,
    val safeContainer: Color,
    val onSafeContainer: Color,
    val emergency: Color,
    val onEmergency: Color,
    val emergencyContainer: Color,
    val onEmergencyContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val softBlueBackground: Color = SoftBlueBackground,
    val softLavenderBackground: Color = SoftLavenderBackground,
    val lavender: Color = SecondaryLavender,
    val border: Color = BorderDivider
)

val LightCustomColors = SamrakshaCustomColors(
    safe = SafeStatusTeal,
    onSafe = Color.White,
    safeContainer = SafeContainer,
    onSafeContainer = OnSafeContainer,
    emergency = EmergencyCoralRed,
    onEmergency = Color.White,
    emergencyContainer = EmergencyContainer,
    onEmergencyContainer = OnEmergencyContainer,
    warning = WarningAmber,
    onWarning = Color.White,
    warningContainer = WarningContainer,
    onWarningContainer = OnWarningContainer,
    info = PrimarySoftBlue,
    onInfo = Color.White,
    infoContainer = SoftBlueBackground,
    onInfoContainer = PrimaryDark,
    softBlueBackground = SoftBlueBackground,
    softLavenderBackground = SoftLavenderBackground,
    lavender = SecondaryLavender,
    border = BorderDivider
)

val DarkCustomColors = SamrakshaCustomColors(
    safe = Color(0xFF66CBB4),
    onSafe = Color(0xFF00382D),
    safeContainer = Color(0xFF104E40),
    onSafeContainer = Color(0xFFB0ECE0),
    emergency = Color(0xFFF27B79),
    onEmergency = Color(0xFF5C0E0E),
    emergencyContainer = Color(0xFF7A1515),
    onEmergencyContainer = Color(0xFFFFDAD6),
    warning = Color(0xFFF7C873),
    onWarning = Color(0xFF452B00),
    warningContainer = Color(0xFF634000),
    onWarningContainer = Color(0xFFFFDF9E),
    info = Color(0xFF90ACFF),
    onInfo = Color(0xFF142C68),
    infoContainer = Color(0xFF28458A),
    onInfoContainer = Color(0xFFDCE5FF),
    softBlueBackground = Color(0xFF1D2436),
    softLavenderBackground = Color(0xFF241F36),
    lavender = Color(0xFFCFBEFF),
    border = Color(0xFF333C52)
)

val LocalSamrakshaColors = staticCompositionLocalOf { LightCustomColors }
