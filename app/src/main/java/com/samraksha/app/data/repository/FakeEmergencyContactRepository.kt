package com.samraksha.app.data.repository

import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeEmergencyContactRepository : EmergencyContactRepository {
    private val contactsFlow = MutableStateFlow<List<EmergencyContact>>(emptyList())

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> {
        return contactsFlow.asStateFlow()
    }

    override suspend fun addContact(contact: EmergencyContact) {
        contactsFlow.update { currentList ->
            currentList + contact
        }
    }

    override suspend fun updateContact(contact: EmergencyContact) {
        contactsFlow.update { currentList ->
            currentList.map { if (it.id == contact.id) contact else it }
        }
    }

    override suspend fun deleteContact(contact: EmergencyContact) {
        contactsFlow.update { currentList ->
            currentList.filter { it.id != contact.id }
        }
    }

    override suspend fun syncContacts(): Result<List<EmergencyContact>> {
        return Result.success(contactsFlow.value)
    }
}
