package com.samraksha.app.ui.components.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Standard content container card with subtle border, 18dp rounded corners, and pure white surface.
 */
@Composable
fun SamrakshaCard(
    modifier: Modifier = Modifier,
    shape: Shape = SamrakshaTheme.shapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base),
            content = content
        )
    }
}

/**
 * Elevated card with soft drop shadow for floating content.
 */
@Composable
fun SamrakshaElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = SamrakshaTheme.shapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base),
            content = content
        )
    }
}

/**
 * Safety status card with left colored accent bar, status icon, title, description,
 * and custom action content slot.
 */
@Composable
fun SamrakshaStatusCard(
    title: String,
    description: String,
    level: SafetyLevel,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingAction: @Composable (() -> Unit)? = null
) {
    val accentColor = when (level) {
        SafetyLevel.SAFE -> SamrakshaTheme.colors.safe
        SafetyLevel.WARNING -> SamrakshaTheme.colors.warning
        SafetyLevel.CRITICAL -> SamrakshaTheme.colors.emergency
        SafetyLevel.INFO -> SamrakshaTheme.colors.info
        SafetyLevel.INACTIVE -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left status accent vertical bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(SamrakshaTheme.spacing.base),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.base))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (trailingAction != null) {
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    trailingAction()
                }
            }
        }
    }
}

/**
 * Clickable row card with a pastel icon container, title, subtitle, and right navigation chevron.
 */
@Composable
fun SamrakshaActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    iconBackground: Color = MaterialTheme.colorScheme.primaryContainer
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(SamrakshaTheme.shapes.card)
            .clickable(onClick = onClick),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SamrakshaTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.base))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Premium banner card with subtle pastel gradient, soft border, and clean typography.
 */
@Composable
fun SamrakshaGradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = Brush.linearGradient(
        colors = listOf(
            SamrakshaTheme.colors.softBlueBackground,
            SamrakshaTheme.colors.softLavenderBackground
        )
    ),
    shape: Shape = SamrakshaTheme.shapes.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(SamrakshaTheme.spacing.base)
        ) {
            Column(content = content)
        }
    }
}
