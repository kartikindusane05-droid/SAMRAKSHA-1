package com.samraksha.app.ui.screens.emergency

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationDisabled
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.emergency.AlertDeliveryState
import com.samraksha.app.emergency.ContactAlertDelivery
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.ui.components.buttons.SamrakshaSecondaryButton
import com.samraksha.app.ui.components.dialogs.LocationPermissionRationaleDialog
import com.samraksha.app.ui.theme.EmergencyRed10
import com.samraksha.app.ui.theme.EmergencyRed20
import com.samraksha.app.ui.theme.EmergencyRed40
import com.samraksha.app.ui.theme.EmergencyRed50
import com.samraksha.app.ui.theme.EmergencyRed90
import com.samraksha.app.ui.theme.SamrakshaTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    viewModel: SosViewModel = viewModel(
        factory = SosViewModel.Factory(
            repository = SamrakshaApp.instance.emergencyEventRepository,
            contactRepository = SamrakshaApp.instance.emergencyContactRepository,
            locationClient = SamrakshaApp.instance.locationClient,
            alertDispatcher = SamrakshaApp.instance.emergencyAlertDispatcher,
            userRepository = SamrakshaApp.instance.userRepository,
            settingsPreferences = SamrakshaApp.instance.settingsPreferences
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(LocationPermissionHelper.hasLocationPermission(context))
    }
    var showLocationRationale by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    hasLocationPermission = LocationPermissionHelper.hasLocationPermission(context)
                    viewModel.onScreenResumed(context)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onScreenPaused()
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

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.any { it }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.retryLocationAndResendAlert()
        }
    }

    // Background color changes based on state
    val bgColor = when (uiState.state) {
        SosState.ACTIVATED, SosState.ACTIVATING -> EmergencyRed10
        SosState.ARMED                          -> EmergencyRed20
        SosState.ARMING, SosState.CANCELLED, SosState.IDLE -> MaterialTheme.colorScheme.background
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Emergency SOS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = if (uiState.state in listOf(SosState.ACTIVATED, SosState.ACTIVATING, SosState.ARMED))
                        Color.White else MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SamrakshaTheme.spacing.base),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top status banner ──────────────────────────────────
            StatusBanner(
                state = uiState.state,
                isShakeTriggered = uiState.isShakeTriggered
            )

            if (uiState.state == SosState.IDLE && uiState.isShakeToSosEnabled) {
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (uiState.isShakeListening) SamrakshaTheme.colors.safe else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isShakeListening)
                                "Three-Shake SOS Ready (Foreground Only)"
                            else
                                "Shake to Activate SOS Enabled",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.state == SosState.IDLE && !hasLocationPermission) {
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showLocationRationale = true }
                ) {
                    Row(
                        modifier = Modifier.padding(SamrakshaTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationDisabled,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Location access is disabled",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Emergency coordinates won't be sent to guardians. Tap to enable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // ── Central SOS control / Active View ──────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (uiState.state) {
                    SosState.ACTIVATING -> ActivatingView()
                    SosState.ACTIVATED  -> ActivatedView(
                        uiState = uiState,
                        onRetryLocation = { viewModel.retryLocationAndResendAlert() },
                        onRequestSmsPermission = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
                        onOpenMessagingApp = {
                            val numbers = uiState.contactDeliveries.map { it.phoneNumber }
                            val msg = uiState.activeAlertMessage ?: "EMERGENCY SOS ALERT! Please contact me immediately."
                            try {
                                val intent = SamrakshaApp.instance.emergencyAlertDispatcher.createMessagingIntent(numbers, msg)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        onNavigateToContacts = onNavigateToContacts,
                        onShareEmergencyLink = {
                            val msg = uiState.activeAlertMessage ?: "EMERGENCY SOS ALERT! Please help."
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Emergency SOS Alert"))
                        }
                    )
                    SosState.CANCELLED  -> CancelledView(onReset = { viewModel.reset() })
                    else -> SosHoldButton(
                        state = uiState.state,
                        holdProgress = uiState.holdProgress,
                        countdownSeconds = uiState.countdownSeconds,
                        isShakeTriggered = uiState.isShakeTriggered,
                        onHoldStart  = { viewModel.onHoldStart() },
                        onHoldRelease = { viewModel.onHoldRelease() }
                    )
                }
            }

            // ── Bottom controls ────────────────────────────────────
            BottomControls(
                state = uiState.state,
                onCancel = { viewModel.cancel() },
                onReset  = { viewModel.reset() }
            )
        }
    }

    if (showLocationRationale) {
        val activity = context as? Activity
        val permanentlyDenied = activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)

        LocationPermissionRationaleDialog(
            isPermanentlyDenied = permanentlyDenied,
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
}

// ── Status Banner ──────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(
    state: SosState,
    isShakeTriggered: Boolean = false
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "status_banner"
    ) { s ->
        val (icon, title, message, tint) = when (s) {
            SosState.IDLE       -> BannerData(Icons.Default.Warning,  "Emergency SOS Ready",
                "Hold the SOS button for 3 seconds to arm the emergency signal.",
                MaterialTheme.colorScheme.onSurfaceVariant)
            SosState.ARMING     -> BannerData(Icons.Default.Warning,  "Arming…",
                "Keep holding — release to cancel.",
                EmergencyRed40)
            SosState.ARMED      -> if (isShakeTriggered) {
                BannerData(Icons.Default.Warning, "3 SHAKES DETECTED",
                    "Shake-triggered countdown active. Tap Cancel immediately to abort.",
                    EmergencyRed50)
            } else {
                BannerData(Icons.Default.Warning, "SOS WILL ACTIVATE",
                    "Countdown started. Tap Cancel to abort immediately.",
                    EmergencyRed50)
            }
            SosState.ACTIVATING -> BannerData(Icons.Default.Refresh, "Activating SOS…",
                "Acquiring GPS fix and dispatching guardian alerts.",
                Color.White)
            SosState.ACTIVATED  -> BannerData(Icons.Default.CheckCircle, "SOS ACTIVATED",
                "Emergency signal dispatched. Guardians have been notified.",
                Color.White)
            SosState.CANCELLED  -> BannerData(Icons.Default.CheckCircle, "SOS Cancelled",
                "The emergency signal was cancelled. You are safe.",
                SamrakshaTheme.colors.safe)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    when (s) {
                        SosState.ACTIVATED, SosState.ACTIVATING -> EmergencyRed40
                        SosState.ARMED     -> EmergencyRed90
                        SosState.ARMING    -> MaterialTheme.colorScheme.errorContainer
                        SosState.CANCELLED -> SamrakshaTheme.colors.safeContainer
                        SosState.IDLE      -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(SamrakshaTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Column {
                Text(title,   style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = tint)
                Text(message, style = MaterialTheme.typography.bodySmall, color = tint.copy(alpha = 0.85f))
            }
        }
    }
}

private data class BannerData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val tint: Color
)

// ── Press-and-Hold SOS Button ──────────────────────────────────────────────────

@Composable
private fun SosHoldButton(
    state: SosState,
    holdProgress: Float,
    countdownSeconds: Int,
    isShakeTriggered: Boolean = false,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
    buttonSize: Dp = 200.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress,
        animationSpec = tween(100),
        label = "hold_progress"
    )

    // Pulsing halo when IDLE
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == SosState.IDLE) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(buttonSize * 1.4f)
    ) {
        // Outer pulsing halo ring
        if (state == SosState.IDLE) {
            Box(
                modifier = Modifier
                    .size(buttonSize * 1.25f)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(EmergencyRed40.copy(alpha = 0.12f))
            )
        }

        // Hold-progress arc
        if (state == SosState.ARMING) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(buttonSize * 1.18f),
                color = EmergencyRed40,
                strokeWidth = 8.dp,
                strokeCap = StrokeCap.Round,
                trackColor = EmergencyRed90
            )
        }

        // Core button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        listOf(EmergencyRed50, EmergencyRed40)
                    )
                )
                .border(4.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .pointerInput(state) {
                    detectTapGestures(
                        onPress = {
                            onHoldStart()
                            tryAwaitRelease()
                            onHoldRelease()
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { scaleIn(tween(200)) + fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "button_content"
            ) { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (s) {
                        SosState.ARMED -> {
                            Text(
                                text = countdownSeconds.toString(),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                if (isShakeTriggered) "SHAKE DETECTED" else "ACTIVATING",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Text(
                                "SOS",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 6.sp
                            )
                            Text(
                                text = if (s == SosState.ARMING) "HOLD…" else "HOLD 3s",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Activating In-Progress View ────────────────────────────────────────────────

@Composable
private fun ActivatingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(SamrakshaTheme.spacing.large)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color.White,
            strokeWidth = 5.dp
        )
        Spacer(Modifier.height(SamrakshaTheme.spacing.large))
        Text(
            text = "Activating Emergency SOS...",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(Modifier.height(SamrakshaTheme.spacing.small))
        Text(
            text = "Acquiring live GPS fix and preparing alerts for your emergency contacts.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

// ── Activated State View with Delivery & GPS Status ───────────────────────────

@Composable
private fun ActivatedView(
    uiState: SosUiState,
    onRetryLocation: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onOpenMessagingApp: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onShareEmergencyLink: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(vertical = SamrakshaTheme.spacing.small)
    ) {
        // Pulsing red ring to show live SOS
        val infiniteTransition = rememberInfiniteTransition(label = "active_pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "active_scale"
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(EmergencyRed40.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EmergencyRed40)
                    .border(3.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("LIVE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(SamrakshaTheme.spacing.small))
        Text(
            "Emergency SOS Active",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            uiState.deliverySummary ?: "Emergency coordinates and alerts generated.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(SamrakshaTheme.spacing.base))

        // ── 1. GPS Coordinates Card ───────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = if (uiState.userLocation != null) SamrakshaTheme.colors.safe else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Emergency Coordinates",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (uiState.isFetchingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (uiState.userLocation == null) {
                        TextButton(onClick = onRetryLocation) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Retry GPS", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (uiState.userLocation != null) {
                    val loc = uiState.userLocation
                    Text(
                        text = String.format(Locale.US, "Latitude: %.5f | Longitude: %.5f", loc.latitude, loc.longitude),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Clickable OpenStreetMap link included in guardian dispatch",
                        style = MaterialTheme.typography.bodySmall,
                        color = SamrakshaTheme.colors.safe
                    )
                } else {
                    Text(
                        text = uiState.locationError ?: "GPS location could not be determined.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Alert dispatched with timestamp; tap Retry GPS once you have reception.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(SamrakshaTheme.spacing.small))

        // ── 2. Guardian Alerts Delivery Card ──────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guardian Alert Status",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.contactDeliveries.size} Contact(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (uiState.contactDeliveries.isEmpty()) {
                    Text(
                        text = "No emergency contacts configured yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToContacts,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Emergency Contacts")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.contactDeliveries.forEach { delivery ->
                            ContactDeliveryRow(delivery = delivery)
                        }
                    }

                    val hasPermissionFailure = uiState.contactDeliveries.any { it.status == AlertDeliveryState.FAILED_PERMISSION_DENIED }
                    val hasAnyFailure = uiState.contactDeliveries.any { it.status != AlertDeliveryState.SENT && it.status != AlertDeliveryState.DELIVERED }

                    if (hasPermissionFailure) {
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = onRequestSmsPermission,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant SMS Permission & Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }

                    if (hasAnyFailure) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onOpenMessagingApp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send via Device SMS App")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onShareEmergencyLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Emergency Link Externally")
                }
            }
        }
    }
}

@Composable
private fun ContactDeliveryRow(delivery: ContactAlertDelivery) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = delivery.contactName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = delivery.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val (badgeText, badgeColor, textColor) = when (delivery.status) {
            AlertDeliveryState.SENT, AlertDeliveryState.DELIVERED ->
                Triple("Sent via SMS", SamrakshaTheme.colors.safeContainer, SamrakshaTheme.colors.safe)
            AlertDeliveryState.PENDING ->
                Triple("Pending", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            AlertDeliveryState.FAILED_PERMISSION_DENIED ->
                Triple("Permission Needed", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
            AlertDeliveryState.FAILED_NO_SERVICE ->
                Triple("No Service", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
            AlertDeliveryState.FAILED_INVALID_NUMBER ->
                Triple("Invalid Number", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
            AlertDeliveryState.FAILED_UNKNOWN ->
                Triple("Delivery Failed", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}

// ── Cancelled View ──────────────────────────────────────────────────────────────

@Composable
private fun CancelledView(onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SamrakshaTheme.colors.safe,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(SamrakshaTheme.spacing.large))
        Text(
            "You are safe",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(SamrakshaTheme.spacing.small))
        Text(
            "Emergency SOS signal has been cancelled.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(SamrakshaTheme.spacing.large))
        SamrakshaSecondaryButton(
            text = "Return to SOS Screen",
            onClick = onReset,
            fullWidth = false
        )
    }
}

// ── Bottom Controls ─────────────────────────────────────────────────────────────

@Composable
private fun BottomControls(
    state: SosState,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    AnimatedVisibility(
        visible = state == SosState.ARMED || state == SosState.ACTIVATED || state == SosState.ACTIVATING,
        enter = fadeIn(tween(300)) + scaleIn(tween(300)),
        exit  = fadeOut(tween(200))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SamrakshaTheme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                onClick = onCancel,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White,
                    contentColor = EmergencyRed40
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    if (state == SosState.ACTIVATED) "I AM SAFE / CANCEL SOS" else "CANCEL SOS",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Tap cancel immediately to end the emergency alert",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
