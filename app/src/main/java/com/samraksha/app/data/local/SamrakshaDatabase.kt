package com.samraksha.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samraksha.app.data.local.dao.EmergencyContactDao
import com.samraksha.app.data.local.dao.EmergencyEventDao
import com.samraksha.app.data.local.dao.JourneyDao
import com.samraksha.app.data.local.dao.UserDao
import com.samraksha.app.data.local.entity.EmergencyContactEntity
import com.samraksha.app.data.local.entity.EmergencyEventEntity
import com.samraksha.app.data.local.entity.JourneyEntity
import com.samraksha.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        EmergencyEventEntity::class,
        JourneyEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SamrakshaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun emergencyEventDao(): EmergencyEventDao
    abstract fun journeyDao(): JourneyDao
}
