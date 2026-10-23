package com.samraksha.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samraksha.app.domain.model.EmergencyContact

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
) {
    fun toDomainModel(): EmergencyContact {
        return EmergencyContact(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship
        )
    }
}

fun EmergencyContact.toEntity(isSynced: Boolean = false, isDeleted: Boolean = false): EmergencyContactEntity {
    return EmergencyContactEntity(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship,
        isSynced = isSynced,
        isDeleted = isDeleted
    )
}
