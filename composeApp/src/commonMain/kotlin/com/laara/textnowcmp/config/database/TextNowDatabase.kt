package com.laara.textnowcmp.config.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.laara.textnowcmp.config.database.dao.ContactDao
import com.laara.textnowcmp.config.database.dao.ConversationDao
import com.laara.textnowcmp.config.database.dao.MessageDao
import com.laara.textnowcmp.config.database.entity.ContactEntity
import com.laara.textnowcmp.config.database.entity.ConversationEntity
import com.laara.textnowcmp.config.database.entity.MessageEntity

@Database(
    entities = [
        ContactEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TextNowDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
