package com.laara.textnowcmp.config.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val ACCESS_TOKEN = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
}