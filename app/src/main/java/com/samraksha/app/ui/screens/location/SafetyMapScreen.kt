package com.samraksha.app.ui.screens.location

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.BuildConfig
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.model.RouteInfo
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.location.model.LocationState
import com.samraksha.app.location.model.UserLocation
import com.samraksha.app.location.model.toAndroidLocation
import com.samraksha.app.ui.components.dialogs.LocationPermissionRationaleDialog
import com.samraksha.app.ui.components.map.MapLibreComposeView
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.screens.location.model.MapMarkerItem
import com.samraksha.app.ui.screens.location.model.MarkerCategory
import com.samraksha.app.ui.theme.SamrakshaTheme
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

/**
 * OpenStreetMap tile styles served via Geoapify with fallback to MapLibre demotiles.
 */
enum class MapStyleOption(val title: String, val styleUrl: String) {
    OSM_BRIGHT(
        "Standard (OSM)",
        if (BuildConfig.GEOAPIFY_API_KEY.isNotBlank())
            "https://maps.geoapify.com/v1/styles/osm-bright/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
        else
            "https://demotiles.maplibre.org/style.json"
    ),
    OSM_LIBERTY(
        "Liberty (OSM)",
        if (BuildConfig.GEOAPIFY_API_KEY.isNotBlank())
            "https://maps.geoapify.com/v1/styles/osm-liberty/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
        else
            "https://demotiles.maplibre.org/style.json"
    ),
    DARK_MATTER(
        "Night Mode",
        if (BuildConfig.GEOAPIFY_API_KEY.isNotBlank())
            "https://maps.geoapify.com/v1/styles/dark-matter/style.json?apiKey=${BuildConfig.GEOAPIFY_API_KEY}"
        else
            "https://demotiles.maplibre.org/style.json"
    )
}

/**
 * Renders POI markers and active journey transit route overlays onto MapLibre instance.
 */
private fun renderSafetyOverlays(
    map: MapLibreMap,
    markers: List<MapMarkerItem>,
    activeRoute: RouteInfo? = null
) {
    map.clear()

    // 1. Render active journey route polyline and terminal markers
    if (activeRoute != null && activeRoute.points.isNotEmpty()) {
        map.addPolyline(
            PolylineOptions()
                .addAll(activeRoute.points)
                .color(android.graphics.Color.parseColor("#2563EB"))
                .width(5.5f)
        )
        map.addMarker(
            MarkerOptions()
                .position(activeRoute.startPoint)
                .title("Journey Start")
        )
        map.addMarker(
            MarkerOptions()
                .position(activeRoute.destinationPoint)
                .title("Destination: ${activeRoute.destinationName}")
        )
    }

    // 2. Render safety POI markers
    for (poi in markers) {
        map.addMarker(
            MarkerOptions()
                .position(poi.position)
                .title(poi.title)
                .snippet(poi.snippet ?: "")
        )
    }
}

/**
 * Safely initializes and activates MapLibre's LocationComponent with a verified, fully loaded style.
 * Uses [RenderMode.NORMAL] and disables MapLibre's internal location engine ([useDefaultLocationEngine = false])
 * to prevent background sensor race conditions (compass heading crashes) and synchronize coordinate
 * updates directly with [SafetyMapViewModel].
 */
private fun setupLocationComponent(
    context: Context,
    map: MapLibreMap,
    style: Style,
    userLocation: UserLocation?
) {
    if (!style.isFullyLoaded) return
    try {
        val locationComponent = map.locationComponent
        val activationOptions = LocationComponentActivationOptions.builder(context, style)
            .useDefaultLocationEngine(false)
            .build()
        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.isLocationComponentEnabled = true
        locationComponent.cameraMode = CameraMode.NONE
        locationComponent.renderMode = RenderMode.NORMAL

        userLocation?.let { loc ->
            if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                locationComponent.forceLocationUpdate(loc.toAndroidLocation("samraksha_gps"))
            }
        }
    } catch (_: Exception) {}
}

@Composable
fun SafetyMapScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SafetyMapViewModel = viewModel(
        factory = SafetyMapViewModel.Factory(SamrakshaApp.instance.locationClient)
    ),
    incidentViewModel: IncidentReportViewModel = viewModel(
        factory = IncidentReportViewModel.Factory()
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showLocationRationale by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var maplibreMapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var maplibreStyleInstance by remember { mutableStateOf<Style?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val activity = context as? Activity
                    val permanentlyDenied = activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)
                    val isFine = LocationPermissionHelper.hasFineLocationPermission(context)
                    viewModel.onScreenResumed(isFineLocation = isFine, isPermanentlyDenied = permanentlyDenied)
                    try {
                        val map = maplibreMapInstance
                        val style = maplibreStyleInstance
                        if (map != null && style != null && style.isFullyLoaded && LocationPermissionHelper.hasLocationPermission(context)) {
                            setupLocationComponent(context, map, style, uiState.userLocation)
                        }
                    } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onScreenPaused()
                    try {
                        maplibreMapInstance?.let { map ->
                            if (map.locationComponent.isLocationComponentActivated) {
                                map.locationComponent.isLocationComponentEnabled = false
                            }
                        }
                    } catch (_: Exception) {}
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenPaused()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        val isFine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val activity = context as? Activity
        val permanentlyDenied = activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)
        viewModel.onPermissionResult(
            granted = granted,
            isFineLocation = isFine,
            isPermanentlyDenied = permanentlyDenied
        )
    }

    val onRequestPermission: () -> Unit = {
        val activity = context as? Activity
        if (activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)) {
            showLocationRationale = true
        } else {
            LocationPermissionHelper.markPermissionRequested(context)
            permissionLauncher.launch(LocationPermissionHelper.REQUIRED_PERMISSIONS)
        }
    }

    val onOpenAppSettings: () -> Unit = {
        LocationPermissionHelper.openAppSettings(context)
    }

    val onOpenLocationSettings: () -> Unit = {
        LocationPermissionHelper.openLocationSettings(context)
    }

    val onRetryLocation: () -> Unit = {
        viewModel.refreshLocation()
    }

    var selectedMapStyle by remember { mutableStateOf(MapStyleOption.OSM_BRIGHT) }
    var hasCenteredOnUser by remember { mutableStateOf(false) }

    // Sync user location with MapLibre location component and initial camera centering
    LaunchedEffect(uiState.userLocation, maplibreMapInstance, maplibreStyleInstance, uiState.hasLocationPermission) {
        val loc = uiState.userLocation
        val map = maplibreMapInstance
        val style = maplibreStyleInstance
        if (loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0) && map != null && style != null && style.isFullyLoaded && uiState.hasLocationPermission) {
            try {
                val locationComponent = map.locationComponent
                if (locationComponent.isLocationComponentActivated && locationComponent.isLocationComponentEnabled) {
                    locationComponent.forceLocationUpdate(loc.toAndroidLocation("samraksha_gps"))
                }
            } catch (_: Exception) {}

            if (!hasCenteredOnUser) {
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15.0))
                    hasCenteredOnUser = true
                } catch (_: Exception) {}
            }
        }
    }

    // Refresh POI markers and active transit route overlays
    LaunchedEffect(uiState.filteredMarkers, uiState.activeRoute, maplibreMapInstance, maplibreStyleInstance) {
        val map = maplibreMapInstance
        val style = maplibreStyleInstance
        if (map != null && style != null && style.isFullyLoaded) {
            renderSafetyOverlays(map, uiState.filteredMarkers, uiState.activeRoute)
            uiState.activeRoute?.let { route ->
                if (route.points.size >= 2) {
                    val builder = LatLngBounds.Builder()
                    builder.include(route.startPoint)
                    builder.include(route.destinationPoint)
                    route.points.forEach { builder.include(it) }
                    try {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 70))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Update location component when permission is granted or revoked
    LaunchedEffect(uiState.hasLocationPermission, maplibreMapInstance, maplibreStyleInstance) {
        val map = maplibreMapInstance
        val style = maplibreStyleInstance
        if (map != null && style != null && style.isFullyLoaded) {
            try {
                val locationComponent = map.locationComponent
                if (uiState.hasLocationPermission) {
                    setupLocationComponent(context, map, style, uiState.userLocation)
                } else {
                    if (locationComponent.isLocationComponentActivated) {
                        locationComponent.isLocationComponentEnabled = false
                    }
                }
            } catch (_: Exception) {}
        }
    }

    ScreenScaffold(
        title = "Safety Map",
        onNavigateBack = onNavigateBack
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            // ── MapLibre OpenStreetMap View ──
            MapLibreComposeView(
                modifier = Modifier.fillMaxSize(),
                styleUrl = selectedMapStyle.styleUrl,
                onStyleWillChange = {
                    // Invalidate style instance immediately to prevent stale access during style transition
                    maplibreStyleInstance = null
                },
                onMapDestroyed = {
                    maplibreMapInstance = null
                    maplibreStyleInstance = null
                },
                onMapReady = { maplibreMap, style ->
                    if (!style.isFullyLoaded) return@MapLibreComposeView
                    maplibreMapInstance = maplibreMap
                    maplibreStyleInstance = style

                    maplibreMap.uiSettings.isCompassEnabled = true
                    maplibreMap.uiSettings.isLogoEnabled = true
                    maplibreMap.uiSettings.isAttributionEnabled = true

                    renderSafetyOverlays(maplibreMap, uiState.filteredMarkers, uiState.activeRoute)

                    if (uiState.hasLocationPermission) {
                        setupLocationComponent(context, maplibreMap, style, uiState.userLocation)
                    }

                    val initialLat = uiState.userLocation?.latitude?.takeIf { it != 0.0 } ?: 12.9716
                    val initialLng = uiState.userLocation?.longitude?.takeIf { it != 0.0 } ?: 77.5946
                    maplibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(initialLat, initialLng), 14.5))

                    // Marker click listener
                    maplibreMap.setOnMarkerClickListener { marker ->
                        val matchedPoi = uiState.filteredMarkers.firstOrNull { it.title == marker.title }
                        if (matchedPoi != null) {
                            viewModel.selectMarker(matchedPoi)
                            return@setOnMarkerClickListener true
                        }
                        false
                    }

                    // Map click listener to dismiss marker selection
                    maplibreMap.addOnMapClickListener {
                        viewModel.selectMarker(null)
                        true
                    }
                }
            )

            // ── Attribution Badge: OpenStreetMap & Geoapify ──
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = SamrakshaTheme.spacing.base,
                        bottom = if (uiState.selectedMarker != null) 170.dp else SamrakshaTheme.spacing.base
                    ),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "© OpenStreetMap contributors, Powered by Geoapify",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // ── Top Bar: Active Transit Route Banner ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = SamrakshaTheme.spacing.base, vertical = SamrakshaTheme.spacing.small)
            ) {
                uiState.activeRoute?.let { route ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NearMe,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "To: ${route.destinationName}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${route.formattedDistance} • ${route.formattedDuration}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Dynamic Location & Permission Status Banner ──
            LocationStatusBanner(
                uiState = uiState,
                onRequestPermission = onRequestPermission,
                onOpenAppSettings = onOpenAppSettings,
                onOpenLocationSettings = onOpenLocationSettings,
                onRetryLocation = onRetryLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            )

            // ── Map Controls (Center Right) ──
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = SamrakshaTheme.spacing.base),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
            ) {
                // Map Style Switcher (OSM Bright -> OSM Liberty -> Dark Matter)
                SmallFloatingActionButton(
                    onClick = {
                        maplibreStyleInstance = null
                        try {
                            maplibreMapInstance?.let { map ->
                                if (map.locationComponent.isLocationComponentActivated) {
                                    map.locationComponent.isLocationComponentEnabled = false
                                }
                            }
                        } catch (_: Exception) {}
                        selectedMapStyle = when (selectedMapStyle) {
                            MapStyleOption.OSM_BRIGHT -> MapStyleOption.OSM_LIBERTY
                            MapStyleOption.OSM_LIBERTY -> MapStyleOption.DARK_MATTER
                            MapStyleOption.DARK_MATTER -> MapStyleOption.OSM_BRIGHT
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Switch Map Style", modifier = Modifier.size(20.dp))
                }

                // Zoom In
                SmallFloatingActionButton(
                    onClick = {
                        maplibreMapInstance?.animateCamera(CameraUpdateFactory.zoomIn())
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                }

                // Zoom Out
                SmallFloatingActionButton(
                    onClick = {
                        maplibreMapInstance?.animateCamera(CameraUpdateFactory.zoomOut())
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                }
            }

            // ── Action Controls (Bottom Right) ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = SamrakshaTheme.spacing.base,
                        bottom = if (uiState.selectedMarker != null) 160.dp else SamrakshaTheme.spacing.base
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Report an Unsafe Incident FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        val centerLat = maplibreMapInstance?.cameraPosition?.target?.latitude
                            ?: uiState.userLocation?.latitude?.takeIf { it != 0.0 }
                            ?: 12.9716
                        val centerLng = maplibreMapInstance?.cameraPosition?.target?.longitude
                            ?: uiState.userLocation?.longitude?.takeIf { it != 0.0 }
                            ?: 77.5946

                        val defaultLat = uiState.userLocation?.latitude?.takeIf { it != 0.0 } ?: centerLat
                        val defaultLng = uiState.userLocation?.longitude?.takeIf { it != 0.0 } ?: centerLng
                        incidentViewModel.resetForm(defaultLat, defaultLng)
                        showReportSheet = true
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Report Incident",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                )

                // Recenter GPS FAB
                FloatingActionButton(
                    onClick = {
                        if (!uiState.hasLocationPermission) {
                            onRequestPermission()
                        } else if (!uiState.isGpsEnabled) {
                            onOpenLocationSettings()
                        } else {
                            viewModel.refreshLocation()
                            uiState.userLocation?.let { loc ->
                                maplibreMapInstance?.animateCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude),
                                        15.5
                                    )
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (uiState.locationState is LocationState.Loading) {
                            Icons.Default.LocationSearching
                        } else {
                            Icons.Default.MyLocation
                        },
                        contentDescription = "Recenter GPS"
                    )
                }
            }

            // ── POI Marker Detail Card ──
            AnimatedVisibility(
                visible = uiState.selectedMarker != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(SamrakshaTheme.spacing.base)
            ) {
                uiState.selectedMarker?.let { marker ->
                    PoiDetailCard(
                        marker = marker,
                        onClose = { viewModel.selectMarker(null) }
                    )
                }
            }

            // ── Pre-permission rationale dialog ──
            if (showLocationRationale) {
                val activity = context as? Activity
                val permanentlyDenied = activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)

                LocationPermissionRationaleDialog(
                    isPermanentlyDenied = permanentlyDenied,
                    onGrant = {
                        showLocationRationale = false
                        LocationPermissionHelper.markPermissionRequested(context)
                        permissionLauncher.launch(LocationPermissionHelper.REQUIRED_PERMISSIONS)
                    },
                    onDismiss = { showLocationRationale = false },
                    onOpenSettings = {
                        showLocationRationale = false
                        LocationPermissionHelper.openAppSettings(context)
                    }
                )
            }

            // ── Report Unsafe Incident Sheet ──
            if (showReportSheet) {
                val centerLat = maplibreMapInstance?.cameraPosition?.target?.latitude
                    ?: uiState.userLocation?.latitude?.takeIf { it != 0.0 }
                    ?: 12.9716
                val centerLng = maplibreMapInstance?.cameraPosition?.target?.longitude
                    ?: uiState.userLocation?.longitude?.takeIf { it != 0.0 }
                    ?: 77.5946

                ReportIncidentSheet(
                    onDismiss = { showReportSheet = false },
                    userLocation = uiState.userLocation,
                    currentMapCenterLat = centerLat,
                    currentMapCenterLng = centerLng,
                    viewModel = incidentViewModel
                )
            }
        }
    }
}

// ── POI Marker Detail Card ──────────────────────────────────────────────────

@Composable
private fun PoiDetailCard(
    marker: MapMarkerItem,
    onClose: () -> Unit
) {
    val (icon, tint) = when (marker.category) {
        MarkerCategory.POLICE_STATION -> Pair(Icons.Default.LocalPolice, MaterialTheme.colorScheme.primary)
        MarkerCategory.HOSPITAL -> Pair(Icons.Default.LocalHospital, SamrakshaTheme.colors.emergency)
        MarkerCategory.SAFE_ZONE -> Pair(Icons.Default.Shield, SamrakshaTheme.colors.safe)
        MarkerCategory.EMERGENCY_INCIDENT -> Pair(Icons.Default.Warning, SamrakshaTheme.colors.warning)
        MarkerCategory.USER_LOCATION -> Pair(Icons.Default.NearMe, MaterialTheme.colorScheme.primary)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SamrakshaTheme.shapes.dialog,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = marker.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = marker.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            if (marker.snippet != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = marker.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Location Status Banner ──────────────────────────────────────────────────

@Composable
fun LocationStatusBanner(
    uiState: SafetyMapUiState,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetryLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        // State 1: Permission permanently denied (Direct user to Android Settings)
        !uiState.hasLocationPermission && uiState.isPermanentlyDenied -> {
            LocationBannerCard(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.LocationDisabled,
                title = "Location Permission Denied",
                message = "Location access was declined. Enable Location in Settings to track your position on the map.",
                buttonText = "Open Settings",
                buttonIcon = Icons.Default.Settings,
                onAction = onOpenAppSettings
            )
        }

        // State 2: Permission required (Not yet granted)
        !uiState.hasLocationPermission -> {
            LocationBannerCard(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.LocationDisabled,
                title = "Location Permission Required",
                message = "Grant permission to view and track your location on the map in real time.",
                buttonText = "Grant Permission",
                buttonIcon = Icons.Default.MyLocation,
                onAction = onRequestPermission
            )
        }

        // State 3: GPS / Location Services Disabled on device
        !uiState.isGpsEnabled || uiState.locationState is LocationState.Error.GpsDisabled -> {
            LocationBannerCard(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.LocationOff,
                title = "Location Services Disabled",
                message = "Device GPS is turned off. Turn on location services to view your current location.",
                buttonText = "Enable GPS",
                buttonIcon = Icons.Default.LocationOn,
                onAction = onOpenLocationSettings
            )
        }

        // State 4: Location Loading / Acquiring fix
        uiState.locationState is LocationState.Loading -> {
            Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Acquiring GPS fix…",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Connecting to satellites to determine coordinates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // State 5: Location Unavailable / Error
        uiState.locationState is LocationState.Error.Unavailable -> {
            val reason = uiState.locationState.reason
            LocationBannerCard(
                modifier = modifier,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = Icons.Default.Warning,
                title = "Location Unavailable",
                message = reason.ifBlank { "Could not retrieve current GPS coordinates. Ensure clear view of sky." },
                buttonText = "Retry",
                buttonIcon = Icons.Default.Refresh,
                onAction = onRetryLocation
            )
        }

        // State 6: Approximate location active notice (only if COARSE granted, but FINE not granted)
        uiState.hasLocationPermission && !uiState.isFineLocation -> {
            Card(
                modifier = modifier,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Approximate Location Active",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Exact location tracking benefits from Precise Location.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    TextButton(onClick = onRequestPermission) {
                        Text("Enable Precise")
                    }
                }
            }
        }

        // State 7: Success / Location Available -> No banner displayed!
        else -> Unit
    }
}

@Composable
private fun LocationBannerCard(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String,
    buttonIcon: ImageVector,
    onAction: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onAction() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = contentColor
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = containerColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = buttonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = buttonText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
