package com.samraksha.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized spacing system for the SAMRAKSHA design system.
 * Based on an 8-point grid with 4-point sub-grid micro spacing.
 */
@Immutable
data class SamrakshaSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val base: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp,
    val massive: Dp = 64.dp
)

val LocalSpacing = staticCompositionLocalOf { SamrakshaSpacing() }
