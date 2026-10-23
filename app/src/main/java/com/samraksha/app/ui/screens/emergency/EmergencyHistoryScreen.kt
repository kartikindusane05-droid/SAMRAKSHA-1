package com.samraksha.app.ui.screens.emergency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.model.EmergencyEvent
import com.samraksha.app.domain.model.EmergencyEventStatus
import com.samraksha.app.domain.model.EmergencyEventType
import com.samraksha.app.domain.repository.EmergencyEventRepository
import com.samraksha.app.ui.components.buttons.SamrakshaOutlinedButton
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.components.cards.SamrakshaCard
import com.samraksha.app.ui.components.status.SamrakshaEmptyView
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.theme.SamrakshaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Filter State ─────────────────────────────────────────────────────────────

enum class EventFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    RESOLVED("Resolved"),
    CANCELLED("Cancelled")
}

data class EmergencyHistoryUiState(
    val events: List<EmergencyEvent> = emptyList(),
    val activeFilter: EventFilter = EventFilter.ALL,
    val selectedEvent: EmergencyEvent? = null,
    val isSyncing: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class EmergencyHistoryViewModel(
    private val repository: EmergencyEventRepository
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(EventFilter.ALL)
    private val _selectedEvent = MutableStateFlow<EmergencyEvent?>(null)
    private val _isSyncing = MutableStateFlow(false)

    val uiState: StateFlow<EmergencyHistoryUiState> = combine(
        repository.getAllEvents(),
        _activeFilter,
        _selectedEvent,
        _isSyncing
    ) { allEvents, filter, selected, syncing ->
        val filtered = when (filter) {
            EventFilter.ALL -> allEvents
            EventFilter.ACTIVE -> allEvents.filter { it.status == EmergencyEventStatus.ACTIVE }
            EventFilter.RESOLVED -> allEvents.filter { it.status == EmergencyEventStatus.RESOLVED }
            EventFilter.CANCELLED -> allEvents.filter { it.status == EmergencyEventStatus.CANCELLED }
        }
        EmergencyHistoryUiState(
            events = filtered,
            activeFilter = filter,
            selectedEvent = selected,
            isSyncing = syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EmergencyHistoryUiState()
    )

    fun setFilter(filter: EventFilter) {
        _activeFilter.value = filter
    }

    fun selectEvent(event: EmergencyEvent?) {
        _selectedEvent.value = event
    }

    fun resolveEvent(eventId: String) {
        viewModelScope.launch {
            repository.updateStatus(
                eventId = eventId,
                status = EmergencyEventStatus.RESOLVED,
                resolvedAt = System.currentTimeMillis()
            )
            _selectedEvent.value = null
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            _selectedEvent.value = null
        }
    }

    fun syncEvents() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncEvents()
            _isSyncing.value = false
        }
    }

    class Factory(private val repo: EmergencyEventRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EmergencyHistoryViewModel(repo) as T
    }
}

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmergencyHistoryViewModel = viewModel(
        factory = EmergencyHistoryViewModel.Factory(SamrakshaApp.instance.emergencyEventRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ScreenScaffold(
        title = "Emergency History",
        onNavigateBack = onNavigateBack
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .padding(horizontal = SamrakshaTheme.spacing.base)
        ) {
            Spacer(Modifier.height(SamrakshaTheme.spacing.small))

            // ── Filter Chips & Sync Action ──
            FilterRow(
                selectedFilter = uiState.activeFilter,
                isSyncing = uiState.isSyncing,
                onFilterSelected = { viewModel.setFilter(it) },
                onSync = { viewModel.syncEvents() }
            )

            Spacer(Modifier.height(SamrakshaTheme.spacing.small))

            if (uiState.events.isEmpty()) {
                EmptyHistoryView(activeFilter = uiState.activeFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = SamrakshaTheme.spacing.small),
                    verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
                ) {
                    items(uiState.events, key = { it.id }) { event ->
                        EmergencyEventCard(
                            event = event,
                            onClick = { viewModel.selectEvent(event) }
                        )
                    }
                }
            }
        }

        // ── Details Bottom Sheet ──
        uiState.selectedEvent?.let { event ->
            ModalBottomSheet(
                onDismissRequest = { viewModel.selectEvent(null) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                EventDetailSheetContent(
                    event = event,
                    onResolve = { viewModel.resolveEvent(it) },
                    onDelete = { viewModel.deleteEvent(it) },
                    onClose = { viewModel.selectEvent(null) }
                )
            }
        }
    }
}

// ── Filter Chips Row ─────────────────────────────────────────────────────────

@Composable
private fun FilterRow(
    selectedFilter: EventFilter,
    isSyncing: Boolean,
    onFilterSelected: (EventFilter) -> Unit,
    onSync: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
        ) {
            EventFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            filter.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = SamrakshaTheme.shapes.chip
                )
            }
        }

        IconButton(
            onClick = onSync,
            enabled = !isSyncing
        ) {
            if (isSyncing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Events",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Emergency Event Card ───────────────────────────────────────────────────────

@Composable
private fun EmergencyEventCard(
    event: EmergencyEvent,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateString = dateFormatter.format(Date(event.timestamp))
    val timeString = timeFormatter.format(Date(event.timestamp))

    val (statusBg, statusFg, statusLabel) = when (event.status) {
        EmergencyEventStatus.ACTIVE -> Triple(SamrakshaTheme.colors.emergencyContainer, SamrakshaTheme.colors.emergency, "ACTIVE")
        EmergencyEventStatus.CANCELLED -> Triple(SamrakshaTheme.colors.warningContainer, SamrakshaTheme.colors.warning, "CANCELLED")
        EmergencyEventStatus.RESOLVED -> Triple(SamrakshaTheme.colors.safeContainer, SamrakshaTheme.colors.safe, "RESOLVED")
    }

    val typeIcon: ImageVector = when (event.eventType) {
        EmergencyEventType.MANUAL_SOS -> Icons.Default.Warning
        EmergencyEventType.JOURNEY_TIMEOUT -> Icons.AutoMirrored.Filled.DirectionsWalk
        EmergencyEventType.GEOFENCE_ALERT -> Icons.Default.LocationOn
        EmergencyEventType.VOICE_TRIGGER -> Icons.Default.Mic
        EmergencyEventType.IMPACT_DETECTION -> Icons.Default.Shield
    }

    SamrakshaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(SamrakshaTheme.shapes.avatar)
                    .background(statusBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = statusFg,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(SamrakshaTheme.spacing.base))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.eventType.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Status Pill
                    Surface(
                        color = statusBg,
                        shape = SamrakshaTheme.shapes.chip
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Date & Time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$dateString • $timeString",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Sync & Location indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (event.latitude != 0.0 || event.longitude != 0.0) {
                                String.format(Locale.US, "%.3f°, %.3f°", event.latitude, event.longitude)
                            } else {
                                "GPS Coordinates Logged"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    if (event.isSynced) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = SamrakshaTheme.colors.safe,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = SamrakshaTheme.colors.safe
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = "Local Offline",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "Offline Stored",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Event Detail Bottom Sheet Content ────────────────────────────────────────

@Composable
private fun EventDetailSheetContent(
    event: EmergencyEvent,
    onResolve: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val fullDateFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm:ss a", Locale.getDefault())
    val formattedTimestamp = fullDateFormatter.format(Date(event.timestamp))

    val (statusBg, statusFg, statusLabel) = when (event.status) {
        EmergencyEventStatus.ACTIVE -> Triple(SamrakshaTheme.colors.emergencyContainer, SamrakshaTheme.colors.emergency, "ACTIVE INCIDENT")
        EmergencyEventStatus.CANCELLED -> Triple(SamrakshaTheme.colors.warningContainer, SamrakshaTheme.colors.warning, "CANCELLED BY USER")
        EmergencyEventStatus.RESOLVED -> Triple(SamrakshaTheme.colors.safeContainer, SamrakshaTheme.colors.safe, "RESOLVED & SAFE")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SamrakshaTheme.spacing.base)
            .padding(bottom = SamrakshaTheme.spacing.large)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Incident Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Event ID: ${event.id.take(8)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = statusBg,
                shape = SamrakshaTheme.shapes.chip
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(SamrakshaTheme.spacing.base))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(SamrakshaTheme.spacing.base))

        // Detail rows
        DetailSection(
            icon = Icons.Default.Shield,
            title = "Event Type",
            value = event.eventType.displayName,
            secondary = event.eventType.description
        )

        Spacer(Modifier.height(SamrakshaTheme.spacing.base))

        DetailSection(
            icon = Icons.Default.Schedule,
            title = "Recorded Date & Time",
            value = formattedTimestamp
        )

        if (event.resolvedAt != null) {
            Spacer(Modifier.height(SamrakshaTheme.spacing.base))
            val resolvedFormatted = fullDateFormatter.format(Date(event.resolvedAt))
            DetailSection(
                icon = Icons.Default.CheckCircle,
                title = "Resolution Time",
                value = resolvedFormatted
            )
        }

        Spacer(Modifier.height(SamrakshaTheme.spacing.base))

        DetailSection(
            icon = Icons.Default.PinDrop,
            title = "GPS Coordinates",
            value = if (event.latitude != 0.0 || event.longitude != 0.0) {
                "Lat: ${event.latitude}, Lon: ${event.longitude}"
            } else {
                "Standard GPS Coordinates Captured"
            },
            secondary = event.address ?: "Stored securely in local Room database"
        )

        Spacer(Modifier.height(SamrakshaTheme.spacing.base))

        DetailSection(
            icon = if (event.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
            title = "Cloud Synchronization",
            value = if (event.isSynced) "Synced with SAMRAKSHA Server" else "Stored Locally (Pending Cloud Sync)",
            secondary = if (event.isSynced) "Alert dispatches sent" else "Offline-first guarantee active"
        )

        Spacer(Modifier.height(SamrakshaTheme.spacing.large))

        // Action Buttons
        if (event.status == EmergencyEventStatus.ACTIVE) {
            SamrakshaPrimaryButton(
                text = "Mark as Resolved & Safe",
                onClick = { onResolve(event.id) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(SamrakshaTheme.spacing.small))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)
        ) {
            SamrakshaOutlinedButton(
                text = "Delete",
                onClick = { onDelete(event.id) },
                leadingIcon = {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )

            SamrakshaOutlinedButton(
                text = "Close",
                onClick = onClose,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DetailSection(
    icon: ImageVector,
    title: String,
    value: String,
    secondary: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Empty State View ──────────────────────────────────────────────────────────

@Composable
private fun EmptyHistoryView(activeFilter: EventFilter) {
    SamrakshaEmptyView(
        title = if (activeFilter == EventFilter.ALL) "No Emergency Events" else "No ${activeFilter.label} Events",
        message = "Your safety incident history is clear. When an SOS or safety alert is triggered, detailed records will appear here.",
        icon = Icons.Default.CheckCircle
    )
}
