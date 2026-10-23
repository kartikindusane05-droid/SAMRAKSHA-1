package com.samraksha.app.ui.components.status

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Standard safety level categorization for the app.
 */
enum class SafetyLevel {
    SAFE,
    WARNING,
    CRITICAL,
    INFO,
    INACTIVE
}

/**
 * Pulsing live beacon dot for active background tracking, GPS lock, or live SOS dispatch.
 */
@Composable
fun SamrakshaPulsingDot(
    modifier: Modifier = Modifier,
    color: Color = SamrakshaTheme.colors.safe,
    size: Dp = 10.dp,
    isPulsing: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 2.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isPulsing) 0f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier.size(size * 2.2f),
        contentAlignment = Alignment.Center
    ) {
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Compact pill badge indicating system/user status (e.g. "Safe", "Active Beacon", "SOS Triggered").
 */
@Composable
fun SamrakshaStatusPill(
    label: String,
    level: SafetyLevel,
    modifier: Modifier = Modifier,
    showPulsingDot: Boolean = true
) {
    val (bgColor, contentColor, dotColor) = when (level) {
        SafetyLevel.SAFE -> Triple(
            SamrakshaTheme.colors.safeContainer,
            SamrakshaTheme.colors.onSafeContainer,
            SamrakshaTheme.colors.safe
        )
        SafetyLevel.WARNING -> Triple(
            SamrakshaTheme.colors.warningContainer,
            SamrakshaTheme.colors.onWarningContainer,
            SamrakshaTheme.colors.warning
        )
        SafetyLevel.CRITICAL -> Triple(
            SamrakshaTheme.colors.emergencyContainer,
            SamrakshaTheme.colors.onEmergencyContainer,
            SamrakshaTheme.colors.emergency
        )
        SafetyLevel.INFO -> Triple(
            SamrakshaTheme.colors.infoContainer,
            SamrakshaTheme.colors.onInfoContainer,
            SamrakshaTheme.colors.info
        )
        SafetyLevel.INACTIVE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline
        )
    }

    Row(
        modifier = modifier
            .clip(SamrakshaTheme.shapes.pill)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showPulsingDot) {
            SamrakshaPulsingDot(
                color = dotColor,
                size = 8.dp,
                isPulsing = level == SafetyLevel.SAFE || level == SafetyLevel.CRITICAL
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = contentColor
        )
    }
}

/**
 * Full-width safety alert banner for warnings, broadcast alerts, or notifications.
 */
@Composable
fun SamrakshaAlertBanner(
    title: String,
    message: String,
    level: SafetyLevel,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val (containerColor, contentColor, icon) = when (level) {
        SafetyLevel.SAFE -> Triple(
            SamrakshaTheme.colors.safeContainer,
            SamrakshaTheme.colors.onSafeContainer,
            Icons.Default.CheckCircle
        )
        SafetyLevel.WARNING -> Triple(
            SamrakshaTheme.colors.warningContainer,
            SamrakshaTheme.colors.onWarningContainer,
            Icons.Default.Warning
        )
        SafetyLevel.CRITICAL -> Triple(
            SamrakshaTheme.colors.emergencyContainer,
            SamrakshaTheme.colors.onEmergencyContainer,
            Icons.Default.Error
        )
        SafetyLevel.INFO -> Triple(
            SamrakshaTheme.colors.infoContainer,
            SamrakshaTheme.colors.onInfoContainer,
            Icons.Default.Info
        )
        SafetyLevel.INACTIVE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Info
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(SamrakshaTheme.shapes.banner),
        color = containerColor,
        shape = SamrakshaTheme.shapes.banner
    ) {
        Row(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f)
                )

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.size(SamrakshaTheme.spacing.small))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        ),
                        modifier = Modifier.clickable { onAction() }
                    )
                }
            }

            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standard loading state component for all screens in SAMRAKSHA.
 */
@Composable
fun SamrakshaLoadingView(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Standard error state component with optional retry button for all screens in SAMRAKSHA.
 */
@Composable
fun SamrakshaErrorView(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    retryLabel: String = "Try Again",
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(SamrakshaTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = SamrakshaTheme.shapes.card,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SamrakshaTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (onRetry != null) {
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))
                    Button(
                        onClick = onRetry,
                        shape = SamrakshaTheme.shapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = retryLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * Standard empty state component with optional action for all screens in SAMRAKSHA.
 */
@Composable
fun SamrakshaEmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SamrakshaTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))
            Button(
                onClick = onAction,
                shape = SamrakshaTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

