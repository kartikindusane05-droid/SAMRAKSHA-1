package com.samraksha.app.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.data.local.ThemeMode
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.notifications.NotificationPermissionHelper
import com.samraksha.app.ui.components.cards.SamrakshaCard
import com.samraksha.app.ui.components.dialogs.LocationPermissionRationaleDialog
import com.samraksha.app.ui.components.dialogs.NotificationPermissionRationaleDialog
import com.samraksha.app.ui.components.dialogs.SamrakshaAlertDialog
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.components.status.SamrakshaStatusPill
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.theme.SamrakshaTheme
import com.samraksha.app.ui.theme.WarningAmber40

/**
 * SAMRAKSHA Settings Screen.
 *
 * Provides organized management for:
 * - Appearance / Theme selection (System, Light, Dark)
 * - Emergency preferences (Shake-to-SOS, Countdown delay, Audio recording, SMS fallback, Contacts shortcut)
 * - Location permissions status and system settings launcher
 * - Alerts & Notifications preferences
 * - Privacy information policy
 * - Terms of service
 * - Affiliate disclosure
 * - About SAMRAKSHA & emergency helplines
 * - Logout with session cleanup
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory())
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var showLocationRationale by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.checkPermissions(context)
    }

    // Refresh permissions whenever the screen becomes RESUMED (e.g. after returning from OS Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    ScreenScaffold(
        title = "Settings",
        onNavigateBack = onNavigateBack
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SamrakshaTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.medium)
        ) {
            // ── Section 1: Appearance & Theme ────────────────────────────────
            SettingsSectionHeader(
                title = "Appearance",
                subtitle = "Choose how SAMRAKSHA looks on your device"
            )

            ThemeSelectorCard(
                currentMode = uiState.themeMode,
                onModeSelected = { viewModel.setThemeMode(it) }
            )

            // ── Section 2: Emergency Preferences ──────────────────────────────
            SettingsSectionHeader(
                title = "Emergency Preferences",
                subtitle = "Configure triggers and distress protocol behavior"
            )

            SamrakshaCard {
                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    iconBackground = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Shake to Activate SOS",
                    subtitle = "Trigger emergency distress by shaking device 3 times while SAMRAKSHA is open and active",
                    checked = uiState.shakeToSos,
                    onCheckedChange = { viewModel.toggleShakeToSos(it) },
                    showDivider = true
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Timer,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "SOS Countdown Delay",
                    subtitle = if (uiState.sosCountdownSeconds == 0) {
                        "Instant broadcast (0s buffer)"
                    } else {
                        "${uiState.sosCountdownSeconds} seconds buffer before dispatch"
                    },
                    onClick = { viewModel.openModal(SettingsModal.COUNTDOWN_PICKER) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.Default.Mic,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Auto-Record Audio on SOS",
                    subtitle = "Capture 30-second ambient audio clip during distress",
                    checked = uiState.autoRecordAudio,
                    onCheckedChange = { viewModel.toggleAutoRecordAudio(it) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.Default.Sms,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "SMS Fallback Broadcast",
                    subtitle = "Send direct SMS with live GPS coordinates if internet is down",
                    checked = uiState.smsFallback,
                    onCheckedChange = { viewModel.toggleSmsFallback(it) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.Default.BatterySaver,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Battery Saver Safe Mode",
                    subtitle = "Throttle GPS updates below 15% battery while retaining SOS",
                    checked = uiState.batterySaverSafeMode,
                    onCheckedChange = { viewModel.toggleBatterySaverSafeMode(it) },
                    showDivider = true
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Contacts,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Manage Emergency Contacts",
                    subtitle = "View, add, or test trusted responders",
                    onClick = onNavigateToContacts,
                    showDivider = false
                )
            }

            // ── Section 3: Permissions & System Access ───────────────────────
            SettingsSectionHeader(
                title = "Permissions & System Access",
                subtitle = "Only essential permissions required by active safety features"
            )

            LocationPermissionCard(
                hasFineLocation = uiState.hasFineLocationPermission,
                hasCoarseLocation = uiState.hasCoarseLocationPermission,
                onRequestPermission = { showLocationRationale = true },
                onOpenSettings = { LocationPermissionHelper.openAppSettings(context) }
            )

            NotificationPermissionCard(
                hasNotification = uiState.hasNotificationPermission,
                onRequestPermission = { showNotificationRationale = true },
                onOpenSettings = { NotificationPermissionHelper.openNotificationSettings(context) }
            )

            // ── Section 4: Alerts & Notifications ─────────────────────────────
            SettingsSectionHeader(
                title = "Alerts & Notifications",
                subtitle = "Manage safety broadcasts and audio cues"
            )

            SamrakshaCard {
                SettingsToggleRow(
                    icon = Icons.Default.NotificationsActive,
                    iconBackground = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Emergency Broadcasts",
                    subtitle = "High-priority distress signals from trusted circle",
                    checked = uiState.emergencyAlerts,
                    onCheckedChange = { viewModel.toggleEmergencyAlerts(it) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.Default.GpsFixed,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Safety Zone Alerts",
                    subtitle = "Entry and exit notifications for verified zones",
                    checked = uiState.zoneAlerts,
                    onCheckedChange = { viewModel.toggleZoneAlerts(it) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.Default.Route,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Journey Tracking Reminders",
                    subtitle = "Periodic check-in prompts while travelling",
                    checked = uiState.journeyReminders,
                    onCheckedChange = { viewModel.toggleJourneyReminders(it) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Sound Alerts",
                    subtitle = "Audible alarm tones for emergency countdowns and events",
                    checked = uiState.soundAlerts,
                    onCheckedChange = { viewModel.toggleSoundAlerts(it) },
                    showDivider = true
                )

                SettingsToggleRow(
                    icon = Icons.Default.Vibration,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Haptic Vibration",
                    subtitle = "Vibration cues during countdown and alerts",
                    checked = uiState.vibration,
                    onCheckedChange = { viewModel.toggleVibration(it) },
                    showDivider = false
                )
            }

            // ── Section 5: Legal & Transparency ──────────────────────────────
            SettingsSectionHeader(
                title = "Legal & Transparency",
                subtitle = "Policies, disclaimers, and store disclosures"
            )

            SamrakshaCard {
                SettingsNavigationRow(
                    icon = Icons.Default.Lock,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Privacy Information",
                    subtitle = "Zero data selling, end-to-end encrypted telemetry",
                    onClick = { viewModel.openModal(SettingsModal.PRIVACY_INFO) },
                    showDivider = true
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Description,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = "Terms of Service",
                    subtitle = "Emergency utility terms, obligations, and limitations",
                    onClick = { viewModel.openModal(SettingsModal.TERMS_OF_SERVICE) },
                    showDivider = true
                )

                SettingsNavigationRow(
                    icon = Icons.Default.Storefront,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Affiliate Disclosure",
                    subtitle = "Transparent partnership disclosure for Safety Store links",
                    onClick = { viewModel.openModal(SettingsModal.AFFILIATE_DISCLOSURE) },
                    showDivider = false
                )
            }

            // ── Section 6: About SAMRAKSHA ───────────────────────────────────
            SettingsSectionHeader(
                title = "About SAMRAKSHA",
                subtitle = "System architecture and national emergency resources"
            )

            SamrakshaCard {
                SettingsNavigationRow(
                    icon = Icons.Default.Info,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "About SAMRAKSHA",
                    subtitle = "Version 1.0.0 • Mission, architecture & emergency helplines",
                    onClick = { viewModel.openModal(SettingsModal.ABOUT_SAMRAKSHA) },
                    showDivider = false
                )
            }

            // ── Section 7: Account & Session ─────────────────────────────────
            SettingsSectionHeader(
                title = "Account",
                subtitle = "Manage active session on this device"
            )

            SamrakshaCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openModal(SettingsModal.LOGOUT_CONFIRM) }
                        .padding(vertical = SamrakshaTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Out",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Sign out and purge local user cache from this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraLarge))
        }
    }

    // ── Pre-permission Rationale Dialogs ──
    val activity = context as? Activity

    if (showLocationRationale) {
        val isLocDenied = activity != null && LocationPermissionHelper.isPermanentlyDenied(activity)

        LocationPermissionRationaleDialog(
            isPermanentlyDenied = isLocDenied,
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
        val isNotifDenied = activity != null && NotificationPermissionHelper.isPermanentlyDenied(activity)

        NotificationPermissionRationaleDialog(
            isPermanentlyDenied = isNotifDenied,
            onGrant = {
                showNotificationRationale = false
                val perm = NotificationPermissionHelper.PERMISSION
                if (perm != null) {
                    NotificationPermissionHelper.markPermissionRequested(context)
                    notificationLauncher.launch(perm)
                } else {
                    viewModel.checkPermissions(context)
                }
            },
            onDismiss = { showNotificationRationale = false },
            onOpenSettings = {
                showNotificationRationale = false
                NotificationPermissionHelper.openNotificationSettings(context)
            }
        )
    }

    // ── Dialogs & Modals ─────────────────────────────────────────────────────

    when (uiState.activeModal) {
        SettingsModal.PRIVACY_INFO -> {
            InfoDetailDialog(
                title = "Privacy Information",
                icon = Icons.Default.Lock,
                onDismiss = { viewModel.dismissModal() }
            ) {
                Text(
                    text = "End-to-End Encrypted Telemetry",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Distress signals, live coordinates, and emergency broadcast payloads are encrypted in transit using industry-standard TLS 1.3 protocol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Zero Commercial Tracking",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SAMRAKSHA never sells, rents, or monetizes personal location traces, travel history, or medical data. Your safety profile is strictly utilized for emergency response.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Local-First Offline Architecture",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Emergency contacts and safety zone caches remain accessible on-device even when internet connectivity drops. SMS fallback ensures messages transmit via cellular carrier directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Data Retention & Purging",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "You maintain complete ownership of your data. Logging out immediately purges local caches and revokes active authentication tokens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsModal.TERMS_OF_SERVICE -> {
            InfoDetailDialog(
                title = "Terms of Service",
                icon = Icons.Default.Description,
                onDismiss = { viewModel.dismissModal() }
            ) {
                Text(
                    text = "Auxiliary Safety Companion",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SAMRAKSHA is designed as an auxiliary personal safety utility. It does NOT replace official emergency response authorities (112, 911, or local police/medical services). In immediate life-threatening situations, always contact national emergency services directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "User Responsibilities",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Users agree to utilize SOS triggers responsibly. False or intentional misuse of emergency broadcast systems to panic circles or emergency helplines may be subject to legal liability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Network & Sensor Dependency",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Alert delivery speed depends on cellular coverage, GPS satellite locks, and device battery levels. SAMRAKSHA cannot guarantee uninterrupted delivery under adverse network or hardware conditions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsModal.AFFILIATE_DISCLOSURE -> {
            InfoDetailDialog(
                title = "Safety Store Affiliate Disclosure",
                icon = Icons.Default.Storefront,
                onDismiss = { viewModel.dismissModal() }
            ) {
                Text(
                    text = "Transparent Commercial Partnership",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "The SAMRAKSHA Safety Store features curated, safety-vetted hardware and emergency tools (such as pepper spray, high-decibel alarms, whistles, and escape tools) from verified merchant partners including Amazon and safety gear manufacturers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Zero Extra Cost to You",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "When you tap 'Buy Now' and purchase equipment through external affiliate links, SAMRAKSHA may earn a modest referral commission at zero additional cost to you. Pricing remains identical to direct retail.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "How Funds Are Used",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Affiliate proceeds directly subsidize our 24/7 cloud emergency server operations, high-reliability SMS gateway credits, and continuous safety engineering.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsModal.ABOUT_SAMRAKSHA -> {
            InfoDetailDialog(
                title = "About SAMRAKSHA",
                icon = Icons.Default.Shield,
                onDismiss = { viewModel.dismissModal() }
            ) {
                Text(
                    text = "SAMRAKSHA Safety Platform",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Version 1.0.0 • Build 2026.10-Android-M3\nClean Architecture • Jetpack Compose • Kotlin Flow • Room Local Database",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Our Mission",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Empowering women, solo commuters, and vulnerable citizens with proactive, intelligent safety tools—ensuring no one travels alone and rapid assistance is always a touch away.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "National Emergency Helplines (India)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• National Emergency Number: 112\n• Women Helpline: 1091 / 181\n• Childline: 1098\n• Cyber Crime Helpline: 1930",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsModal.COUNTDOWN_PICKER -> {
            CountdownPickerDialog(
                currentSeconds = uiState.sosCountdownSeconds,
                onSelect = {
                    viewModel.setSosCountdownSeconds(it)
                    viewModel.dismissModal()
                },
                onDismiss = { viewModel.dismissModal() }
            )
        }

        SettingsModal.LOGOUT_CONFIRM -> {
            SamrakshaAlertDialog(
                title = "Log Out of SAMRAKSHA?",
                message = "Your active session will be invalidated, and local cached profile and contact entries will be cleared from this device.",
                confirmButtonText = "Log Out",
                dismissButtonText = "Cancel",
                icon = Icons.AutoMirrored.Filled.Logout,
                isDestructive = true,
                onConfirm = {
                    viewModel.confirmLogout(onLoggedOut)
                },
                onDismiss = { viewModel.dismissModal() }
            )
        }

        SettingsModal.NONE -> Unit
    }
}

// ── Theme Selector Component ─────────────────────────────────────────────────

@Composable
private fun ThemeSelectorCard(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    SamrakshaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
        ) {
            ThemeOptionItem(
                title = "System",
                icon = Icons.Default.PhoneAndroid,
                isSelected = currentMode == ThemeMode.SYSTEM,
                onClick = { onModeSelected(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )

            ThemeOptionItem(
                title = "Light",
                icon = Icons.Default.LightMode,
                isSelected = currentMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )

            ThemeOptionItem(
                title = "Dark",
                icon = Icons.Default.DarkMode,
                isSelected = currentMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "themeBorder"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        label = "themeContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "themeContent"
    )

    OutlinedCard(
        modifier = modifier
            .clip(SamrakshaTheme.shapes.button)
            .clickable { onClick() },
        shape = SamrakshaTheme.shapes.button,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SamrakshaTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = contentColor
            )
        }
    }
}

// ── Location Permission Status Card ──────────────────────────────────────────

@Composable
private fun LocationPermissionCard(
    hasFineLocation: Boolean,
    hasCoarseLocation: Boolean = false,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isCoarseOnly = hasCoarseLocation && !hasFineLocation

    SamrakshaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    hasFineLocation -> MaterialTheme.colorScheme.primaryContainer
                                    isCoarseOnly -> WarningAmber40.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasFineLocation) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = when {
                                hasFineLocation -> MaterialTheme.colorScheme.primary
                                isCoarseOnly -> WarningAmber40
                                else -> MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Precise GPS Location",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = when {
                                hasFineLocation -> "Accurate location active"
                                isCoarseOnly -> "Approximate location active (precise recommended)"
                                else -> "Permission required"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SamrakshaStatusPill(
                    label = when {
                        hasFineLocation -> "Granted"
                        isCoarseOnly -> "Approximate"
                        else -> "Action Needed"
                    },
                    level = when {
                        hasFineLocation -> SafetyLevel.SAFE
                        isCoarseOnly -> SafetyLevel.WARNING
                        else -> SafetyLevel.WARNING
                    }
                )
            }

            Text(
                text = "High-accuracy GPS coordinates are essential for safety zone boundary detection, automated journey safety monitoring, and instant SOS dispatch to emergency contacts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (!hasFineLocation) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SamrakshaTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Grant Location Permission",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = SamrakshaTheme.shapes.button
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                Text(
                    text = "Open Device App Settings",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionCard(
    hasNotification: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    SamrakshaCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasNotification) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasNotification) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (hasNotification) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Safety Notifications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (hasNotification) "Alerts & sentinel active" else "Permission required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SamrakshaStatusPill(
                    label = if (hasNotification) "Granted" else "Action Needed",
                    level = if (hasNotification) SafetyLevel.SAFE else SafetyLevel.WARNING
                )
            }

            Text(
                text = "Notifications provide critical real-time visibility: the ongoing Journey Sentinel status in your system notification bar and urgent heads-up warnings upon entering high-risk areas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            if (!hasNotification) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SamrakshaTheme.shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Grant Notification Permission",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = SamrakshaTheme.shapes.button
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                Text(
                    text = "Open Notification Settings",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ── Reusable Row Components ──────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SamrakshaTheme.spacing.small, bottom = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SamrakshaTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = SamrakshaTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
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

        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 50.dp)
            )
        }
    }
}

// ── Info Detail Dialog ───────────────────────────────────────────────────────

@Composable
private fun InfoDetailDialog(
    title: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = SamrakshaTheme.shapes.button
            ) {
                Text("Close")
            }
        },
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// ── Countdown Picker Dialog ──────────────────────────────────────────────────

@Composable
private fun CountdownPickerDialog(
    currentSeconds: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to "Instant (0 seconds delay)",
        3 to "3 seconds countdown buffer",
        5 to "5 seconds buffer (Recommended)",
        10 to "10 seconds countdown buffer"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SOS Countdown Buffer",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.extraSmall)
            ) {
                Text(
                    text = "A buffer delay lets you cancel false alarms before an emergency broadcast is dispatched to contacts and services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                options.forEach { (seconds, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SamrakshaTheme.shapes.chip)
                            .clickable { onSelect(seconds) }
                            .padding(vertical = SamrakshaTheme.spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSeconds == seconds,
                            onClick = { onSelect(seconds) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (currentSeconds == seconds) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
