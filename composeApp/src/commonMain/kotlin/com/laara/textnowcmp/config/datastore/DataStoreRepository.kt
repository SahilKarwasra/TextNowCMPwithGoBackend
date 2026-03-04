package com.laara.textnowcmp.config.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.laara.textnowcmp.config.datastore.DataStoreKeys.ACCESS_TOKEN
import com.laara.textnowcmp.config.datastore.DataStoreKeys.REFRESH_TOKEN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DataStoreRepository(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            dataStore.data
                .map { preferences -> preferences[ACCESS_TOKEN] }
                .first()
        }
    }
    suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[ACCESS_TOKEN] = token
            }
        }
    }
    val authToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }.flowOn(Dispatchers.IO)

    suspend fun getRefreshToken(): String? {
        return withContext(Dispatchers.IO) {
            dataStore.data
                .map { preferences -> preferences[REFRESH_TOKEN] }
                .first()
        }
    }
    suspend fun saveRefreshToken(token: String) {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[REFRESH_TOKEN] = token
            }
        }
    }
    val refreshToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }.flowOn(Dispatchers.IO)

    suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences.remove(ACCESS_TOKEN)
                preferences.remove(REFRESH_TOKEN)
            }
        }
    }

}