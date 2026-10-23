package com.samraksha.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samraksha.app.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE isDeleted = 0")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE isDeleted = 0")
    suspend fun getAllContactsList(): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedContacts(): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getPendingDeletedContacts(): List<EmergencyContactEntity>

    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE isSynced = 0 OR isDeleted = 1")
    suspend fun getUnsyncedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContactEntity>)

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)

    @Query("UPDATE emergency_contacts SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE emergency_contacts SET isDeleted = 1, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteContact(id: String)

    @Delete
    suspend fun deleteContact(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun hardDeleteContact(id: String)

    @Query("DELETE FROM emergency_contacts")
    suspend fun clearAll()
}
