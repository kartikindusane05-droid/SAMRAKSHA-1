package com.samraksha.app.ui.screens.journey

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.BuildConfig
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.model.ActivityFactorScore
import com.samraksha.app.domain.model.NightActivityScoreResult
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.domain.model.RouteOption
import com.samraksha.app.domain.model.ScoreConfidenceLevel
import com.samraksha.app.ui.components.buttons.SamrakshaOutlinedButton
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.components.buttons.SamrakshaSecondaryButton
import com.samraksha.app.ui.components.map.MapLibreComposeView
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.theme.SamrakshaTheme
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import kotlin.math.roundToInt

/**
 * Dedicated Compose screen for Night Activity Score and Route Comparison.
 *
 * Displays:
 * - Clear title and explanation of what the Night Activity Score measures.
 * - Origin and Destination summary preserved from Stage 1.
 * - Travel-mode selector (Drive, Bicycle, Walk).
 * - Real route comparison cards for genuine alternatives returned by Geoapify.
 * - Expandable breakdown of all 6 activity factors with earned/max points and explanations.
 * - Honest disclosures for confidence, data completeness, and single-route corridors.
 * - Responsible-use safety disclaimer.
 * - "Use This Route" action that synchronizes selected route back to Journey planning.
 */
@Composable
fun NightActivityScoreScreen(
    onNavigateBack: () -> Unit,
    onUseRoute: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JourneyViewModel = viewModel(
        factory = JourneyViewModel.Factory(
            journeyRepository = SamrakshaApp.instance.journeyRepository,
            locationClient = SamrakshaApp.instance.locationClient,
            safetyZoneRepository = SamrakshaApp.instance.safetyZoneRepository,
            routingRepository = SamrakshaApp.instance.routingRepository
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // Automatically trigger route calculation if options haven't loaded yet and destination is present
    LaunchedEffect(uiState.destinationInput) {
        if (uiState.routeOptions.isEmpty() && !uiState.isCalculatingRoute && uiState.destinationInput.isNotBlank()) {
            viewModel.previewRoute(uiState.destinationInput)
        }
    }

    val selectedOption = uiState.selectedRouteOption ?: uiState.routeOptions.firstOrNull { it.isSelected } ?: uiState.routeOptions.firstOrNull()

    ScreenScaffold(
        title = "Night Activity Score",
        onNavigateBack = onNavigateBack,
        actions = {
            if (uiState.destinationInput.isNotBlank() && !uiState.isCalculatingRoute) {
                IconButton(onClick = { viewModel.previewRoute(uiState.destinationInput) }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Routes & Scores",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { scaffoldModifier ->
        Box(modifier = scaffoldModifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SamrakshaTheme.spacing.base),
                contentPadding = PaddingValues(
                    top = SamrakshaTheme.spacing.base,
                    bottom = 96.dp // Extra spacing for fixed bottom button
                ),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.base)
            ) {
                // ── 1. Explanation Header Card ────────────────────────────────
                item {
                    ScoreExplanationHeaderCard()
                }

                // ── 2. Origin & Destination Corridor Card ──────────────────────
                item {
                    CorridorSummaryCard(
                        origin = uiState.originInput,
                        destination = uiState.destinationInput,
                        isCustomOrigin = uiState.isOriginCustom
                    )
                }

                // ── 3. Travel Mode Selector ───────────────────────────────────
                item {
                    NightTravelModeSelector(
                        selectedMode = uiState.travelMode,
                        onModeSelected = { viewModel.setTravelMode(it) }
                    )
                }

                // ── 4. Main Content: Loading, Error, Empty, or Comparison ─────
                when {
                    uiState.isCalculatingRoute -> {
                        item {
                            CalculatingRoutesCard()
                        }
                    }
                    uiState.routeError != null -> {
                        item {
                            RouteErrorCard(
                                errorMessage = uiState.routeError ?: "Unable to calculate route alternatives",
                                onRetry = { viewModel.previewRoute(uiState.destinationInput) }
                            )
                        }
                    }
                    uiState.destinationInput.isBlank() -> {
                        item {
                            NoDestinationCard(onGoToPlanner = onNavigateBack)
                        }
                    }
                    uiState.routeOptions.isEmpty() && uiState.routeInfo == null -> {
                        item {
                            NoRoutesFoundCard(
                                destination = uiState.destinationInput,
                                onRetry = { viewModel.previewRoute(uiState.destinationInput) }
                            )
                        }
                    }
                    else -> {
                        // ── 5. Single Route Limitation Notice (if applicable) ─
                        if (uiState.routeLimitationNotice != null || uiState.routeOptions.size == 1) {
                            item {
                                SingleRouteNoticeCard(
                                    notice = uiState.routeLimitationNotice
                                        ?: "Single direct route available for this transit corridor. Geoapify found no distinct alternative paths."
                                )
                            }
                        }

                        // ── 6. Route Comparison Cards Header ───────────────────
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.routeOptions.size > 1) "Route Comparison (${uiState.routeOptions.size} Alternatives)" else "Evaluated Corridor Route",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Select a route",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // ── 7. Route Comparison Cards ─────────────────────────
                        items(uiState.routeOptions.size) { index ->
                            val option = uiState.routeOptions[index]
                            val isSelected = option.id == selectedOption?.id
                            DedicatedRouteComparisonCard(
                                option = option,
                                isSelected = isSelected,
                                onClick = { viewModel.selectRouteOption(option.id) }
                            )
                        }

                        // ── 8. Map Visualization for Selected Route ────────────
                        val activeRouteInfo = selectedOption?.routeInfo ?: uiState.routeInfo
                        if (activeRouteInfo != null) {
                            item {
                                SelectedRouteMapCard(
                                    routeInfo = activeRouteInfo,
                                    originName = uiState.originInput,
                                    destinationName = uiState.destinationInput
                                )
                            }
                        }

                        // ── 9. Expandable Factor Breakdown for Selected Route ──
                        selectedOption?.let { selected ->
                            item {
                                DedicatedFactorBreakdownCard(
                                    scoreResult = selected.nightActivityScore,
                                    isExpanded = uiState.isFactorBreakdownExpanded,
                                    onToggleExpand = { viewModel.toggleFactorBreakdown() }
                                )
                            }
                        }

                        // ── 10. Responsible Score Transparency Disclaimer ─────
                        item {
                            ResponsibleUseDisclaimerCard()
                        }
                    }
                }
            }

            // ── Fixed Bottom "Use This Route" Action Bar ──────────────────────
            if (selectedOption != null && !uiState.isCalculatingRoute) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SamrakshaTheme.spacing.base,
                                vertical = SamrakshaTheme.spacing.small
                            )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedOption.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${selectedOption.routeInfo.formattedDistance} • ${selectedOption.routeInfo.formattedDuration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(selectedOption.nightActivityScore.activityLevel.colorHex).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(selectedOption.nightActivityScore.activityLevel.colorHex).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${selectedOption.nightActivityScore.totalScore}/100 • ${selectedOption.nightActivityScore.activityLevel.badgeLabel}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(selectedOption.nightActivityScore.activityLevel.colorHex),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        SamrakshaPrimaryButton(
                            text = "Use This Route",
                            onClick = onUseRoute,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ── Explanation Header Card ───────────────────────────────────────────────────

@Composable
private fun ScoreExplanationHeaderCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NightlightRound,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(SamrakshaTheme.spacing.small))
            Column {
                Text(
                    text = "What is Night Activity Score?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The score evaluates commercial venue presence, active public transit hubs, well-lit primary road coverage, and nearby verified emergency points along each route corridor to help you pick the best-supported route after dark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── Corridor Summary (Origin & Destination) Card ─────────────────────────────

@Composable
private fun CorridorSummaryCard(
    origin: String,
    destination: String,
    isCustomOrigin: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(SamrakshaTheme.spacing.base)) {
            Text(
                text = "Transit Corridor",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            // Origin Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(SamrakshaTheme.colors.safe.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SamrakshaTheme.colors.safe)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isCustomOrigin) "Origin (Custom Point)" else "Origin (Current GPS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = origin.ifBlank { "Current Location" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Divider vertical dash
            Box(
                modifier = Modifier
                    .padding(start = 9.dp, top = 2.dp, bottom = 2.dp)
                    .width(2.dp)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Destination Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Destination",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = destination.ifBlank { "Not Specified" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ── Travel Mode Selector ──────────────────────────────────────────────────────

@Composable
private fun NightTravelModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(SamrakshaTheme.spacing.base)) {
            Text(
                text = "Transit Mode",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    Triple("drive", "Drive", Icons.Default.DirectionsCar),
                    Triple("bicycle", "Bicycle", Icons.Default.DirectionsBike),
                    Triple("walk", "Walk", Icons.AutoMirrored.Filled.DirectionsWalk)
                )
                modes.forEach { (modeKey, label, icon) ->
                    val isSelected = selectedMode == modeKey
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onModeSelected(modeKey) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Dedicated Route Comparison Card ───────────────────────────────────────────

@Composable
private fun DedicatedRouteComparisonCard(
    option: RouteOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scoreResult = option.nightActivityScore
    val activityColor = Color(scoreResult.activityLevel.colorHex)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(SamrakshaTheme.spacing.base)) {
            // Header Row: Radio selection, Route label, Distance & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected route" else "Select route",
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Strategy: ${option.strategy.replace('_', ' ').replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${option.routeInfo.formattedDistance} • ${option.routeInfo.formattedDuration}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Score & Tiers Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DedicatedScoreProgressRing(
                        score = scoreResult.totalScore,
                        color = activityColor
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Night Activity Score",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${scoreResult.openPlacesCount} open venues along corridor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = activityColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, activityColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = scoreResult.activityLevel.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = activityColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (scoreResult.confidenceLevel) {
                            ScoreConfidenceLevel.HIGH -> SamrakshaTheme.colors.safe.copy(alpha = 0.12f)
                            ScoreConfidenceLevel.MEDIUM -> SamrakshaTheme.colors.warning.copy(alpha = 0.12f)
                            ScoreConfidenceLevel.LOW -> SamrakshaTheme.colors.info.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = scoreResult.confidenceLevel.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = when (scoreResult.confidenceLevel) {
                                ScoreConfidenceLevel.HIGH -> SamrakshaTheme.colors.safe
                                ScoreConfidenceLevel.MEDIUM -> SamrakshaTheme.colors.warning
                                ScoreConfidenceLevel.LOW -> SamrakshaTheme.colors.info
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Score Circular Progress Ring ──────────────────────────────────────────────

@Composable
private fun DedicatedScoreProgressRing(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 50.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.5.dp
        )
        CircularProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 4.5.dp
        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Selected Route Map Preview Card ───────────────────────────────────────────

@Composable
private fun SelectedRouteMapCard(
    routeInfo: RouteInfo,
    originName: String,
    destinationName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(SamrakshaTheme.spacing.base)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Selected Route Geometry",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${routeInfo.formattedDistance} • ${routeInfo.formattedDuration}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                val styleUrl = if (BuildConfig.GEOAPIFY_API_KEY.isNotBlank()) {
                    "https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
                } else {
                    "https://demotiles.maplibre.org/style.json"
                }

                MapLibreComposeView(
                    modifier = Modifier.fillMaxSize(),
                    styleUrl = styleUrl,
                    onMapReady = { map, _ ->
                        map.clear()
                        if (routeInfo.points.isNotEmpty()) {
                            map.addPolyline(
                                PolylineOptions()
                                    .addAll(routeInfo.points)
                                    .color(android.graphics.Color.parseColor("#2563EB"))
                                    .width(5.5f)
                            )
                        }
                        map.addMarker(
                            MarkerOptions()
                                .position(routeInfo.startPoint)
                                .title("Origin: $originName")
                        )
                        map.addMarker(
                            MarkerOptions()
                                .position(routeInfo.destinationPoint)
                                .title("Destination: $destinationName")
                        )

                        val builder = LatLngBounds.Builder()
                        builder.include(routeInfo.startPoint)
                        builder.include(routeInfo.destinationPoint)
                        routeInfo.points.forEach { builder.include(it) }
                        try {
                            val bounds = builder.build()
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))
                        } catch (_: Exception) {
                            try {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(routeInfo.startPoint, 13.0))
                            } catch (_: Exception) {}
                        }
                    }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "© OpenStreetMap contributors, Geoapify",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ── Dedicated Factor Breakdown Card ───────────────────────────────────────────

@Composable
private fun DedicatedFactorBreakdownCard(
    scoreResult: NightActivityScoreResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(SamrakshaTheme.spacing.base)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Score Factor Breakdown (6 Core Factors)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse" else "Tap to inspect all factor scores",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse factors" else "Expand factors",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .animateContentSize()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    scoreResult.factorScores.forEachIndexed { index, factorScore ->
                        if (index > 0) Spacer(Modifier.height(10.dp))
                        DedicatedFactorScoreRow(factorScore = factorScore)
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    // Confidence and Operating Hours Disclosure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (scoreResult.confidenceLevel) {
                                ScoreConfidenceLevel.HIGH -> SamrakshaTheme.colors.safe.copy(alpha = 0.15f)
                                ScoreConfidenceLevel.MEDIUM -> SamrakshaTheme.colors.warning.copy(alpha = 0.15f)
                                ScoreConfidenceLevel.LOW -> SamrakshaTheme.colors.info.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = scoreResult.confidenceLevel.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (scoreResult.confidenceLevel) {
                                    ScoreConfidenceLevel.HIGH -> SamrakshaTheme.colors.safe
                                    ScoreConfidenceLevel.MEDIUM -> SamrakshaTheme.colors.warning
                                    ScoreConfidenceLevel.LOW -> SamrakshaTheme.colors.info
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = scoreResult.confidenceDisclosure,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DedicatedFactorScoreRow(
    factorScore: ActivityFactorScore,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = factorScore.factor.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = factorScore.factor.description,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${factorScore.earnedPoints.roundToInt()} / ${factorScore.maxPoints.roundToInt()} pts",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { (factorScore.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = factorScore.detail,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── State Feedback Cards ──────────────────────────────────────────────────────

@Composable
private fun CalculatingRoutesCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SamrakshaTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Calculating Route Alternatives & Night Activity Scores...",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Querying Geoapify routing strategies, open late-night commercial venues, verified emergency help points, and corridor road hierarchy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RouteErrorCard(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
    ) {
        Column(modifier = Modifier.padding(SamrakshaTheme.spacing.base)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Route Calculation Notice",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            SamrakshaOutlinedButton(
                text = "Retry Route & Score Calculation",
                onClick = onRetry,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NoDestinationCard(
    onGoToPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No Destination Selected",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Please enter a destination in the Journey Planner using the autocomplete search bar to compare route night activity scores.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            SamrakshaPrimaryButton(
                text = "Return to Journey Planner",
                onClick = onGoToPlanner,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NoRoutesFoundCard(
    destination: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AltRoute,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No Routes Found",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Could not calculate viable routes to '$destination'. Check network connectivity or specify more precise coordinates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            SamrakshaOutlinedButton(
                text = "Calculate Routes Again",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SingleRouteNoticeCard(
    notice: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ResponsibleUseDisclaimerCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Responsible-Use Safety Disclaimer",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The Night Activity Score measures available activity, transit infrastructure, and public venue schedules. It is not an absolute predictor of personal safety or guarantee of freedom from crime. Always remain attentive, avoid poorly lit corridors, and activate the manual 3-second SOS if you encounter an emergency.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
