package com.samraksha.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 shape system configured for SAMRAKSHA.
 * Features modern 16-22 dp corner radii for cards and gentle rounding throughout.
 */
val SamrakshaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/**
 * Specialized component shapes for the SAMRAKSHA design system.
 */
@Immutable
data class SamrakshaCustomShapes(
    val button: Shape = RoundedCornerShape(14.dp),
    val card: Shape = RoundedCornerShape(18.dp),
    val dialog: Shape = RoundedCornerShape(22.dp),
    val textField: Shape = RoundedCornerShape(14.dp),
    val pill: Shape = CircleShape,
    val avatar: Shape = RoundedCornerShape(14.dp),
    val chip: Shape = RoundedCornerShape(12.dp),
    val emergencyButton: Shape = CircleShape,
    val banner: Shape = RoundedCornerShape(16.dp),
    val bottomSheet: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
)

val LocalCustomShapes = staticCompositionLocalOf { SamrakshaCustomShapes() }
