package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.veganbeauty.app.data.local.dao.CommunityDao
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.ReelEntity

@Database(entities = [UserEntity::class, CommunityPostEntity::class, ReelEntity::class], version = 2)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun communityDao(): CommunityDao
}
