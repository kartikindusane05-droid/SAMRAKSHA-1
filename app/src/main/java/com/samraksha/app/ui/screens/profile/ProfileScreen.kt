package com.samraksha.app.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.ui.components.cards.SamrakshaCard
import com.samraksha.app.ui.components.inputs.SamrakshaTextField
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.components.status.SamrakshaStatusPill
import com.samraksha.app.ui.theme.SamrakshaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SAMRAKSHA Profile Screen.
 *
 * Sections:
 *  - Hero avatar header with name, email, phone
 *  - Edit profile bottom sheet (name, phone, medical info)
 *  - Emergency contacts shortcut
 *  - Journey settings (auto-start, speed alerts, SOS share)
 *  - Safety preferences (shake-SOS, background tracking, sound, location share)
 *  - Sync status card
 *  - Logout with confirmation dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    onNavigateToJourney: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()) }

    // Show snackbar on transient messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Edit shortcut in top bar only if authenticated
                    if (uiState.user != null) {
                        IconButton(onClick = { viewModel.showEditDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.isLoading && uiState.user == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.medium)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Loading profile...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (uiState.user == null) {
            ProfileEmptyState(
                onLoginClick = onLoggedOut,
                onRetry = { viewModel.refreshProfile() },
                errorMessage = uiState.errorMessage,
                modifier = modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
            ) {
                // ── Hero Header ───────────────────────────────────────────────────
            ProfileHeroHeader(
                uiState = uiState,
                syncOnline = syncState.isOnline,
                onEditClick = { viewModel.showEditDialog() }
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

            // ── Section: Emergency Contacts ───────────────────────────────────
            ProfileSectionHeader(
                label = "Safety Network",
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
            )

            ProfileNavigationRow(
                icon = Icons.Default.Contacts,
                iconBackground = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.error,
                title = "Emergency Contacts",
                subtitle = "Manage guardians & SOS recipients",
                onClick = onNavigateToContacts,
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
            )

            // ── Section: Journey Settings ─────────────────────────────────────
            ProfileSectionHeader(
                label = "Journey Settings",
                modifier = Modifier.padding(
                    start = SamrakshaTheme.spacing.base,
                    end = SamrakshaTheme.spacing.base,
                    top = SamrakshaTheme.spacing.small
                )
            )

            SamrakshaCard(
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
            ) {
                ProfileNavigationRow(
                    icon = Icons.Default.Route,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Journey Monitor",
                    subtitle = "Configure active journey tracking",
                    onClick = onNavigateToJourney,
                    showDivider = true
                )
                ProfileToggleRow(
                    icon = Icons.Default.MyLocation,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Auto-Start on Departure",
                    subtitle = "Detect trip start automatically",
                    checked = uiState.journeyAutoStart,
                    onCheckedChange = { viewModel.toggleJourneyAutoStart(it) },
                    showDivider = true
                )
                ProfileToggleRow(
                    icon = Icons.Default.Speed,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Speed Alert Notifications",
                    subtitle = "Alert when exceeding safe speed",
                    checked = uiState.journeySpeedAlerts,
                    onCheckedChange = { viewModel.toggleJourneySpeedAlerts(it) },
                    showDivider = true
                )
                ProfileToggleRow(
                    icon = Icons.Default.Shield,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Share Route on SOS",
                    subtitle = "Send live GPS link to contacts on SOS",
                    checked = uiState.journeyShareOnSos,
                    onCheckedChange = { viewModel.toggleJourneyShareOnSos(it) }
                )
            }

            // ── Section: Safety Preferences ───────────────────────────────────
            ProfileSectionHeader(
                label = "Safety Preferences",
                modifier = Modifier.padding(
                    start = SamrakshaTheme.spacing.base,
                    end = SamrakshaTheme.spacing.base,
                    top = SamrakshaTheme.spacing.small
                )
            )

            SamrakshaCard(
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
            ) {
                ProfileToggleRow(
                    icon = Icons.Default.Warning,
                    iconBackground = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = "Shake Phone for SOS",
                    subtitle = "Trigger alert by shaking device 3×",
                    checked = uiState.shakeToSos,
                    onCheckedChange = { viewModel.toggleShakeToSos(it) },
                    showDivider = true
                )
                ProfileToggleRow(
                    icon = Icons.Default.GpsFixed,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Background GPS Sentinel",
                    subtitle = "Keep geofencing active in background",
                    checked = uiState.backgroundTracking,
                    onCheckedChange = { viewModel.toggleBackgroundTracking(it) },
                    showDivider = true
                )
                ProfileToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = "Sound Alerts",
                    subtitle = "Play audio cue on SOS and safety events",
                    checked = uiState.soundAlerts,
                    onCheckedChange = { viewModel.toggleSoundAlerts(it) },
                    showDivider = true
                )
                ProfileToggleRow(
                    icon = Icons.Default.MyLocation,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Live Location Sharing",
                    subtitle = "Share real-time location with contacts",
                    checked = uiState.locationShareWithContacts,
                    onCheckedChange = { viewModel.toggleLocationShare(it) }
                )
            }

            // ── Section: Medical Information ──────────────────────────────────
            ProfileSectionHeader(
                label = "Emergency Medical ID",
                modifier = Modifier.padding(
                    start = SamrakshaTheme.spacing.base,
                    end = SamrakshaTheme.spacing.base,
                    top = SamrakshaTheme.spacing.small
                )
            )

            MedicalInfoCard(
                uiState = uiState,
                onEditClick = { viewModel.showEditDialog() },
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
            )

            // ── Section: Sync Status ──────────────────────────────────────────
            ProfileSectionHeader(
                label = "Data & Cloud Sync",
                modifier = Modifier.padding(
                    start = SamrakshaTheme.spacing.base,
                    end = SamrakshaTheme.spacing.base,
                    top = SamrakshaTheme.spacing.small
                )
            )

            SyncStatusCard(
                isOnline = syncState.isOnline,
                isSyncing = syncState.isSyncing,
                statusMessage = syncState.statusMessage,
                lastSyncTimestamp = syncState.lastSyncTimestamp,
                lastSummary = syncState.lastSummary,
                timeFormatter = timeFormatter,
                onSyncNow = { viewModel.triggerSync() },
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.base)
            )

            // ── Logout Button ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            LogoutButton(
                isLoggingOut = uiState.isLoggingOut,
                onLogoutClick = { viewModel.requestLogout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SamrakshaTheme.spacing.base)
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.huge))
        }
    }
}

    // ── Edit Profile Dialog ───────────────────────────────────────────────────
    if (uiState.isEditing) {
        EditProfileDialog(
            uiState = uiState,
            onDismiss = { viewModel.dismissEditDialog() },
            onSave = { viewModel.saveProfile() },
            onNameChange = { viewModel.updateEditName(it) },
            onPhoneChange = { viewModel.updateEditPhone(it) },
            onBloodGroupChange = { viewModel.updateEditBloodGroup(it) },
            onAllergiesChange = { viewModel.updateEditAllergies(it) },
            onOrganDonorChange = { viewModel.updateEditOrganDonor(it) }
        )
    }

    // ── Logout Confirmation Dialog ────────────────────────────────────────────
    if (uiState.showLogoutConfirm) {
        LogoutConfirmDialog(
            onDismiss = { viewModel.dismissLogoutConfirm() },
            onConfirm = { viewModel.confirmLogout(onLoggedOut) }
        )
    }
}

// ── Hero Header ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroHeader(
    uiState: ProfileUiState,
    syncOnline: Boolean,
    onEditClick: () -> Unit
) {
    val user = uiState.user
    val initials = user?.fullName
        ?.split(" ")
        ?.take(2)
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.joinToString("")
        ?: "?"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = SamrakshaTheme.spacing.large,
                    vertical = SamrakshaTheme.spacing.large
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with initials
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                    .clickable { onEditClick() },
                contentAlignment = Alignment.Center
            ) {
                if (initials != "?") {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Edit overlay hint
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tap to edit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onEditClick() }
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

            // Full name
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = user?.fullName?.ifBlank { "SAMRAKSHA User" } ?: "SAMRAKSHA User",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

            // Email — read-only (from auth)
            user?.email?.takeIf { it.isNotBlank() }?.let { email ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Phone
            user?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

            // Status pills row
            Row(
                horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SamrakshaStatusPill(
                    label = if (syncOnline) "Connected" else "Offline",
                    level = if (syncOnline) SafetyLevel.SAFE else SafetyLevel.WARNING
                )
                if (user?.isOrganDonor == true) {
                    SamrakshaStatusPill(
                        label = "Organ Donor",
                        level = SafetyLevel.INFO,
                        showPulsingDot = false
                    )
                }
                user?.bloodGroup?.takeIf { it.isNotBlank() }?.let { bloodGroup ->
                    SamrakshaStatusPill(
                        label = "Blood: $bloodGroup",
                        level = SafetyLevel.INACTIVE,
                        showPulsingDot = false
                    )
                }
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

// ── Navigation Row (no toggle) ────────────────────────────────────────────────

@Composable
private fun ProfileNavigationRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = SamrakshaTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

// ── Toggle Row ────────────────────────────────────────────────────────────────

@Composable
private fun ProfileToggleRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "switchTrack"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SamrakshaTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = Color.White
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 56.dp)
            )
        }
    }
}

// ── Medical Info Card ─────────────────────────────────────────────────────────

@Composable
private fun MedicalInfoCard(
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = uiState.user
    SamrakshaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Medical Information",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(
                onClick = onEditClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = SamrakshaTheme.spacing.small)
        )

        MedicalInfoRow(
            icon = Icons.Default.Bloodtype,
            label = "Blood Group",
            value = user?.bloodGroup?.ifBlank { "Not set" } ?: "Not set"
        )
        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
        MedicalInfoRow(
            icon = Icons.Default.MedicalServices,
            label = "Allergies",
            value = user?.allergies?.ifBlank { "None reported" } ?: "None reported"
        )
        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
        MedicalInfoRow(
            icon = Icons.Default.HealthAndSafety,
            label = "Organ Donor",
            value = if (user?.isOrganDonor == true) "Registered Donor" else "Not registered"
        )
    }
}

@Composable
private fun MedicalInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Sync Status Card ──────────────────────────────────────────────────────────

@Composable
private fun SyncStatusCard(
    isOnline: Boolean,
    isSyncing: Boolean,
    statusMessage: String,
    lastSyncTimestamp: Long?,
    lastSummary: com.samraksha.app.domain.repository.SyncSummary?,
    timeFormatter: SimpleDateFormat,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    SamrakshaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isOnline) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isOnline) "Cloud Synced" else "Offline Mode",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val lastSyncStr = lastSyncTimestamp?.let {
                        "Last sync: ${timeFormatter.format(Date(it))}"
                    } ?: "Not synced yet"
                    Text(
                        text = lastSyncStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        AnimatedVisibility(visible = lastSummary != null) {
            lastSummary?.let { summary ->
                Column {
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        SyncMetricItem("Contacts", summary.contactsSynced)
                        SyncMetricItem("Events", summary.eventsSynced)
                        SyncMetricItem("Journeys", summary.journeysSynced)
                        SyncMetricItem("Zones", summary.safetyZonesSynced)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

        Button(
            onClick = onSyncNow,
            enabled = !isSyncing && isOnline,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSyncing) "Syncing..." else "Sync Now",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SyncMetricItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Logout Button ─────────────────────────────────────────────────────────────

@Composable
private fun LogoutButton(
    isLoggingOut: Boolean,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isLoggingOut, onClick = onLogoutClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SamrakshaTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
            Text(
                text = if (isLoggingOut) "Signing out..." else "Sign Out",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ── Edit Profile Dialog ───────────────────────────────────────────────────────

@Composable
private fun EditProfileDialog(
    uiState: ProfileUiState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBloodGroupChange: (String) -> Unit,
    onAllergiesChange: (String) -> Unit,
    onOrganDonorChange: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.medium)
            ) {
                // Show email read-only
                uiState.user?.email?.let { email ->
                    if (email.isNotBlank()) {
                        OutlinedCard(
                            shape = SamrakshaTheme.shapes.chip,
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Email (read-only)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                SamrakshaTextField(
                    value = uiState.editName,
                    onValueChange = onNameChange,
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person,
                    errorMessage = if (uiState.errorMessage != null && uiState.editName.isBlank())
                        uiState.errorMessage else null
                )

                SamrakshaTextField(
                    value = uiState.editPhone,
                    onValueChange = onPhoneChange,
                    label = "Phone Number",
                    leadingIcon = Icons.Default.Phone
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Emergency Medical ID",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                SamrakshaTextField(
                    value = uiState.editBloodGroup,
                    onValueChange = onBloodGroupChange,
                    label = "Blood Group (e.g. O+, A−)",
                    leadingIcon = Icons.Default.Bloodtype
                )

                SamrakshaTextField(
                    value = uiState.editAllergies,
                    onValueChange = onAllergiesChange,
                    label = "Allergies / Medical Conditions",
                    leadingIcon = Icons.Default.MedicalServices,
                    singleLine = false,
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Emergency Organ Donor",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Visible on Medical ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.editOrganDonor,
                        onCheckedChange = onOrganDonorChange,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Error message
                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    uiState.errorMessage?.let { error ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                shape = SamrakshaTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (uiState.isSaving) "Saving..." else "Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ── Logout Confirm Dialog ─────────────────────────────────────────────────────

@Composable
private fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Sign Out?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You will be signed out of your SAMRAKSHA account. Your local data will be preserved on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active SOS sessions will be ended",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = SamrakshaTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sign Out", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ── Profile Empty / Unauthenticated State ─────────────────────────────────────

@Composable
private fun ProfileEmptyState(
    onLoginClick: () -> Unit,
    onRetry: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(SamrakshaTheme.spacing.large),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.medium),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "No Active Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = errorMessage
                    ?: "You are not logged in. Sign in or register to access and manage your verified emergency profile, medical details, and safety settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.medium)
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            Button(
                onClick = onLoginClick,
                shape = SamrakshaTheme.shapes.button,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Log In / Sign Up", style = MaterialTheme.typography.labelLarge)
            }

            if (errorMessage != null) {
                TextButton(onClick = onRetry) {
                    Text("Retry Connecting")
                }
            }
        }
    }
}
