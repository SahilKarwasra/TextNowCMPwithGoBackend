package com.laara.textnowcmp.config.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.laara.textnowcmp.config.database.dao.ContactDao
import com.laara.textnowcmp.config.database.dao.ConversationDao
import com.laara.textnowcmp.config.database.dao.MessageDao
import com.laara.textnowcmp.config.database.entity.ContactEntity
import com.laara.textnowcmp.config.database.entity.ConversationEntity
import com.laara.textnowcmp.config.database.entity.MessageEntity

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        ContactEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ],
)
@ConstructedBy(TextNowDatabaseConstructor::class)
abstract class TextNowDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}

// Room compiler will automatically generate the actual for this expect object
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TextNowDatabaseConstructor : RoomDatabaseConstructor<TextNowDatabase> {
    override fun initialize(): TextNowDatabase
}
