package com.samraksha.app.ui.components.buttons

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samraksha.app.ui.theme.EmergencyRed40
import com.samraksha.app.ui.theme.EmergencyRed50
import com.samraksha.app.ui.theme.EmergencyRed80
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Standard primary button for key actions in SAMRAKSHA.
 * Supports loading indicator, leading/trailing icons, and full-width mode.
 */
@Composable
fun SamrakshaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SamrakshaTheme.shapes.button,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    Button(
        onClick = onClick,
        modifier = buttonModifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = SamrakshaTheme.spacing.large)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    trailingIcon()
                }
            }
        }
    }
}

/**
 * Secondary tonal button for supporting or alternate actions.
 */
@Composable
fun SamrakshaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SamrakshaTheme.shapes.button,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    Button(
        onClick = onClick,
        modifier = buttonModifier.height(52.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(horizontal = SamrakshaTheme.spacing.large)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Clean outlined button for neutral secondary choices (e.g., Cancel, View Map).
 */
@Composable
fun SamrakshaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SamrakshaTheme.shapes.button,
    fullWidth: Boolean = true
) {
    val buttonModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    OutlinedButton(
        onClick = onClick,
        modifier = buttonModifier.height(52.dp),
        enabled = enabled,
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = SamrakshaTheme.spacing.large)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * Text button for low-emphasis tertiary actions (e.g., Skip, Learn More).
 */
@Composable
fun SamrakshaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}

/**
 * Signature SOS Emergency button.
 * Designed with a high-visibility pulsing halo ring, high contrast crimson styling,
 * and bold emergency typography to enable instantaneous trigger during emergencies.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SamrakshaEmergencyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    title: String = "SOS",
    subtitle: String = "HOLD 3s",
    isPulsing: Boolean = true,
    enabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing && enabled) 1.22f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isPulsing && enabled) 0.08f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(size * 1.3f),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing safety halo ring
        if (isPulsing && enabled) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(EmergencyRed50.copy(alpha = pulseAlpha))
            )
            Box(
                modifier = Modifier
                    .size(size * 1.08f)
                    .clip(CircleShape)
                    .border(2.dp, EmergencyRed80.copy(alpha = 0.3f), CircleShape)
            )
        }

        // Main core emergency trigger circle
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (enabled) EmergencyRed40 else MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = {},
                    onLongClick = onClick
                )
                .border(
                    width = 4.dp,
                    color = if (enabled) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (enabled) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
