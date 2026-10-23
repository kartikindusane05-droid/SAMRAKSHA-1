package com.samraksha.app.domain.repository

import com.samraksha.app.domain.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

interface EmergencyContactRepository {
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>
    suspend fun addContact(contact: EmergencyContact)
    suspend fun updateContact(contact: EmergencyContact)
    suspend fun deleteContact(contact: EmergencyContact)
    suspend fun syncContacts(): Result<List<EmergencyContact>>
}
