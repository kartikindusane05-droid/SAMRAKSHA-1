package com.samraksha.app.data.repository

import com.samraksha.app.data.local.SessionManager
import com.samraksha.app.data.local.dao.EmergencyContactDao
import com.samraksha.app.data.local.entity.EmergencyContactEntity
import com.samraksha.app.data.local.entity.toDomainModel
import com.samraksha.app.data.local.entity.toEntity
import com.samraksha.app.data.model.EmergencyContactDto
import com.samraksha.app.data.remote.ApiClient
import com.samraksha.app.data.remote.EmergencyContactApiService
import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Robust, offline-first Room-backed implementation of [EmergencyContactRepository].
 *
 * All operations execute locally in Room first to provide instant UI responsiveness
 * while offline. Once network connectivity is available, changes sync with the backend.
 */
class RoomEmergencyContactRepository(
    private val dao: EmergencyContactDao,
    private val apiService: EmergencyContactApiService = ApiClient.emergencyContactApiService,
    private val sessionManager: SessionManager = SessionManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EmergencyContactRepository {

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> {
        return dao.getAllContacts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addContact(contact: EmergencyContact) = withContext(ioDispatcher) {
        // 1. Local write first with isSynced = false
        val entity = contact.toEntity(isSynced = false)
        dao.insertContact(entity)

        // 2. Attempt push to backend
        try {
            val userId = sessionManager.getUser()?.id ?: "user_demo_001"
            val dto = EmergencyContactDto(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                relationship = contact.relationship,
                userId = userId
            )
            val response = apiService.addContact(userId = userId, contact = dto)
            if (response.isSuccessful && response.body()?.success == true) {
                dao.markSynced(contact.id)
            }
        } catch (_: Exception) {
            // Offline: data remains safely in local database
        }
    }

    override suspend fun updateContact(contact: EmergencyContact) = withContext(ioDispatcher) {
        // 1. Local write first
        val entity = contact.toEntity(isSynced = false)
        dao.updateContact(entity)

        // 2. Attempt push to backend
        try {
            val userId = sessionManager.getUser()?.id ?: "user_demo_001"
            val dto = EmergencyContactDto(
                id = contact.id,
                name = contact.name,
                phoneNumber = contact.phoneNumber,
                relationship = contact.relationship,
                userId = userId
            )
            val response = apiService.updateContact(id = contact.id, contact = dto)
            if (response.isSuccessful && response.body()?.success == true) {
                dao.markSynced(contact.id)
            }
        } catch (_: Exception) {
            // Offline: changes preserved locally
        }
    }

    override suspend fun deleteContact(contact: EmergencyContact) = withContext(ioDispatcher) {
        // 1. Mark as soft-deleted locally so UI updates immediately
        dao.softDeleteContact(contact.id)

        // 2. Attempt delete on backend
        try {
            val response = apiService.deleteContact(contact.id)
            if (response.isSuccessful) {
                dao.hardDeleteContact(contact.id)
            }
        } catch (_: Exception) {
            // Offline: soft-deleted record is retained for later sync
        }
    }

    override suspend fun syncContacts(): Result<List<EmergencyContact>> = withContext(ioDispatcher) {
        val userId = sessionManager.getUser()?.id ?: "user_demo_001"
        try {
            // 1. Process pending soft-deletions on backend first so deleted contacts are not resurrected
            val pendingDeletes = dao.getPendingDeletedContacts()
            for (del in pendingDeletes) {
                try {
                    val delResponse = apiService.deleteContact(del.id)
                    if (delResponse.isSuccessful || delResponse.code() == 404) {
                        dao.hardDeleteContact(del.id)
                    }
                } catch (_: Exception) {
                    // Still offline or failed; preserve soft-deleted record for next retry
                }
            }

            // 2. Prepare local active contacts for batch sync (deduplicated by ID)
            val localEntities = dao.getAllContactsList().distinctBy { it.id }
            val dtos = localEntities.map { entity ->
                EmergencyContactDto(
                    id = entity.id,
                    name = entity.name,
                    phoneNumber = entity.phoneNumber,
                    relationship = entity.relationship,
                    userId = userId
                )
            }

            val response = apiService.syncContacts(userId = userId, contacts = dtos)
            if (response.isSuccessful && response.body()?.data != null) {
                val serverContacts = response.body()!!.data!!
                val currentlyDeletedIds = dao.getPendingDeletedContacts().map { it.id }.toSet()

                val newEntities = serverContacts
                    .filterNot { it.id in currentlyDeletedIds }
                    .distinctBy { it.id }
                    .map { dto ->
                        EmergencyContactEntity(
                            id = dto.id,
                            name = dto.name,
                            phoneNumber = dto.phoneNumber,
                            relationship = dto.relationship,
                            isSynced = true,
                            isDeleted = false
                        )
                    }

                // Upsert all synchronized contacts into Room and mark existing synced
                dao.insertContacts(newEntities)
                localEntities.forEach { dao.markSynced(it.id) }

                val domainList = dao.getAllContactsList().map { it.toDomainModel() }
                Result.success(domainList)
            } else {
                val fallbackList = dao.getAllContactsList().map { it.toDomainModel() }
                Result.success(fallbackList)
            }
        } catch (_: Exception) {
            // Offline gracefully: return local cached contacts
            val cachedList = dao.getAllContactsList().map { it.toDomainModel() }
            Result.success(cachedList)
        }
    }
}
