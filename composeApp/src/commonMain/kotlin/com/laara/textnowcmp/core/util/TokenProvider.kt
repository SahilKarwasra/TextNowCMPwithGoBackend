package com.laara.textnowcmp.core.util

import com.laara.textnowcmp.config.datastore.DataStoreRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class TokenProvider(
    private val dataStoreRepository: DataStoreRepository,
) {
    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            dataStoreRepository.authToken.collect { token ->
                cachedAccessToken = token
            }
        }

        scope.launch {
            dataStoreRepository.refreshToken.collect { token ->
                cachedRefreshToken = token
            }
        }
    }

    fun getAccessToken(): String? = cachedAccessToken
    fun getRefreshToken(): String? = cachedRefreshToken

    /**
     * Atomically update both tokens in DataStore and in-memory cache.
     */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        mutex.withLock {
            dataStoreRepository.saveToken(accessToken)
            dataStoreRepository.saveRefreshToken(refreshToken)
            cachedAccessToken = accessToken
            cachedRefreshToken = refreshToken
        }
    }

    /**
     * Clear all tokens (used on logout / session expiry).
     */
    suspend fun clearTokens() {
        mutex.withLock {
            dataStoreRepository.clearTokens()
            cachedAccessToken = null
            cachedRefreshToken = null
        }
    }
}