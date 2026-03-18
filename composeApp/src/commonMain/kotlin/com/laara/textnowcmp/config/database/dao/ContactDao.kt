package com.laara.textnowcmp.config.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laara.textnowcmp.config.database.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Query("SELECT * FROM contacts WHERE isOnboarded = 1 ORDER BY name ASC")
    fun getOnboardedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isOnboarded = 0 ORDER BY name ASC")
    fun getNotOnboardedContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isOnboarded DESC, name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isOnboarded = 1 ORDER BY name ASC")
    suspend fun getOnboardedContactsOnce(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE isOnboarded = 0 ORDER BY name ASC")
    suspend fun getNotOnboardedContactsOnce(): List<ContactEntity>

    @Query("DELETE FROM contacts")
    suspend fun clearAll()
}
