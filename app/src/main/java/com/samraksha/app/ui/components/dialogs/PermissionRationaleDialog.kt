package com.samraksha.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.samraksha.app.location.LocationPermissionHelper
import com.samraksha.app.notifications.NotificationPermissionHelper
import com.samraksha.app.ui.components.buttons.SamrakshaOutlinedButton
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Standard pre-permission rationale dialog.
 *
 * Explains *why* the permission is strictly needed before invoking the OS system dialog,
 * assuring the user that SAMRAKSHA does not misuse permissions or monetize personal data.
 */
@Composable
fun PermissionRationaleDialog(
    icon: ImageVector,
    title: String,
    purposeMessage: String,
    privacyNote: String,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    grantButtonText: String = "Continue",
    dismissButtonText: String = "Not Now",
    onOpenSettings: (() -> Unit)? = null,
    settingsButtonText: String = "Open Settings"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(SamrakshaTheme.spacing.base),
            shape = SamrakshaTheme.shapes.dialog,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(SamrakshaTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                Text(
                    text = purposeMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

                // Privacy Assurance Pill / Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(SamrakshaTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(SamrakshaTheme.colors.safe.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = SamrakshaTheme.colors.safe,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.small))

                        Text(
                            text = privacyNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

                // Action Buttons
                SamrakshaPrimaryButton(
                    text = grantButtonText,
                    onClick = onGrant,
                    shape = SamrakshaTheme.shapes.button,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onOpenSettings != null) {
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                    SamrakshaOutlinedButton(
                        text = settingsButtonText,
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                SamrakshaOutlinedButton(
                    text = dismissButtonText,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Convenient Location Permission Rationale Dialog pre-populated with SAMRAKSHA copy.
 */
@Composable
fun LocationPermissionRationaleDialog(
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    isPermanentlyDenied: Boolean = false
) {
    if (isPermanentlyDenied && onOpenSettings != null) {
        PermissionRationaleDialog(
            icon = Icons.Default.MyLocation,
            title = LocationPermissionHelper.PERMANENTLY_DENIED_TITLE,
            purposeMessage = LocationPermissionHelper.PERMANENTLY_DENIED_MESSAGE,
            privacyNote = LocationPermissionHelper.RATIONALE_PRIVACY_NOTE,
            onGrant = onOpenSettings,
            grantButtonText = "Open App Settings",
            onDismiss = onDismiss,
            dismissButtonText = "Cancel"
        )
    } else {
        PermissionRationaleDialog(
            icon = Icons.Default.MyLocation,
            title = LocationPermissionHelper.RATIONALE_TITLE,
            purposeMessage = LocationPermissionHelper.RATIONALE_MESSAGE,
            privacyNote = LocationPermissionHelper.RATIONALE_PRIVACY_NOTE,
            onGrant = onGrant,
            onDismiss = onDismiss,
            onOpenSettings = onOpenSettings
        )
    }
}

/**
 * Convenient Notification Permission Rationale Dialog pre-populated with SAMRAKSHA copy.
 */
@Composable
fun NotificationPermissionRationaleDialog(
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    isPermanentlyDenied: Boolean = false
) {
    if (isPermanentlyDenied && onOpenSettings != null) {
        PermissionRationaleDialog(
            icon = Icons.Default.NotificationsActive,
            title = NotificationPermissionHelper.PERMANENTLY_DENIED_TITLE,
            purposeMessage = NotificationPermissionHelper.PERMANENTLY_DENIED_MESSAGE,
            privacyNote = NotificationPermissionHelper.RATIONALE_PRIVACY_NOTE,
            onGrant = onOpenSettings,
            grantButtonText = "Open Notification Settings",
            onDismiss = onDismiss,
            dismissButtonText = "Cancel"
        )
    } else {
        PermissionRationaleDialog(
            icon = Icons.Default.NotificationsActive,
            title = NotificationPermissionHelper.RATIONALE_TITLE,
            purposeMessage = NotificationPermissionHelper.RATIONALE_MESSAGE,
            privacyNote = NotificationPermissionHelper.RATIONALE_PRIVACY_NOTE,
            onGrant = onGrant,
            onDismiss = onDismiss,
            onOpenSettings = onOpenSettings
        )
    }
}
