package com.samraksha.app.ui.screens.emergency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EmergencyContactsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val isAddingContact: Boolean = false,
    val contactToEdit: EmergencyContact? = null,
    val newName: String = "",
    val newPhone: String = "",
    val newRelationship: String = "",
    val isSyncing: Boolean = false,
    val error: String? = null
)

class EmergencyContactsViewModel(
    private val repository: EmergencyContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyContactsUiState())
    val uiState: StateFlow<EmergencyContactsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getEmergencyContacts().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }
        syncContacts()
    }

    fun showAddContactDialog() {
        _uiState.update { it.copy(isAddingContact = true, contactToEdit = null, newName = "", newPhone = "", newRelationship = "", error = null) }
    }

    fun showEditContactDialog(contact: EmergencyContact) {
        _uiState.update { 
            it.copy(
                isAddingContact = true,
                contactToEdit = contact,
                newName = contact.name,
                newPhone = contact.phoneNumber,
                newRelationship = contact.relationship,
                error = null
            )
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(isAddingContact = false, contactToEdit = null, newName = "", newPhone = "", newRelationship = "", error = null) }
    }

    fun updateNewName(name: String) = _uiState.update { it.copy(newName = name, error = null) }
    fun updateNewPhone(phone: String) = _uiState.update { it.copy(newPhone = phone, error = null) }
    fun updateNewRelationship(rel: String) = _uiState.update { it.copy(newRelationship = rel, error = null) }

    fun setContactFromPicker(name: String, phone: String) {
        _uiState.update {
            it.copy(
                isAddingContact = true,
                newName = name.trim().ifBlank { it.newName },
                newPhone = phone.trim().ifBlank { it.newPhone },
                error = null
            )
        }
    }

    fun saveContact() {
        val state = _uiState.value
        val name = state.newName.trim()
        val phone = state.newPhone.trim()
        val relationship = state.newRelationship.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Contact name cannot be empty") }
            return
        }
        if (name.length < 2) {
            _uiState.update { it.copy(error = "Please enter a valid contact name (at least 2 characters)") }
            return
        }

        val digitsOnly = phone.filter { it.isDigit() }
        if (phone.isBlank()) {
            _uiState.update { it.copy(error = "Phone number is required for emergency alerts") }
            return
        }
        if (digitsOnly.length < 10) {
            _uiState.update { it.copy(error = "Please enter a valid phone number with at least 10 digits") }
            return
        }

        if (relationship.isBlank()) {
            _uiState.update { it.copy(error = "Relationship is required (e.g., Parent, Friend, Doctor)") }
            return
        }

        viewModelScope.launch {
            if (state.contactToEdit != null) {
                val updated = state.contactToEdit.copy(
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship
                )
                repository.updateContact(updated)
            } else {
                val newContact = EmergencyContact(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship
                )
                repository.addContact(newContact)
            }
            dismissDialog()
        }
    }

    fun deleteContact(contact: EmergencyContact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun syncContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            repository.syncContacts()
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    class Factory(private val repository: EmergencyContactRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EmergencyContactsViewModel(repository) as T
        }
    }
}
