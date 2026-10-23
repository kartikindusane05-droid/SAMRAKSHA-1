package com.samraksha.app.ui.screens.auth

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samraksha.app.ui.components.buttons.SamrakshaPrimaryButton
import com.samraksha.app.ui.components.buttons.SamrakshaTextButton
import com.samraksha.app.ui.components.inputs.SamrakshaTextField
import com.samraksha.app.ui.components.status.SafetyLevel
import com.samraksha.app.ui.components.status.SamrakshaAlertBanner
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.theme.SamrakshaTheme

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Forgot Password Dialog
    if (uiState.showForgotPasswordDialog) {
        var recoveryInput by remember { mutableStateOf(uiState.identifier) }

        AlertDialog(
            onDismissRequest = { viewModel.onDismissForgotPasswordDialog() },
            title = {
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your registered email address or phone number to receive secure reset instructions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))
                    SamrakshaTextField(
                        value = recoveryInput,
                        onValueChange = { recoveryInput = it },
                        label = "Email or Phone",
                        placeholder = "name@domain.com or +91...",
                        leadingIcon = Icons.Default.Email
                    )
                    if (uiState.forgotPasswordSuccessMessage != null) {
                        Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))
                        Text(
                            text = uiState.forgotPasswordSuccessMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = SamrakshaTheme.colors.safe
                        )
                    }
                }
            },
            confirmButton = {
                SamrakshaTextButton(
                    text = "Send Link",
                    onClick = { viewModel.onSendPasswordReset(recoveryInput) }
                )
            },
            dismissButton = {
                SamrakshaTextButton(
                    text = "Cancel",
                    onClick = { viewModel.onDismissForgotPasswordDialog() },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = SamrakshaTheme.shapes.dialog,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    ScreenScaffold(
        title = "Sign In",
        onNavigateBack = onNavigateBack
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SamrakshaTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield Emblem Header
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "SAMRAKSHA",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

            Text(
                text = "Sign in to activate your emergency network and live guardian monitoring",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // General error alert (if any)
            if (uiState.generalError != null) {
                SamrakshaAlertBanner(
                    title = "Authentication Notice",
                    message = uiState.generalError ?: "",
                    level = SafetyLevel.WARNING
                )
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))
            }

            // ── Email / Phone Input Field ─────────────────────
            SamrakshaTextField(
                value = uiState.identifier,
                onValueChange = { viewModel.onIdentifierChanged(it) },
                label = "Email or Phone Number",
                placeholder = "name@domain.com or 10-digit number",
                leadingIcon = Icons.Default.Person,
                errorMessage = uiState.identifierError,
                isError = uiState.identifierError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

            // ── Password Input Field with Show/Hide Toggle ───
            SamrakshaTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = "Password",
                placeholder = "Enter your password",
                leadingIcon = Icons.Default.Lock,
                errorMessage = uiState.passwordError,
                isError = uiState.passwordError != null,
                visualTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.onTogglePasswordVisibility() }) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.onLogin(onNavigateToHome)
                    }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            // ── Forgot Password Link ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot password?",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { viewModel.onOpenForgotPasswordDialog() }
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // ── Login Action Button ───────────────────────────
            SamrakshaPrimaryButton(
                text = "Sign In",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onLogin(onNavigateToHome)
                },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // ── Link to Sign Up ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.extraSmall))
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable(onClick = onNavigateToSignup)
                )
            }
        }
    }
}
