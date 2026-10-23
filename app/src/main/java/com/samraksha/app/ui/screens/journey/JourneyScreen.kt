package com.samraksha.app.ui.screens.journey

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Security
import com.samraksha.app.domain.model.PlaceSuggestion
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import com.samraksha.app.domain.model.ActivityFactorScore
import com.samraksha.app.domain.model.ActivityFactorType
import com.samraksha.app.domain.model.ActivityLevel
import com.samraksha.app.domain.model.NightActivityScoreResult
import com.samraksha.app.domain.model.RouteOption
import com.samraksha.app.domain.model.ScoreConfidenceLevel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.domain.safety.JourneyRiskLevel
import com.samraksha.app.domain.safety.JourneySafetyReport
import com.samraksha.app.domain.safety.JourneySafetyWarning
import com.samraksha.app.domain.safety.NearbySafetyZone
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.notifications.NotificationPermissionHelper
import com.samraksha.app.ui.components.buttons.SamrakshaOutlinedButton
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.components.buttons.SamrakshaSecondaryButton
import com.samraksha.app.ui.components.cards.SamrakshaCard
import com.samraksha.app.BuildConfig
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.ui.components.map.MapLibreComposeView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import com.samraksha.app.ui.components.dialogs.LocationPermissionRationaleDialog
import com.samraksha.app.ui.components.dialogs.NotificationPermissionRationaleDialog
import com.samraksha.app.ui.components.inputs.SamrakshaTextField
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.components.status.SamrakshaAlertBanner
import com.samraksha.app.ui.components.status.SamrakshaPulsingDot
import com.samraksha.app.ui.components.status.SamrakshaStatusPill
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.screens.location.model.SafetyZone
import com.samraksha.app.ui.screens.location.model.SafetyZoneLevel
import com.samraksha.app.ui.theme.SamrakshaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun JourneyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToNightActivityScore: () -> Unit = {},
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
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(uiState.routeInfo, uiState.routeOptions, uiState.isCalculatingRoute, uiState.routeError) {
        if (uiState.routeInfo != null || uiState.routeOptions.isNotEmpty() || uiState.isCalculatingRoute || uiState.routeError != null) {
            try {
                listState.animateScrollToItem(index = 3)
            } catch (_: Exception) {}
        }
    }

    var showLocationRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (LocationPermissionHelper.hasLocationPermission(context)) {
                    viewModel.fetchCurrentLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Notifications enhance situational awareness but lack of permission should not block transit
        viewModel.startJourney()
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.fetchCurrentLocation()
            if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                showNotificationRationale = true
            } else {
                viewModel.startJourney()
            }
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    ScreenScaffold(
        title = "Journey Safety Sentinel",
        onNavigateBack = onNavigateBack,
        actions = {
            IconButton(onClick = {
                if (!LocationPermissionHelper.hasLocationPermission(context)) {
                    showLocationRationale = true
                } else {
                    viewModel.fetchCurrentLocation()
                }
            }) {
                if (uiState.isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS Location",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SamrakshaTheme.spacing.base),
                contentPadding = PaddingValues(vertical = SamrakshaTheme.spacing.base),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.base)
            ) {
                // ── 1. Top Metrics & Sentinel Overview Strip ──
                item {
                    JourneyMetricsRow(
                        hasActiveJourney = uiState.activeJourney != null,
                        completedCount = uiState.completedJourneysCount,
                        riskLevel = uiState.currentRiskLevel,
                        nearbyZonesCount = uiState.nearbySafetyZones.size
                    )
                }

                // ── 2. Active Situational Safety Warnings (when appropriate) ──
                if (uiState.activeJourney != null && uiState.activeWarnings.isNotEmpty()) {
                    items(uiState.activeWarnings, key = { it.id }) { warning ->
                        SamrakshaAlertBanner(
                            title = warning.title,
                            message = warning.message,
                            level = warning.riskLevel.safetyLevel,
                            onDismiss = { viewModel.dismissWarning(warning.id) },
                            actionLabel = "Send 'I'm Safe'",
                            onAction = { viewModel.performSafetyCheckIn() }
                        )
                    }
                }

                // ── 3. Current GPS Location Status Card ──
                item {
                    CurrentLocationCard(
                        location = uiState.currentLocation,
                        isLocating = uiState.isLocating,
                        errorMessage = uiState.locationError,
                        lastUpdatedEpoch = uiState.lastLocationUpdateEpoch,
                        isBatterySaver = uiState.isBatterySaverEnabled,
                        onRefresh = {
                            if (!LocationPermissionHelper.hasLocationPermission(context)) {
                                showLocationRationale = true
                            } else {
                                viewModel.fetchCurrentLocation()
                            }
                        }
                    )
                }

                // ── 4. Active Journey Card OR Start Journey Planner ──
                item {
                    if (uiState.activeJourney != null) {
                        ActiveJourneyCard(
                            journey = uiState.activeJourney!!,
                            currentLocation = uiState.currentLocation,
                            elapsedTime = uiState.elapsedTimeFormatted,
                            isCheckInSuccess = uiState.isCheckInSuccess,
                            onCheckIn = { viewModel.performSafetyCheckIn() },
                            onEndJourneyClick = { viewModel.setShowEndConfirmationDialog(true) },
                            onCancelJourneyClick = { viewModel.cancelJourney() }
                        )
                    } else {
                        StartJourneyPlannerCard(
                            currentLocation = uiState.currentLocation,
                            origin = uiState.originInput,
                            isOriginCustom = uiState.isOriginCustom,
                            destination = uiState.destinationInput,
                            activeAutocompleteField = uiState.activeAutocompleteField,
                            autocompleteState = uiState.autocompleteState,
                            selectedCityFilter = uiState.selectedCityFilter,
                            isLocationPermissionGranted = uiState.isLocationPermissionGranted,
                            isLoading = uiState.isLoading,
                            onOriginChange = { viewModel.onOriginChange(it) },
                            onDestinationChange = { viewModel.onDestinationChange(it) },
                            onOriginFocus = { viewModel.onFocusField(AutocompleteField.ORIGIN) },
                            onDestinationFocus = { viewModel.onFocusField(AutocompleteField.DESTINATION) },
                            onSelectOriginSuggestion = { viewModel.selectOriginSuggestion(it) },
                            onSelectDestinationSuggestion = { viewModel.selectDestinationSuggestion(it) },
                            onClearOrigin = { viewModel.clearOrigin() },
                            onResetOrigin = { viewModel.resetOriginToCurrentLocation() },
                            onClearDestination = { viewModel.clearDestination() },
                            onCityFilterChange = { viewModel.setCityFilter(it) },
                            onRetrySearch = { viewModel.retryAutocompleteSearch() },
                            onDismissAutocomplete = { viewModel.dismissAutocomplete() },
                            onQuickSelect = { viewModel.onQuickDestinationSelect(it) },
                            onPreviewRoute = { viewModel.previewRoute(uiState.destinationInput) },
                            onNavigateToNightActivityScore = onNavigateToNightActivityScore,
                            onStartJourney = {
                                if (!LocationPermissionHelper.hasLocationPermission(context)) {
                                    showLocationRationale = true
                                } else if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
                                    showNotificationRationale = true
                                } else {
                                    viewModel.startJourney()
                                }
                            }
                        )
                    }
                }

                // ── 4b. Geoapify Safe Transit Route & Night Activity Score Comparison ──
                if (uiState.activeJourney != null || uiState.routeInfo != null || uiState.routeOptions.isNotEmpty() || uiState.isCalculatingRoute || uiState.routeError != null) {
                    item {
                        JourneyRouteMapCard(
                            routeInfo = uiState.routeInfo,
                            routeOptions = uiState.routeOptions,
                            selectedRouteOption = uiState.selectedRouteOption,
                            travelMode = uiState.travelMode,
                            routeLimitationNotice = uiState.routeLimitationNotice,
                            isFactorBreakdownExpanded = uiState.isFactorBreakdownExpanded,
                            isCalculatingRoute = uiState.isCalculatingRoute,
                            routeError = uiState.routeError,
                            startLocationName = uiState.activeJourney?.startLocationName ?: "Current Location",
                            destinationName = uiState.activeJourney?.endLocationName ?: uiState.destinationInput,
                            onSelectRouteOption = { viewModel.selectRouteOption(it) },
                            onTravelModeChange = { viewModel.setTravelMode(it) },
                            onToggleFactorBreakdown = { viewModel.toggleFactorBreakdown() },
                            onOpenFullMap = onNavigateToMap,
                            onNavigateToNightActivityScore = onNavigateToNightActivityScore,
                            onRetryRoute = {
                                val active = uiState.activeJourney
                                if (active != null) {
                                    viewModel.fetchRoute(
                                        startLat = active.startLatitude.takeIf { it != 0.0 } ?: uiState.currentLocation?.latitude ?: 0.0,
                                        startLng = active.startLongitude.takeIf { it != 0.0 } ?: uiState.currentLocation?.longitude ?: 0.0,
                                        destination = active.endLocationName,
                                        mode = uiState.travelMode
                                    )
                                } else if (uiState.destinationInput.isNotBlank()) {
                                    viewModel.previewRoute(uiState.destinationInput)
                                }
                            }
                        )
                    }
                }

                // ── 5. Active Journey Safety Monitoring: Status Bar + Risk Level Card ──
                if (uiState.activeJourney != null) {
                    // 5a. Compact real-time monitoring status strip
                    item {
                        SafetyMonitoringStatusBar(
                            isSafetyMonitoringActive = uiState.isSafetyMonitoringActive,
                            lastUpdatedEpoch = uiState.lastLocationUpdateEpoch,
                            isBatterySaver = uiState.isBatterySaverEnabled,
                            currentRiskLevel = uiState.currentRiskLevel
                        )
                    }

                    // 5b. Risk level card
                    item {
                        ActiveRiskLevelCard(
                            riskLevel = uiState.currentRiskLevel,
                            safetyReport = uiState.safetyReport
                        )
                    }

                    // ── 6. Nearby Safety Zones Radar List ──
                    item {
                        NearbySafetyZonesCard(
                            nearbyZones = uiState.nearbySafetyZones
                        )
                    }

                    // ── 7. Battery-Conscious Sentinel Controls ──
                    item {
                        BatteryConsciousSentinelCard(
                            isBatterySaver = uiState.isBatterySaverEnabled,
                            onToggleBatterySaver = { viewModel.toggleBatterySaverMode(it) }
                        )
                    }
                }

                // ── 8. Sentinel Guarantee Card ──
                item {
                    SentinelSafetyAssuranceCard()
                }

                // ── 9. Recent Journeys History Header & List ──
                item {
                    Text(
                        text = "Journey History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = SamrakshaTheme.spacing.small)
                    )
                }

                if (uiState.recentJourneys.isEmpty()) {
                    item {
                        EmptyJourneyHistoryCard()
                    }
                } else {
                    items(uiState.recentJourneys, key = { it.id }) { journey ->
                        JourneyHistoryItem(journey = journey)
                    }
                }
            }

            // ── Floating Snackbar Host ──
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(SamrakshaTheme.spacing.base)
            )

            // ── Confirmation Dialog for Ending Journey ──
            if (uiState.showEndConfirmationDialog) {
                EndJourneyConfirmationDialog(
                    onConfirm = { viewModel.endJourney() },
                    onDismiss = { viewModel.setShowEndConfirmationDialog(false) }
                )
            }

            // ── Contextual Pre-Permission Rationale Dialogs ──
            val activity = context as? Activity

            if (showLocationRationale) {
                val isLocationPermanentlyDenied = activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)

                LocationPermissionRationaleDialog(
                    isPermanentlyDenied = isLocationPermanentlyDenied,
                    onGrant = {
                        showLocationRationale = false
                        LocationPermissionHelper.markPermissionRequested(context)
                        locationLauncher.launch(LocationPermissionHelper.REQUIRED_PERMISSIONS)
                    },
                    onDismiss = { showLocationRationale = false },
                    onOpenSettings = {
                        showLocationRationale = false
                        LocationPermissionHelper.openAppSettings(context)
                    }
                )
            }

            if (showNotificationRationale) {
                val isNotifPermanentlyDenied = activity != null && NotificationPermissionHelper.isPermanentlyDenied(activity)

                NotificationPermissionRationaleDialog(
                    isPermanentlyDenied = isNotifPermanentlyDenied,
                    onGrant = {
                        showNotificationRationale = false
                        val perm = NotificationPermissionHelper.PERMISSION
                        if (perm != null) {
                            NotificationPermissionHelper.markPermissionRequested(context)
                            notificationLauncher.launch(perm)
                        } else {
                            viewModel.startJourney()
                        }
                    },
                    onDismiss = {
                        showNotificationRationale = false
                        viewModel.startJourney()
                    },
                    onOpenSettings = {
                        showNotificationRationale = false
                        NotificationPermissionHelper.openNotificationSettings(context)
                    }
                )
            }
        }
    }
}

// ── Real-time Safety Monitoring Status Bar ─────────────────────────────────

@Composable
private fun SafetyMonitoringStatusBar(
    isSafetyMonitoringActive: Boolean,
    lastUpdatedEpoch: Long,
    isBatterySaver: Boolean,
    currentRiskLevel: JourneyRiskLevel,
    modifier: Modifier = Modifier
) {
    val riskColor = when (currentRiskLevel) {
        JourneyRiskLevel.SAFE -> SamrakshaTheme.colors.safe
        JourneyRiskLevel.MODERATE -> SamrakshaTheme.colors.warning
        JourneyRiskLevel.HIGH -> SamrakshaTheme.colors.emergency
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = riskColor.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 600),
        label = "risk_border_color"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, animatedBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: GPS pulse + status text
            Row(verticalAlignment = Alignment.CenterVertically) {
                SamrakshaPulsingDot(
                    color = if (isSafetyMonitoringActive) riskColor else MaterialTheme.colorScheme.outline,
                    size = 7.dp,
                    isPulsing = isSafetyMonitoringActive
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    AnimatedContent(
                        targetState = if (isSafetyMonitoringActive) "GPS Active" else "Acquiring fix…",
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "gps_status_label"
                    ) { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSafetyMonitoringActive) riskColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (lastUpdatedEpoch > 0L) formatSecondsAgo(lastUpdatedEpoch)
                               else "Awaiting first fix",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: battery mode chip + interval badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = (if (isBatterySaver) SamrakshaTheme.colors.safe else SamrakshaTheme.colors.info).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isBatterySaver) "🔋 Ultra-Saver" else "📶 Standard",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isBatterySaver) SamrakshaTheme.colors.safe else SamrakshaTheme.colors.info,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isBatterySaver) "30s" else "15s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ── Dashboard Metrics Row ─────────────────────────────────────────────────────

@Composable
private fun JourneyMetricsRow(
    hasActiveJourney: Boolean,
    completedCount: Int,
    riskLevel: JourneyRiskLevel = JourneyRiskLevel.SAFE,
    nearbyZonesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val riskColor = when (riskLevel) {
        JourneyRiskLevel.SAFE -> SamrakshaTheme.colors.safe
        JourneyRiskLevel.MODERATE -> SamrakshaTheme.colors.warning
        JourneyRiskLevel.HIGH -> SamrakshaTheme.colors.emergency
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
    ) {
        MetricTile(
            title = "Status",
            value = if (hasActiveJourney) "MONITORING" else "STANDBY",
            icon = Icons.Default.NearMe,
            highlightColor = if (hasActiveJourney) SamrakshaTheme.colors.safe else SamrakshaTheme.colors.info,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "Risk Level",
            value = if (hasActiveJourney) riskLevel.badgeLabel else "SECURED",
            icon = if (riskLevel == JourneyRiskLevel.SAFE) Icons.Default.Shield else Icons.Default.Warning,
            highlightColor = if (hasActiveJourney) riskColor else SamrakshaTheme.colors.safe,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = if (hasActiveJourney) "Nearby Zones" else "Completed",
            value = if (hasActiveJourney) "$nearbyZonesCount Detected" else "$completedCount Safe",
            icon = if (hasActiveJourney) Icons.Default.LocationOn else Icons.Default.CheckCircle,
            highlightColor = SamrakshaTheme.colors.safe,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.small),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = highlightColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = highlightColor,
                maxLines = 1
            )
        }
    }
}

// ── Current GPS Location Card ─────────────────────────────────────────────────

@Composable
private fun CurrentLocationCard(
    location: UserLocation?,
    isLocating: Boolean,
    errorMessage: String?,
    lastUpdatedEpoch: Long = 0L,
    isBatterySaver: Boolean = false,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SamrakshaTheme.colors.info.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SamrakshaTheme.colors.info,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Column {
                        Text(
                            text = "Current GPS Location",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val statusText = if (isLocating) {
                            "Acquiring fresh coordinates..."
                        } else if (lastUpdatedEpoch > 0L) {
                            "Periodic fix (${if (isBatterySaver) "30s" else "15s"} interval) • ${formatSecondsAgo(lastUpdatedEpoch)}"
                        } else {
                            "Live satellite fix"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SamrakshaTheme.colors.info
                    )
                } else {
                    SamrakshaPulsingDot(
                        color = if (errorMessage == null) SamrakshaTheme.colors.safe else SamrakshaTheme.colors.emergency,
                        size = 8.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(SamrakshaTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable { onRefresh() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            } else if (location != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.US, "%.5f° N, %.5f° E", location.latitude, location.longitude),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (location.accuracy > 0f) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "±${location.accuracy.toInt()}m accuracy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Requesting initial location lock...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Active Journey Live Sentinel Card ─────────────────────────────────────────

@Composable
private fun ActiveJourneyCard(
    journey: JourneyEntity,
    currentLocation: UserLocation?,
    elapsedTime: String,
    isCheckInSuccess: Boolean,
    onCheckIn: () -> Unit,
    onEndJourneyClick: () -> Unit,
    onCancelJourneyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(2.dp, SamrakshaTheme.colors.safe)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base)
        ) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SamrakshaStatusPill(
                    label = "LIVE SENTINEL ACTIVE",
                    level = SafetyLevel.SAFE,
                    showPulsingDot = true
                )

                Surface(
                    shape = SamrakshaTheme.shapes.chip,
                    color = SamrakshaTheme.colors.safe.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SamrakshaTheme.colors.safe,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Guardians Notified",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SamrakshaTheme.colors.safe
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

            // Large Monospace Elapsed Timer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(SamrakshaTheme.spacing.base),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live Journey Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

            // Origin -> Destination Timeline
            Column(modifier = Modifier.fillMaxWidth()) {
                // Origin
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SamrakshaTheme.colors.safe.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SamrakshaTheme.colors.safe)
                        )
                    }
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Column {
                        Text(
                            text = "ORIGIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = journey.startLocationName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Connector line
                Box(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .width(2.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Destination
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SamrakshaTheme.colors.emergency.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = SamrakshaTheme.colors.emergency,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Column {
                        Text(
                            text = "DESTINATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = journey.endLocationName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // Quick Safety Check-in Pill
            if (isCheckInSuccess) {
                Surface(
                    shape = SamrakshaTheme.shapes.chip,
                    color = SamrakshaTheme.colors.safe.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(SamrakshaTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SamrakshaTheme.colors.safe,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                        Text(
                            text = "Safe Check-in sent to guardians!",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SamrakshaTheme.colors.safe
                        )
                    }
                }
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
            }

            // Action Buttons
            SamrakshaOutlinedButton(
                text = "Send 'I'm Safe' Check-in",
                onClick = onCheckIn,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            SamrakshaPrimaryButton(
                text = "Arrived Safely • End Journey",
                onClick = onEndJourneyClick,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onCancelJourneyClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Cancel Monitoring",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Active Risk Level & Safety Assessment Card ────────────────────────────────

@Composable
private fun ActiveRiskLevelCard(
    riskLevel: JourneyRiskLevel,
    safetyReport: JourneySafetyReport?,
    modifier: Modifier = Modifier
) {
    val (themeColor, icon) = when (riskLevel) {
        JourneyRiskLevel.SAFE -> Pair(SamrakshaTheme.colors.safe, Icons.Default.Shield)
        JourneyRiskLevel.MODERATE -> Pair(SamrakshaTheme.colors.warning, Icons.Default.Warning)
        JourneyRiskLevel.HIGH -> Pair(SamrakshaTheme.colors.emergency, Icons.Default.Error)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, themeColor)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base)
        ) {
            // Header Row: Risk Level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(themeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Column {
                        Text(
                            text = "Current Risk Level",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = riskLevel.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = themeColor
                        )
                    }
                }

                SamrakshaStatusPill(
                    label = riskLevel.badgeLabel,
                    level = riskLevel.safetyLevel,
                    showPulsingDot = true
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            // Risk Description
            Text(
                text = riskLevel.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

            // Safety Score Progress Bar
            val score = safetyReport?.safetyScore ?: 90
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transit Safety Index",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$score / 100",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = themeColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = themeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Current Zone Info Block if located inside a recognized zone
            safetyReport?.currentZone?.let { currentZone ->
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(SamrakshaTheme.spacing.small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inside: ${currentZone.name}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentZone.level.badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = themeColor
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentZone.advisory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalPolice,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentZone.activePatrols} Active Patrols",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentZone.cctvCoveragePercentage}% CCTV",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } ?: run {
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                Text(
                    text = "Route Sector: General Transit Roadway (clear of registered hazard zones)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Nearby Safety Zones Radar Card ────────────────────────────────────────────

@Composable
private fun NearbySafetyZonesCard(
    nearbyZones: List<NearbySafetyZone>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Text(
                        text = "Nearby Safety Zones",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${nearbyZones.size} Detected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            if (nearbyZones.isEmpty()) {
                Text(
                    text = "No recorded safety zones within 1.5 km of current GPS coordinates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = SamrakshaTheme.spacing.small)
                )
            } else {
                nearbyZones.take(4).forEachIndexed { index, nearby ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                    }
                    NearbyZoneItem(item = nearby)
                }
            }
        }
    }
}

@Composable
private fun NearbyZoneItem(item: NearbySafetyZone) {
    val zone = item.zone
    val levelColor = when (zone.level) {
        SafetyZoneLevel.SAFE -> SamrakshaTheme.colors.safe
        SafetyZoneLevel.MODERATE_RISK -> SamrakshaTheme.colors.warning
        SafetyZoneLevel.UNSAFE -> SamrakshaTheme.colors.emergency
        SafetyZoneLevel.UNKNOWN -> Color(0xFF757575)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = levelColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (item.isInside) "INSIDE" else "${item.distanceMeters}m away",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = zone.advisory,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

// ── Battery-Conscious Sentinel Controls Card ──────────────────────────────────

@Composable
private fun BatteryConsciousSentinelCard(
    isBatterySaver: Boolean,
    onToggleBatterySaver: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SamrakshaTheme.colors.safe.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = SamrakshaTheme.colors.safe,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                    Column {
                        Text(
                            text = "Battery-Conscious Tracking",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBatterySaver) "Ultra-Saver Mode Active (30s)" else "Standard Precision Active (15s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isBatterySaver,
                    onCheckedChange = onToggleBatterySaver,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = SamrakshaTheme.colors.safe
                    )
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            Text(
                text = if (isBatterySaver) {
                    "Ultra-Saver Mode uses balanced location accuracy with 30s intervals and 25m displacement filters to minimize GPS power consumption."
                } else {
                    "Periodic 15s GPS updates with 10m displacement filtering. Eliminates idle sensor wakeups and avoids continuous background battery drain."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Start Journey Planner Card ────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartJourneyPlannerCard(
    currentLocation: UserLocation?,
    origin: String,
    isOriginCustom: Boolean,
    destination: String,
    activeAutocompleteField: AutocompleteField,
    autocompleteState: AutocompleteUiState,
    selectedCityFilter: String?,
    isLocationPermissionGranted: Boolean,
    isLoading: Boolean,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onOriginFocus: () -> Unit,
    onDestinationFocus: () -> Unit,
    onSelectOriginSuggestion: (PlaceSuggestion) -> Unit,
    onSelectDestinationSuggestion: (PlaceSuggestion) -> Unit,
    onClearOrigin: () -> Unit,
    onResetOrigin: () -> Unit,
    onClearDestination: () -> Unit,
    onCityFilterChange: (String?) -> Unit,
    onRetrySearch: () -> Unit,
    onDismissAutocomplete: () -> Unit,
    onQuickSelect: (String) -> Unit,
    onPreviewRoute: () -> Unit,
    onNavigateToNightActivityScore: () -> Unit = {},
    onStartJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showCustomCityDialog by remember { mutableStateOf(false) }
    var customCityInput by remember { mutableStateOf("") }

    SamrakshaCard(modifier = modifier) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
            Column {
                Text(
                    text = "Plan Safe Journey",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Automated guardian monitoring & Geoapify route intelligence",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

        // ── 1. Origin / Pickup Point Field ──
        val originDisplay = if (!isOriginCustom && currentLocation != null) {
            "Current Location (${String.format(Locale.US, "%.4f, %.4f", currentLocation.latitude, currentLocation.longitude)})"
        } else {
            origin
        }

        SamrakshaTextField(
            value = originDisplay,
            onValueChange = onOriginChange,
            label = "Pickup / Origin",
            placeholder = "Search origin or pickup point",
            leadingIcon = if (!isOriginCustom) Icons.Default.MyLocation else Icons.Default.PinDrop,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOriginCustom && origin.isNotBlank()) {
                        IconButton(onClick = onClearOrigin) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Origin",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (isOriginCustom) {
                        IconButton(
                            onClick = {
                                onResetOrigin()
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Reset to Current Location",
                                tint = SamrakshaTheme.colors.safe,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )

        // Suggestions for Origin
        if (activeAutocompleteField == AutocompleteField.ORIGIN && autocompleteState !is AutocompleteUiState.Idle) {
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))
            AutocompleteSuggestionsBox(
                state = autocompleteState,
                isLocationPermissionGranted = isLocationPermissionGranted,
                targetFieldLabel = "Origin",
                onSelectSuggestion = { suggestion ->
                    onSelectOriginSuggestion(suggestion)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onRetry = onRetrySearch,
                onDismiss = onDismissAutocomplete
            )
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

        // ── 2. Destination Field with Clear Button ──
        SamrakshaTextField(
            value = destination,
            onValueChange = onDestinationChange,
            label = "Destination Address / Place",
            placeholder = "Enter destination or landmark",
            leadingIcon = Icons.Default.Flag,
            trailingIcon = if (destination.isNotBlank()) {
                {
                    IconButton(onClick = onClearDestination) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Destination",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null
        )

        // Suggestions for Destination
        if (activeAutocompleteField == AutocompleteField.DESTINATION && autocompleteState !is AutocompleteUiState.Idle) {
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))
            AutocompleteSuggestionsBox(
                state = autocompleteState,
                isLocationPermissionGranted = isLocationPermissionGranted,
                targetFieldLabel = "Destination",
                onSelectSuggestion = { suggestion ->
                    onSelectDestinationSuggestion(suggestion)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onRetry = onRetrySearch,
                onDismiss = onDismissAutocomplete
            )
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

        // ── 3. City Selection / Bias Filter ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationCity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Search City Scope:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedCityFilter != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onCityFilterChange(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedCityFilter ✕",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // City Selector Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val cities = listOf("All Cities", "Bengaluru", "Mumbai", "Delhi", "Hyderabad", "Kolkata", "Chennai", "Pune", "Custom...")
            cities.forEach { city ->
                val isSelected = when (city) {
                    "All Cities" -> selectedCityFilter == null
                    "Custom..." -> selectedCityFilter != null && selectedCityFilter !in cities
                    else -> selectedCityFilter?.equals(city, ignoreCase = true) == true
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.clickable {
                        if (city == "All Cities") {
                            onCityFilterChange(null)
                        } else if (city == "Custom...") {
                            customCityInput = selectedCityFilter ?: ""
                            showCustomCityDialog = true
                        } else {
                            onCityFilterChange(city)
                        }
                    }
                ) {
                    Text(
                        text = if (city == "Custom..." && selectedCityFilter != null && selectedCityFilter !in cities) {
                            "City: $selectedCityFilter"
                        } else {
                            city
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

        // ── 4. Quick Shortcuts ──
        Text(
            text = "Quick Shortcuts",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Home", "Office / Work", "College / Campus", "Central Station", "City Hospital").forEach { shortcut ->
                QuickDestinationChip(
                    label = shortcut,
                    isSelected = destination == shortcut,
                    onClick = { onQuickSelect(shortcut) }
                )
            }
        }

        if (destination.isNotBlank()) {
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
            ) {
                SamrakshaOutlinedButton(
                    text = "Preview Route",
                    onClick = onPreviewRoute,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                SamrakshaOutlinedButton(
                    text = "Compare Night Activity",
                    onClick = onNavigateToNightActivityScore,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

        SamrakshaPrimaryButton(
            text = "Start Safe Journey Monitoring",
            onClick = onStartJourney,
            enabled = destination.isNotBlank(),
            isLoading = isLoading,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Custom City Dialog
    if (showCustomCityDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCityDialog = false },
            title = {
                Text(
                    text = "Search in Different City",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter a city name to scope autocomplete suggestions (e.g. Jaipur, Ahmedabad, Kochi).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                    SamrakshaTextField(
                        value = customCityInput,
                        onValueChange = { customCityInput = it },
                        placeholder = "City name (e.g. Lucknow)",
                        label = "City Name"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = customCityInput.trim()
                        if (trimmed.isNotBlank()) {
                            onCityFilterChange(trimmed)
                        } else {
                            onCityFilterChange(null)
                        }
                        showCustomCityDialog = false
                    }
                ) {
                    Text("Apply Filter", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomCityDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AutocompleteSuggestionsBox(
    state: AutocompleteUiState,
    isLocationPermissionGranted: Boolean,
    targetFieldLabel: String,
    onSelectSuggestion: (PlaceSuggestion) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SamrakshaTheme.spacing.small)
        ) {
            // Header strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$targetFieldLabel Suggestions",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Suggestions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!isLocationPermissionGranted) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GPS permission off • Distances unavailable, search active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (state) {
                is AutocompleteUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Searching genuine places via Geoapify...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is AutocompleteUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                    ) {
                        items(state.suggestions, key = { it.id }) { suggestion ->
                            PlaceSuggestionItem(
                                suggestion = suggestion,
                                onClick = { onSelectSuggestion(suggestion) }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
                is AutocompleteUiState.NoResults -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "No matching places found",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Check spelling or try selecting a different city above",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is AutocompleteUiState.Error -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                            .padding(SamrakshaTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isRateLimited) Icons.Default.Warning else Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.isRateLimited) "Search rate limit reached" else if (state.isNetworkError) "Network connection error" else "Search error",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(onClick = onRetry) {
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                is AutocompleteUiState.Idle -> {}
            }
        }
    }
}

@Composable
private fun PlaceSuggestionItem(
    suggestion: PlaceSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name.ifBlank { suggestion.fullAddress },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            val subText = if (suggestion.locality != null && suggestion.locality != suggestion.name) {
                suggestion.locality
            } else {
                suggestion.fullAddress
            }
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        suggestion.formattedDistance?.let { distance ->
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDestinationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ── Sentinel Safety Assurance Card ────────────────────────────────────────────

@Composable
private fun SentinelSafetyAssuranceCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = SamrakshaTheme.colors.safe,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
            Column {
                Text(
                    text = "Automated Journey Sentinel",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "During an active journey, your trusted contacts are linked to your transit state. Live location updates keep guardians informed until you tap Arrived Safely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Journey History Item & Empty State ────────────────────────────────────────

@Composable
private fun JourneyHistoryItem(
    journey: JourneyEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(SamrakshaTheme.spacing.base)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = journey.endLocationName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val isCompleted = journey.status == "COMPLETED"
                Surface(
                    shape = SamrakshaTheme.shapes.chip,
                    color = if (isCompleted) SamrakshaTheme.colors.safe.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isCompleted) "Safely Completed" else journey.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isCompleted) SamrakshaTheme.colors.safe else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "From: ${journey.startLocationName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                val durationText = formatDuration(journey.startTime, journey.completedTime)
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatTimestamp(journey.startTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun EmptyJourneyHistoryCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SamrakshaTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
            Text(
                text = "No Journey Records Yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Completed journey logs and safety records will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── End Journey Confirmation Dialog ───────────────────────────────────────────

@Composable
private fun EndJourneyConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Safe Arrival",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "Have you arrived at your destination safely? Ending the journey will complete tracking and notify your emergency guardians of your safe arrival.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            SamrakshaPrimaryButton(
                text = "Yes, Arrived Safely",
                onClick = onConfirm,
                fullWidth = false
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Keep Tracking",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// ── Utility Formatting Helpers ────────────────────────────────────────────────

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(startMs: Long, endMs: Long?): String {
    if (endMs == null || endMs <= startMs) return "In progress"
    val diffSec = (endMs - startMs) / 1000
    val mins = diffSec / 60
    val hours = mins / 60
    return if (hours > 0) {
        "${hours}h ${mins % 60}m"
    } else {
        "${mins.coerceAtLeast(1)} mins"
    }
}

private fun formatSecondsAgo(epoch: Long): String {
    if (epoch == 0L) return "Awaiting fix"
    val diffSec = (System.currentTimeMillis() - epoch) / 1000
    return when {
        diffSec < 5 -> "Updated just now"
        diffSec < 60 -> "Updated ${diffSec}s ago"
        else -> "Updated ${diffSec / 60}m ago"
    }
}

// ── Geoapify Safe Transit Route & Night Activity Score Comparison Card ────────

@Composable
private fun JourneyRouteMapCard(
    routeInfo: RouteInfo?,
    routeOptions: List<RouteOption>,
    selectedRouteOption: RouteOption?,
    travelMode: String,
    routeLimitationNotice: String?,
    isFactorBreakdownExpanded: Boolean,
    isCalculatingRoute: Boolean,
    routeError: String?,
    startLocationName: String,
    destinationName: String,
    onSelectRouteOption: (String) -> Unit,
    onTravelModeChange: (String) -> Unit,
    onToggleFactorBreakdown: () -> Unit,
    onRetryRoute: () -> Unit,
    onOpenFullMap: (() -> Unit)? = null,
    onNavigateToNightActivityScore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(SamrakshaTheme.spacing.base)) {
            // Header: Route title & ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Night Activity & Transit Routes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Open commercial venues & corridor infrastructure",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onOpenFullMap != null) {
                    IconButton(
                        onClick = onOpenFullMap,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Open in Safety Map",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(SamrakshaTheme.spacing.small))

            // Travel Mode Switcher: Drive / Bicycle / Walk
            TravelModeSelector(
                selectedMode = travelMode,
                onModeSelected = onTravelModeChange
            )

            Spacer(Modifier.height(SamrakshaTheme.spacing.small))

            if (isCalculatingRoute) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Analyzing night activity, open venues & routes...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (routeOptions.isNotEmpty()) {
                // Route Comparison List (up to 3 routes)
                Text(
                    text = if (routeOptions.size > 1) "Route Comparison (${routeOptions.size} Options)" else "Selected Corridor Path",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (routeLimitationNotice != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = routeLimitationNotice,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Render Route Comparison Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    routeOptions.forEach { option ->
                        RouteComparisonCard(
                            option = option,
                            isSelected = option.id == selectedRouteOption?.id || (selectedRouteOption == null && option.isSelected),
                            onClick = { onSelectRouteOption(option.id) }
                        )
                    }
                }

                // Factor Breakdown Card for Selected Route
                selectedRouteOption?.let { selected ->
                    Spacer(Modifier.height(10.dp))
                    NightActivityFactorBreakdownCard(
                        scoreResult = selected.nightActivityScore,
                        isExpanded = isFactorBreakdownExpanded,
                        onToggleExpand = onToggleFactorBreakdown
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Map visualization with route polyline and markers
                routeInfo?.let { activeRoute ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
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

                                if (activeRoute.points.isNotEmpty()) {
                                    map.addPolyline(
                                        PolylineOptions()
                                            .addAll(activeRoute.points)
                                            .color(android.graphics.Color.parseColor("#2563EB"))
                                            .width(5f)
                                    )
                                }

                                map.addMarker(
                                    MarkerOptions()
                                        .position(activeRoute.startPoint)
                                        .title("Start: $startLocationName")
                                )

                                map.addMarker(
                                    MarkerOptions()
                                        .position(activeRoute.destinationPoint)
                                        .title("Destination: $destinationName")
                                )

                                val builder = LatLngBounds.Builder()
                                builder.include(activeRoute.startPoint)
                                builder.include(activeRoute.destinationPoint)
                                activeRoute.points.forEach { builder.include(it) }
                                try {
                                    val bounds = builder.build()
                                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50))
                                } catch (_: Exception) {
                                    try {
                                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(activeRoute.startPoint, 13.0))
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

                Spacer(Modifier.height(10.dp))

                // Responsible Score Presentation & Transparency Disclaimer
                ResponsibleScoreDisclaimerCard()

                if (onOpenFullMap != null) {
                    Spacer(Modifier.height(8.dp))
                    SamrakshaOutlinedButton(
                        text = "View in Full Safety Map",
                        onClick = onOpenFullMap,
                        leadingIcon = {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (onNavigateToNightActivityScore != null) {
                    Spacer(Modifier.height(8.dp))
                    SamrakshaSecondaryButton(
                        text = "Full Night Activity Score & Route Comparison",
                        onClick = onNavigateToNightActivityScore,
                        leadingIcon = {
                            Icon(Icons.Default.NightlightRound, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (routeInfo != null) {
                // Fallback single route display if options failed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                                        .width(5f)
                                )
                            }
                            map.addMarker(
                                MarkerOptions()
                                    .position(routeInfo.startPoint)
                                    .title("Start: $startLocationName")
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
                            } catch (_: Exception) {}
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Distance: ${routeInfo.formattedDistance}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Est. Duration: ${routeInfo.formattedDuration}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (routeError != null) {
                // Graceful error state without crash
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = routeError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetryRoute) {
                            Text("Retry", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

// ── Travel Mode Selector ──────────────────────────────────────────────────────

@Composable
private fun TravelModeSelector(
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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

// ── Route Comparison Card ─────────────────────────────────────────────────────

@Composable
private fun RouteComparisonCard(
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
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Label, Radio, and Distance/Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${option.routeInfo.formattedDistance} • ${option.routeInfo.formattedDuration}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Middle Row: Score Progress Ring & Activity Level Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NightActivityScoreProgressRing(
                        score = scoreResult.totalScore,
                        color = activityColor
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Night Activity Score",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${scoreResult.openPlacesCount} open venues along corridor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
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
            }
        }
    }
}

// ── Night Activity Score Circular Progress Ring ───────────────────────────────

@Composable
private fun NightActivityScoreProgressRing(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.dp
        )
        CircularProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 4.dp
        )
        Text(
            text = "$score",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Night Activity Factor Breakdown Card ──────────────────────────────────────

@Composable
private fun NightActivityFactorBreakdownCard(
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Score Factor Breakdown (6 Factors)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse factors" else "Expand factors",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))

                    scoreResult.factorScores.forEachIndexed { index, factorScore ->
                        if (index > 0) Spacer(Modifier.height(8.dp))
                        FactorScoreRow(factorScore = factorScore)
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))

                    // Confidence and Operating Hours Disclosure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
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
private fun FactorScoreRow(
    factorScore: ActivityFactorScore,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = factorScore.factor.title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${factorScore.earnedPoints.roundToInt()} / ${factorScore.maxPoints.roundToInt()} pts",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(3.dp))

        LinearProgressIndicator(
            progress = { (factorScore.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = factorScore.detail,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Responsible Score Transparency & Safety Disclaimer Card ─────────────────

@Composable
private fun ResponsibleScoreDisclaimerCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Night Activity Metric Transparency",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "The Night Activity Score measures visible commercial activity, transit presence, and road infrastructure. It does NOT predict personal safety or guarantee freedom from crime. Always remain alert, stay on well-lit paths, and use the manual SOS button if you feel unsafe.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

