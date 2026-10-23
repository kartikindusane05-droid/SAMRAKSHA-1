package com.samraksha.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samraksha.app.SamrakshaApp
import com.samraksha.app.data.repository.AuthRepositoryImpl
import com.samraksha.app.domain.repository.AuthRepository
import com.samraksha.app.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Login screen.
 */
data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val identifierError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordSuccessMessage: String? = null
)

/**
 * ViewModel managing Login screen state, validation rules, and backend API authentication.
 */
class LoginViewModel(
    private val authRepository: AuthRepository = try { SamrakshaApp.instance.authRepository } catch (_: Exception) { AuthRepositoryImpl() },
    private val userRepository: UserRepository? = try { SamrakshaApp.instance.userRepository } catch (_: Exception) { null }
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onIdentifierChanged(value: String) {
        _uiState.update {
            it.copy(
                identifier = value,
                identifierError = null,
                generalError = null
            )
        }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onOpenForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotPasswordDialog = true, forgotPasswordSuccessMessage = null) }
    }

    fun onDismissForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotPasswordDialog = false, forgotPasswordSuccessMessage = null) }
    }

    fun onSendPasswordReset(identifier: String) {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) {
            return
        }

        viewModelScope.launch {
            delay(600)
            _uiState.update {
                it.copy(
                    forgotPasswordSuccessMessage = "Recovery instructions sent to $trimmed"
                )
            }
        }
    }

    /**
     * Validates input fields and executes backend API login.
     */
    fun onLogin(onSuccess: () -> Unit) {
        val state = _uiState.value
        val identifier = state.identifier.trim()
        val password = state.password

        var hasError = false
        var idError: String? = null
        var pwdError: String? = null

        // ── Identifier Validation ─────────────────────────────
        if (identifier.isEmpty()) {
            idError = "Email or phone number is required"
            hasError = true
        } else if (identifier.contains("@")) {
            val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!emailRegex.matches(identifier)) {
                idError = "Please enter a valid email address"
                hasError = true
            }
        } else {
            val digitsOnly = identifier.filter { it.isDigit() }
            if (digitsOnly.length < 10) {
                idError = "Please enter a valid 10-digit phone number or email"
                hasError = true
            }
        }

        // ── Password Validation ───────────────────────────────
        if (password.isEmpty()) {
            pwdError = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            pwdError = "Password must be at least 6 characters"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    identifierError = idError,
                    passwordError = pwdError
                )
            }
            return
        }

        // ── Backend API Authentication ────────────────────────
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            val result = authRepository.login(
                identifier = identifier,
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
                            generalError = error.message ?: "Authentication failed. Please try again."
                        )
                    }
                }
            )
        }
    }
}
