package com.samraksha.app.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying SignupViewModel validation logic and state transitions.
 */
class SignupViewModelTest {

    private lateinit var viewModel: SignupViewModel

    @Before
    fun setUp() {
        viewModel = SignupViewModel()
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals("", state.fullName)
        assertEquals("", state.email)
        assertEquals("", state.phone)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.termsAccepted)
        assertFalse(state.isPasswordVisible)
        assertFalse(state.isConfirmPasswordVisible)
        assertNull(state.fullNameError)
        assertNull(state.emailError)
        assertNull(state.phoneError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
        assertNull(state.termsError)
    }

    @Test
    fun emptyFields_produceAllValidationErrors() {
        var successCalled = false
        viewModel.onSignup { successCalled = true }

        val state = viewModel.uiState.value
        assertFalse(successCalled)
        assertEquals("Full name is required", state.fullNameError)
        assertEquals("Email address is required", state.emailError)
        assertEquals("Phone number is required for SOS dispatch", state.phoneError)
        assertEquals("Password is required", state.passwordError)
        assertEquals("Please confirm your password", state.confirmPasswordError)
        assertEquals("You must agree to the Terms of Service to continue", state.termsError)
    }

    @Test
    fun shortName_producesNameLengthError() {
        viewModel.onFullNameChanged("A") // 1 char
        viewModel.onSignup {}
        assertEquals("Please enter your full name (at least 2 characters)", viewModel.uiState.value.fullNameError)
    }

    @Test
    fun invalidEmail_producesEmailFormatError() {
        viewModel.onEmailChanged("user@not-complete")
        viewModel.onSignup {}
        assertEquals("Please enter a valid email address", viewModel.uiState.value.emailError)
    }

    @Test
    fun invalidPhone_producesPhoneLengthError() {
        viewModel.onPhoneChanged("12345") // Only 5 digits
        viewModel.onSignup {}
        assertEquals("Please enter a valid 10-digit phone number", viewModel.uiState.value.phoneError)
    }

    @Test
    fun shortPassword_producesPasswordLengthError() {
        viewModel.onPasswordChanged("12345") // 5 chars
        viewModel.onSignup {}
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.passwordError)
    }

    @Test
    fun passwordMismatch_producesConfirmPasswordError() {
        viewModel.onPasswordChanged("securePassword123")
        viewModel.onConfirmPasswordChanged("differentPassword")
        viewModel.onSignup {}
        assertEquals("Passwords do not match", viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun termsNotAccepted_producesTermsError() {
        viewModel.onFullNameChanged("Karthik R")
        viewModel.onEmailChanged("karthik@example.com")
        viewModel.onPhoneChanged("+919876543210")
        viewModel.onPasswordChanged("securePass123")
        viewModel.onConfirmPasswordChanged("securePass123")
        viewModel.onTermsAcceptedChanged(false)

        var successCalled = false
        viewModel.onSignup { successCalled = true }

        assertFalse(successCalled)
        assertEquals("You must agree to the Terms of Service to continue", viewModel.uiState.value.termsError)
    }

    @Test
    fun visibilityToggles_toggleCorrectly() {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)

        assertFalse(viewModel.uiState.value.isConfirmPasswordVisible)
        viewModel.onToggleConfirmPasswordVisibility()
        assertTrue(viewModel.uiState.value.isConfirmPasswordVisible)
    }

    @Test
    fun typingClearsErrors() {
        viewModel.onSignup {}
        assertNotNull(viewModel.uiState.value.fullNameError)
        assertNotNull(viewModel.uiState.value.emailError)

        viewModel.onFullNameChanged("Karthik")
        assertNull(viewModel.uiState.value.fullNameError)

        viewModel.onEmailChanged("karthik@example.com")
        assertNull(viewModel.uiState.value.emailError)
    }
}
