package com.samraksha.app.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying LoginViewModel validation rules and state management.
 */
class LoginViewModelTest {

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        viewModel = LoginViewModel()
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals("", state.identifier)
        assertEquals("", state.password)
        assertFalse(state.isPasswordVisible)
        assertNull(state.identifierError)
        assertNull(state.passwordError)
        assertFalse(state.isLoading)
    }

    @Test
    fun onTogglePasswordVisibility_togglesFlagCorrectly() {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.onTogglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun emptyFields_produceValidationErrors() {
        var successCalled = false
        viewModel.onLogin { successCalled = true }

        val state = viewModel.uiState.value
        assertFalse(successCalled)
        assertEquals("Email or phone number is required", state.identifierError)
        assertEquals("Password is required", state.passwordError)
    }

    @Test
    fun invalidEmail_producesEmailError() {
        viewModel.onIdentifierChanged("invalid-email@")
        viewModel.onPasswordChanged("securePassword123")

        var successCalled = false
        viewModel.onLogin { successCalled = true }

        val state = viewModel.uiState.value
        assertFalse(successCalled)
        assertEquals("Please enter a valid email address", state.identifierError)
        assertNull(state.passwordError)
    }

    @Test
    fun invalidPhone_producesPhoneError() {
        viewModel.onIdentifierChanged("12345") // Only 5 digits
        viewModel.onPasswordChanged("securePassword123")

        var successCalled = false
        viewModel.onLogin { successCalled = true }

        val state = viewModel.uiState.value
        assertFalse(successCalled)
        assertEquals("Please enter a valid 10-digit phone number or email", state.identifierError)
        assertNull(state.passwordError)
    }

    @Test
    fun shortPassword_producesPasswordLengthError() {
        viewModel.onIdentifierChanged("user@example.com")
        viewModel.onPasswordChanged("123") // Less than 6 characters

        var successCalled = false
        viewModel.onLogin { successCalled = true }

        val state = viewModel.uiState.value
        assertFalse(successCalled)
        assertNull(state.identifierError)
        assertEquals("Password must be at least 6 characters", state.passwordError)
    }

    @Test
    fun typingClearsErrors() {
        viewModel.onLogin {}
        assertNotNull(viewModel.uiState.value.identifierError)
        assertNotNull(viewModel.uiState.value.passwordError)

        viewModel.onIdentifierChanged("test@example.com")
        assertNull(viewModel.uiState.value.identifierError)

        viewModel.onPasswordChanged("validPassword")
        assertNull(viewModel.uiState.value.passwordError)
    }
}
