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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
import com.samraksha.app.ui.screens.common.ScreenScaffold
import com.samraksha.app.ui.theme.SamrakshaTheme
import com.samraksha.app.ui.components.status.SamrakshaAlertBanner
import com.samraksha.app.ui.components.status.SafetyLevel

@Composable
fun SignupScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Terms of Service Modal Dialog
    if (uiState.showTermsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissTermsDialog() },
            title = {
                Text(
                    text = "Terms & Safety Protocol",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "By creating an account with SAMRAKSHA, you agree to:\n\n" +
                                "1. Accurate Information: Provide authentic emergency contacts and personal details for life-saving assistance.\n\n" +
                                "2. Location Telemetry: Allow continuous, high-accuracy GPS monitoring during active SOS triggers and monitored journeys.\n\n" +
                                "3. Emergency Dispatches: Authorize automated SMS, phone call dispatches, and emergency alerts to your selected guardians.\n\n" +
                                "4. End-to-End Encryption: Your location and health data remain strictly confidential and encrypted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                SamrakshaTextButton(
                    text = "I Understand",
                    onClick = {
                        viewModel.onTermsAcceptedChanged(true)
                        viewModel.onDismissTermsDialog()
                    }
                )
            },
            shape = SamrakshaTheme.shapes.dialog,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    ScreenScaffold(
        title = "Create Account",
        onNavigateBack = onNavigateBack
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SamrakshaTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Shield Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "SAMRAKSHA",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

            Text(
                text = "Join SAMRAKSHA",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))

            Text(
                text = "Set up your emergency profile and connect your guardian network",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // General Error Banner
            if (uiState.generalError != null) {
                SamrakshaAlertBanner(
                    title = "Authentication Notice",
                    message = uiState.generalError!!,
                    level = SafetyLevel.WARNING,
                    modifier = Modifier.padding(bottom = SamrakshaTheme.spacing.medium)
                )
            }

            // ── 1. Full Name ──────────────────────────────────
            SamrakshaTextField(
                value = uiState.fullName,
                onValueChange = { viewModel.onFullNameChanged(it) },
                label = "Full Name",
                placeholder = "Enter your full name",
                leadingIcon = Icons.Default.Person,
                errorMessage = uiState.fullNameError,
                isError = uiState.fullNameError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            // ── 2. Email Address ──────────────────────────────
            SamrakshaTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChanged(it) },
                label = "Email Address",
                placeholder = "name@domain.com",
                leadingIcon = Icons.Default.Email,
                errorMessage = uiState.emailError,
                isError = uiState.emailError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            // ── 3. Phone Number ───────────────────────────────
            SamrakshaTextField(
                value = uiState.phone,
                onValueChange = { viewModel.onPhoneChanged(it) },
                label = "Phone Number",
                placeholder = "+91 98765 43210",
                leadingIcon = Icons.Default.Phone,
                errorMessage = uiState.phoneError,
                isError = uiState.phoneError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            // ── 4. Password ───────────────────────────────────
            SamrakshaTextField(
                value = uiState.password,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = "Password",
                placeholder = "Minimum 6 characters",
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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.small))

            // ── 5. Confirm Password ───────────────────────────
            SamrakshaTextField(
                value = uiState.confirmPassword,
                onValueChange = { viewModel.onConfirmPasswordChanged(it) },
                label = "Confirm Password",
                placeholder = "Re-enter your password",
                leadingIcon = Icons.Default.Lock,
                errorMessage = uiState.confirmPasswordError,
                isError = uiState.confirmPasswordError != null,
                visualTransformation = if (uiState.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.onToggleConfirmPasswordVisibility() }) {
                        Icon(
                            imageVector = if (uiState.isConfirmPasswordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (uiState.isConfirmPasswordVisible) "Hide password" else "Show password",
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
                        viewModel.onSignup(onNavigateToHome)
                    }
                )
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.medium))

            // ── 6. Terms Acceptance ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onTermsAcceptedChanged(!uiState.termsAccepted) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.termsAccepted,
                    onCheckedChange = { viewModel.onTermsAcceptedChanged(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "I accept the ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.clickable { viewModel.onOpenTermsDialog() }
                        )
                        Text(
                            text = " & SOS Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (uiState.termsError != null) {
                Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.extraSmall))
                Text(
                    text = uiState.termsError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.large))

            // ── 7. Sign Up Button ─────────────────────────────
            SamrakshaPrimaryButton(
                text = "Create Safety Account",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onSignup(onNavigateToHome)
                },
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))

            // ── 8. Link to Sign In ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(SamrakshaTheme.spacing.extraSmall))
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable(onClick = onNavigateToLogin)
                )
            }

            Spacer(modifier = Modifier.height(SamrakshaTheme.spacing.base))
        }
    }
}
