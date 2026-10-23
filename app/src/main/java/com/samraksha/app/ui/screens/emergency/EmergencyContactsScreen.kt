package com.samraksha.app.ui.screens.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.components.cards.SamrakshaCard
import com.samraksha.app.ui.components.inputs.SamrakshaTextField
import com.samraksha.app.ui.components.status.SamrakshaAlertBanner
import com.samraksha.app.ui.components.status.SamrakshaEmptyView
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.theme.SamrakshaTheme

@Composable
fun EmergencyContactsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmergencyContactsViewModel = viewModel(
        factory = EmergencyContactsViewModel.Factory(SamrakshaApp.instance.emergencyContactRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    ScreenScaffold(
        title = "Emergency Contacts",
        onNavigateBack = onNavigateBack
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SamrakshaTheme.spacing.base)
            ) {
                SamrakshaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trusted Guardians",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = { viewModel.syncContacts() },
                            enabled = !uiState.isSyncing
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Contacts",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))
                    Text(
                        text = "These contacts will receive immediate SOS alerts, your live GPS location, and automatic audio recordings when triggered. Offline-first: saved locally and synced to cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

                if (uiState.contacts.isEmpty()) {
                    SamrakshaEmptyView(
                        title = "No Emergency Contacts",
                        message = "Add trusted guardians who will receive instant SOS alerts, live GPS coordinates, and audio recordings when triggered.",
                        icon = Icons.Default.Person,
                        actionLabel = "Add Guardian",
                        onAction = { viewModel.showAddContactDialog() },
                        modifier = Modifier.padding(bottom = 80.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp) // space for Add Button
                    ) {
                        items(uiState.contacts, key = { it.id }) { contact ->
                            ContactItem(
                                contact = contact,
                                onEdit = { viewModel.showEditContactDialog(contact) },
                                onDelete = { contactToDelete = contact }
                            )
                            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                        }
                    }
                }
            }

            SamrakshaPrimaryButton(
                text = "Add Contact",
                onClick = { viewModel.showAddContactDialog() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(SamrakshaTheme.spacing.base)
                    .fillMaxWidth()
            )
        }

        if (uiState.isAddingContact) {
            ContactDialog(
                uiState = uiState,
                onDismiss = { viewModel.dismissDialog() },
                onNameChange = { viewModel.updateNewName(it) },
                onPhoneChange = { viewModel.updateNewPhone(it) },
                onRelationshipChange = { viewModel.updateNewRelationship(it) },
                onSave = { viewModel.saveContact() }
            )
        }

        contactToDelete?.let { contact ->
            AlertDialog(
                onDismissRequest = { contactToDelete = null },
                shape = SamrakshaTheme.shapes.dialog,
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "Delete Emergency Contact",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove ${contact.name} from your emergency contacts? They will no longer receive emergency alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteContact(contact)
                            contactToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { contactToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ContactItem(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SamrakshaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(SamrakshaTheme.shapes.avatar)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${contact.relationship} • ${contact.phoneNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ContactDialog(
    uiState: EmergencyContactsUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRelationshipChange: (String) -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = if (uiState.contactToEdit != null) "Edit Contact" else "Add Contact",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(SamrakshaTheme.spacing.small)) {
                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                SamrakshaTextField(
                    value = uiState.newName,
                    onValueChange = onNameChange,
                    label = "Name",
                    placeholder = "Full Name",
                    singleLine = true
                )
                SamrakshaTextField(
                    value = uiState.newPhone,
                    onValueChange = onPhoneChange,
                    label = "Phone Number",
                    placeholder = "+91 98765 43210",
                    singleLine = true
                )
                SamrakshaTextField(
                    value = uiState.newRelationship,
                    onValueChange = onRelationshipChange,
                    label = "Relationship",
                    placeholder = "Parent, Friend, Spouse, etc.",
                    singleLine = true
                )
                val suggestions = listOf("Parent", "Spouse", "Sibling", "Friend", "Doctor")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.forEach { tag ->
                        SuggestionChip(
                            onClick = { onRelationshipChange(tag) },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (uiState.newRelationship.equals(tag, ignoreCase = true))
                                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
