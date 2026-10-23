package com.samraksha.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.ui.components.buttons.SamrakshaEmergencyButton
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.components.status.SamrakshaPulsingDot
import com.samraksha.app.ui.components.status.SamrakshaStatusPill
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Clean, organized, and responsive Home screen for SAMRAKSHA.
 *
 * Layout Structure:
 * 1. Top Bar: SAMRAKSHA branding with Profile and Settings action buttons.
 * 2. Welcome Banner: Real user name, dynamic protection pill, and real telemetry metrics.
 * 3. Prominent Manual SOS Button: 3-second press-and-hold trigger with pulsing halo.
 * 4. Your Safety at a Glance: Compact real-state overview of GPS availability, emergency contacts,
 *    and active journey monitoring.
 * 5. Quick Actions: Exactly three compact quick actions (Safety Map, Start a Safe Journey, Safety Store).
 * 6. Bottom Navigation: Home, Map, Journey, History, and Store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSos: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToStore: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.provideFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Automatically refresh location availability when user returns to Home
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SamrakshaTheme.colors.softBlueBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "SAMRAKSHA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SamrakshaTheme.colors.softLavenderBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = SamrakshaTheme.colors.lavender,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Currently on Home */ },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Home") },
                    label = { Text("Home", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = SamrakshaTheme.colors.softBlueBackground,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToMap,
                    icon = { Icon(Icons.Default.Map, contentDescription = "Safety Map") },
                    label = { Text("Map", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToJourney,
                    icon = { Icon(Icons.Default.Navigation, contentDescription = "Safe Journey") },
                    label = { Text("Journey", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistory,
                    icon = { Icon(Icons.Default.History, contentDescription = "Emergency History") },
                    label = { Text("History", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToStore,
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Safety Store") },
                    label = { Text("Store", style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SamrakshaTheme.spacing.base, vertical = SamrakshaTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.medium)
        ) {
            // ── 1. Welcome & Status Banner ─────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SamrakshaTheme.colors.border)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SamrakshaTheme.colors.softBlueBackground,
                                    SamrakshaTheme.colors.softLavenderBackground
                                )
                            )
                        )
                        .padding(SamrakshaTheme.spacing.base)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hello, ${uiState.userName}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (uiState.isJourneyActive) {
                                        "Safe Journey Active • Monitoring Route"
                                    } else {
                                        "Personal safety network standby"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))
                            val (pillLabel, pillLevel) = when {
                                uiState.isProtected -> "PROTECTED" to SafetyLevel.SAFE
                                uiState.contactCount == 0 -> "SETUP NEEDED" to SafetyLevel.WARNING
                                else -> "GPS ATTENTION" to SafetyLevel.WARNING
                            }
                            SamrakshaStatusPill(label = pillLabel, level = pillLevel)
                        }

                        // Real protection metrics row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SamrakshaPulsingDot(
                                color = if (uiState.isProtected) SamrakshaTheme.colors.safe else SamrakshaTheme.colors.warning,
                                size = 8.dp,
                                isPulsing = true
                            )
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (uiState.isProtected) SamrakshaTheme.colors.safe else SamrakshaTheme.colors.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = buildString {
                                    append(if (uiState.isLocationReady) "GPS Active" else "GPS Attention")
                                    append(" • ")
                                    append(uiState.contactCount)
                                    append(" ")
                                    append(if (uiState.contactCount == 1) "Contact" else "Contacts")
                                    append(" Configured")
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ── 2. Prominent Manual SOS Button ─────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "EMERGENCY QUICK TRIGGER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                SamrakshaEmergencyButton(
                    onClick = onNavigateToSos,
                    title = "SOS",
                    subtitle = "HOLD 3s",
                    size = 144.dp,
                    isPulsing = true
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                Text(
                    text = "Press and hold 3 seconds to alert emergency contacts & dispatch coordinates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = SamrakshaTheme.spacing.small)
                )
            }

            // ── 3. Your Safety at a Glance ─────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
            ) {
                Text(
                    text = "YOUR SAFETY AT A GLANCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(SamrakshaTheme.spacing.base),
                        verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
                    ) {
                        // Metric 1: Location & GPS Status
                        SafetyGlanceItem(
                            icon = Icons.Default.LocationOn,
                            iconTint = if (uiState.isLocationReady) MaterialTheme.colorScheme.primary else SamrakshaTheme.colors.warning,
                            iconBackground = if (uiState.isLocationReady) SamrakshaTheme.colors.softBlueBackground else SamrakshaTheme.colors.warningContainer,
                            title = when (uiState.locationStatus) {
                                LocationStatus.READY -> {
                                    val loc = uiState.lastKnownLocation
                                    if (loc != null) {
                                        "GPS Ready (%.2f°, %.2f°)".format(loc.latitude, loc.longitude)
                                    } else {
                                        "GPS Ready & Active"
                                    }
                                }
                                LocationStatus.PERMISSION_NEEDED -> "Location Permission Needed"
                                LocationStatus.GPS_DISABLED -> "Device Location (GPS) Off"
                                LocationStatus.SEARCHING -> "Acquiring GPS Signal..."
                            },
                            subtitle = when (uiState.locationStatus) {
                                LocationStatus.READY -> "Accurate coordinates ready for instant emergency alerts"
                                LocationStatus.PERMISSION_NEEDED -> "Grant permission to attach coordinates to alerts"
                                LocationStatus.GPS_DISABLED -> "Turn on GPS provider in system settings"
                                LocationStatus.SEARCHING -> "Connecting to satellite location provider"
                            },
                            badge = {
                                if (uiState.isLocationReady) {
                                    SamrakshaStatusPill(label = "READY", level = SafetyLevel.SAFE)
                                } else {
                                    SamrakshaStatusPill(label = "CHECK", level = SafetyLevel.WARNING)
                                }
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Metric 2: Emergency Contacts
                        SafetyGlanceItem(
                            icon = Icons.Default.ContactPhone,
                            iconTint = SamrakshaTheme.colors.lavender,
                            iconBackground = SamrakshaTheme.colors.softLavenderBackground,
                            title = if (uiState.contactCount > 0) {
                                "${uiState.contactCount} Emergency ${if (uiState.contactCount == 1) "Contact" else "Contacts"}"
                            } else {
                                "No Emergency Contacts"
                            },
                            subtitle = if (uiState.contactCount > 0) {
                                "Configured guardians receive SMS & clickable map link"
                            } else {
                                "Tap to add trusted contacts who will be alerted"
                            },
                            badge = {
                                if (uiState.contactCount > 0) {
                                    SamrakshaStatusPill(label = "ARMED", level = SafetyLevel.SAFE)
                                } else {
                                    SamrakshaStatusPill(label = "ACTION", level = SafetyLevel.WARNING)
                                }
                            },
                            onClick = onNavigateToContacts
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Metric 3: Safe Journey Status
                        SafetyGlanceItem(
                            icon = Icons.Default.Navigation,
                            iconTint = SamrakshaTheme.colors.safe,
                            iconBackground = SamrakshaTheme.colors.safeContainer,
                            title = if (uiState.isJourneyActive) {
                                "Journey: ${uiState.activeJourneyDestination ?: "In Progress"}"
                            } else {
                                "Safe Journey Sentinel"
                            },
                            subtitle = if (uiState.isJourneyActive) {
                                "Real-time route & ETA check-in active"
                            } else {
                                "Standby • Ready to track your journey"
                            },
                            badge = {
                                if (uiState.isJourneyActive) {
                                    SamrakshaStatusPill(label = "ACTIVE", level = SafetyLevel.SAFE)
                                } else {
                                    SamrakshaStatusPill(label = "STANDBY", level = SafetyLevel.INFO)
                                }
                            },
                            onClick = onNavigateToJourney
                        )
                    }
                }
            }

            // ── 4. Quick Actions (No more than 3 compact quick actions) ────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
            ) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
                ) {
                    QuickActionTile(
                        title = "Safety Map",
                        subtitle = "Live map",
                        icon = Icons.Default.Map,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = SamrakshaTheme.colors.softBlueBackground,
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionTile(
                        title = "Safe Journey",
                        subtitle = "Track route",
                        icon = Icons.Default.Navigation,
                        iconTint = SamrakshaTheme.colors.safe,
                        iconBackground = SamrakshaTheme.colors.safeContainer,
                        onClick = onNavigateToJourney,
                        modifier = Modifier.weight(1f)
                    )

                    QuickActionTile(
                        title = "Safety Store",
                        subtitle = "Verified gear",
                        icon = Icons.Default.ShoppingBag,
                        iconTint = SamrakshaTheme.colors.lavender,
                        iconBackground = SamrakshaTheme.colors.softLavenderBackground,
                        onClick = onNavigateToStore,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
        }
    }
}

/**
 * Compact item row for the "Your Safety at a Glance" container.
 */
@Composable
private fun SafetyGlanceItem(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    badge: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 2.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
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

        Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))

        badge()

        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Compact tile for the Quick Actions section.
 */
@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
