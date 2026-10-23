package com.samraksha.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.samraksha.app.domain.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val profilePictureUrl: String? = null,
    val bloodGroup: String? = null,
    val allergies: String? = null,
    val isOrganDonor: Boolean = false,
    val isSynced: Boolean = true
) {
    fun toDomainModel(): User = User(
        id = id,
        fullName = name,
        email = email,
        phone = phone,
        bloodGroup = bloodGroup,
        allergies = allergies,
        isOrganDonor = isOrganDonor
    )
}

fun User.toEntity(isSynced: Boolean = true): UserEntity = UserEntity(
    id = id,
    name = fullName,
    email = email,
    phone = phone,
    bloodGroup = bloodGroup,
    allergies = allergies,
    isOrganDonor = isOrganDonor,
    isSynced = isSynced
)
