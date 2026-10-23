package com.samraksha.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samraksha.app.ui.components.status.SamrakshaPulsingDot
import com.samraksha.app.ui.theme.SamrakshaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern, safety-focused Splash Screen for SAMRAKSHA.
 *
 * Features:
 * - Layered pulsing safety radar rings
 * - Smooth entrance scale and fade animations
 * - Authoritative branding and safety tagline
 * - Encrypted Sentinel status badge
 * - Automatic transition to authentication/home screen after brief verification delay
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier,
    splashDurationMillis: Long = 2200L
) {
    // ── Entrance Animations ─────────────────────────────────
    val logoScale = remember { Animatable(0.72f) }
    val logoAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(18f) }
    val badgeAlpha = remember { Animatable(0f) }

    // ── Continuous Radar Pulse Ring Animation ───────────────
    val infiniteTransition = rememberInfiniteTransition(label = "splash_halo")
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    // ── Trigger Orchestrated Animations & Delayed Navigation ──
    LaunchedEffect(Unit) {
        // 1. Staggered logo pop-in
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // 2. Text elements fade in and slide up
        delay(250)
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }

        // 3. Security badge fades in
        delay(300)
        launch {
            badgeAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // 4. Hold briefly for branding perception, then transition
        delay(splashDurationMillis)
        onSplashComplete()
    }

    // Soft Sky Blue & Lavender background gradient
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDark = SamrakshaTheme.colors.onInfoContainer
    val softBlue = SamrakshaTheme.colors.softBlueBackground
    val softLavender = SamrakshaTheme.colors.softLavenderBackground
    val appBackground = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        softBlue,
                        softLavender.copy(alpha = 0.6f),
                        appBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashComplete // Tap to skip
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.large)
        ) {
            // ── Shield Icon with Layered Glowing Halo Rings ───
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp)
            ) {
                // Outer radiating safety radar wave
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(radarScale)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = radarAlpha))
                )

                // Static subtle border ring
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = primaryColor.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                )

                // Central Shield Container
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor,
                                    primaryDark
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "SAMRAKSHA Safety Shield",
                        tint = Color.White,
                        modifier = Modifier.size(62.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // ── App Name & Tagline ────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .padding(top = contentOffsetY.value.dp)
            ) {
                Text(
                    text = "SAMRAKSHA",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                Text(
                    text = "Your Trusted Personal Safety Guardian",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // ── Security Status Indicator ─────────────────────
            Row(
                modifier = Modifier
                    .alpha(badgeAlpha.value)
                    .clip(SamrakshaTheme.shapes.pill)
                    .background(SamrakshaTheme.colors.safeContainer)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SamrakshaPulsingDot(
                    color = SamrakshaTheme.colors.safe,
                    size = 8.dp,
                    isPulsing = true
                )
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SamrakshaTheme.colors.onSafeContainer,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.extraSmall))
                Text(
                    text = "Sentinel Engine Active",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = SamrakshaTheme.colors.onSafeContainer
                )
            }
        }

        // ── Subtle Bottom Encryption Watermark ────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = SamrakshaTheme.spacing.large)
                .alpha(badgeAlpha.value)
        ) {
            Text(
                text = "End-to-End Encrypted Telemetry",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
