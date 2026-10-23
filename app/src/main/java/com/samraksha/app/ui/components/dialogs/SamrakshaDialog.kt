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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samraksha.app.ui.components.buttons.SamrakshaOutlinedButton
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.components.buttons.SamrakshaTextButton
import com.samraksha.app.ui.theme.EmergencyRed40
import com.samraksha.app.ui.theme.EmergencyRed95
import com.samraksha.app.ui.theme.SamrakshaTheme

/**
 * Standard confirmation dialog for app actions.
 */
@Composable
fun SamrakshaAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else null,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            SamrakshaTextButton(
                text = confirmButtonText,
                onClick = onConfirm,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        dismissButton = {
            SamrakshaTextButton(
                text = dismissButtonText,
                onClick = onDismiss,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = SamrakshaTheme.shapes.dialog,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

/**
 * Urgent emergency confirmation dialog.
 * Employs high-contrast crimson accents and prominent action buttons for emergency scenarios.
 */
@Composable
fun SamrakshaEmergencyDialog(
    title: String,
    message: String,
    onConfirmEmergency: () -> Unit,
    onCancel: () -> Unit,
    confirmText: String = "TRIGGER SOS NOW",
    cancelText: String = "Cancel",
    icon: ImageVector = Icons.Default.Warning
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(SamrakshaTheme.spacing.base),
            shape = SamrakshaTheme.shapes.dialog,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(SamrakshaTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emergency Warning Icon Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(EmergencyRed95),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = "Emergency Alert",
                        tint = EmergencyRed40,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

                // High-priority full width emergency trigger button
                SamrakshaPrimaryButton(
                    text = confirmText,
                    onClick = onConfirmEmergency,
                    shape = SamrakshaTheme.shapes.button,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

                SamrakshaOutlinedButton(
                    text = cancelText,
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
