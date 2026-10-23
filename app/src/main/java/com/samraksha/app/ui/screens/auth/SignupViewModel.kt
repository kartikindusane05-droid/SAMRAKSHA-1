package com.samraksha.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.repository.AuthRepositoryImpl
import com.samraksha.app.domain.repository.AuthRepository
import com.samraksha.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Signup registration screen.
 */
data class SignupUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val termsAccepted: Boolean = false,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val showTermsDialog: Boolean = false
)

/**
 * ViewModel managing user registration inputs, validation rules, terms agreement, and backend API signup.
 */
class SignupViewModel(
    private val authRepository: AuthRepository = try { SamrakshaApp.instance.authRepository } catch (_: Exception) { AuthRepositoryImpl() },
    private val userRepository: UserRepository? = try { SamrakshaApp.instance.userRepository } catch (_: Exception) { null }
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onFullNameChanged(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null, generalError = null) }
    }

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phone = value, phoneError = null, generalError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                generalError = null,
                confirmPasswordError = if (it.confirmPassword.isNotEmpty() && value != it.confirmPassword) {
                    "Passwords do not match"
                } else null
            )
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = if (it.password.isNotEmpty() && value != it.password) {
                    "Passwords do not match"
                } else null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onTermsAcceptedChanged(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted, termsError = null) }
    }

    fun onOpenTermsDialog() {
        _uiState.update { it.copy(showTermsDialog = true) }
    }

    fun onDismissTermsDialog() {
        _uiState.update { it.copy(showTermsDialog = false) }
    }

    /**
     * Validates all registration parameters and calls the backend API signup endpoint.
     */
    fun onSignup(onSuccess: () -> Unit) {
        val state = _uiState.value
        val name = state.fullName.trim()
        val email = state.email.trim()
        val phone = state.phone.trim()
        val password = state.password
        val confirmPassword = state.confirmPassword

        var hasError = false
        var nameErr: String? = null
        var emailErr: String? = null
        var phoneErr: String? = null
        var pwdErr: String? = null
        var confirmPwdErr: String? = null
        var termsErr: String? = null

        // 1. Full Name Validation
        if (name.isEmpty()) {
            nameErr = "Full name is required"
            hasError = true
        } else if (name.length < 2) {
            nameErr = "Please enter your full name (at least 2 characters)"
            hasError = true
        }

        // 2. Email Validation
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (email.isEmpty()) {
            emailErr = "Email address is required"
            hasError = true
        } else if (!emailRegex.matches(email)) {
            emailErr = "Please enter a valid email address"
            hasError = true
        }

        // 3. Phone Validation
        val digitsOnly = phone.filter { it.isDigit() }
        if (phone.isEmpty()) {
            phoneErr = "Phone number is required for SOS dispatch"
            hasError = true
        } else if (digitsOnly.length < 10) {
            phoneErr = "Please enter a valid 10-digit phone number"
            hasError = true
        }

        // 4. Password Validation
        if (password.isEmpty()) {
            pwdErr = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            pwdErr = "Password must be at least 6 characters"
            hasError = true
        }

        // 5. Confirm Password Validation
        if (confirmPassword.isEmpty()) {
            confirmPwdErr = "Please confirm your password"
            hasError = true
        } else if (password != confirmPassword) {
            confirmPwdErr = "Passwords do not match"
            hasError = true
        }

        // 6. Terms Acceptance Validation
        if (!state.termsAccepted) {
            termsErr = "You must agree to the Terms of Service to continue"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    fullNameError = nameErr,
                    emailError = emailErr,
                    phoneError = phoneErr,
                    passwordError = pwdErr,
                    confirmPasswordError = confirmPwdErr,
                    termsError = termsErr
                )
            }
            return
        }

        // ── Backend API Registration ──────────────────────────
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.signup(
                fullName = name,
                email = email,
                phone = phone,
                password = password
            )

            result.fold(
                onSuccess = { session ->
                    viewModelScope.launch {
                        try {
                            userRepository?.saveUser(session.user)
                        } catch (_: Exception) {}
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            generalError = null
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = error.message ?: "Account registration failed. Please try again."
                        )
                    }
                }
            )
        }
    }
}
